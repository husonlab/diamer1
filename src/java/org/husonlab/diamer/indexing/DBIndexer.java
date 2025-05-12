package org.husonlab.diamer.indexing;

import org.husonlab.diamer.indexing.kmers.KmerExtractor;
import org.husonlab.diamer.io.indexing.BucketIO;
import org.husonlab.diamer.io.indexing.DBIndexIO;
import org.husonlab.diamer.io.seq.FutureSequenceRecords;
import org.husonlab.diamer.io.seq.SequenceSupplier;
import org.husonlab.diamer.io.taxonomy.TreeIO;
import org.husonlab.diamer.main.GlobalSettings;
import org.husonlab.diamer.main.encoders.Encoder;
import org.husonlab.diamer.seq.SequenceRecord;
import org.husonlab.diamer.taxonomy.Tree;
import org.husonlab.diamer.util.FlexibleBucket;
import org.husonlab.diamer.util.FlexibleIntArray;
import org.husonlab.diamer.util.Pair;
import org.husonlab.diamer.util.logging.*;

import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class DBIndexer {

    private final Logger logger;
    /**
     * Expected size of Bucket because of multithreading.
     */
    private final int expectedBucketSize;
    private final static int contingentSizes = 1_024;
    private final SequenceSupplier<Integer, byte[]> sup;
    private final Tree tree;
    private final Encoder encoder;
    private final GlobalSettings settings;
    BlockingQueue<FutureSequenceRecords<Integer, byte[]>[]> queue;
    private final FlexibleBucket[] buckets;
    private AtomicBoolean readingFinished = new AtomicBoolean(false);
    private final DBIndexIO dbIndexIO;
    private final int[] bucketSizes;
    private static final AtomicInteger processedSequences = new AtomicInteger(0);
    private static final AtomicInteger skippedSequences = new AtomicInteger(0);

    public DBIndexer(SequenceSupplier<Integer, byte[]> sup,
                        Tree tree,
                        long maxBucketSize,
                        Encoder encoder,
                        GlobalSettings settings) {
        logger = new Logger("DBIndexer");
        logger.addElement(new Time()).addElement(new RunningTime());
        this.expectedBucketSize = (int) maxBucketSize + contingentSizes * settings.MAX_THREADS;
        this.sup = sup;
        this.tree = tree;
        this.encoder = encoder;
        dbIndexIO = encoder.getDBIndexIO();
        this.settings = settings;
        queue = new ArrayBlockingQueue<>(settings.MAX_THREADS * 10, false);
        logger.logInfo("Allocating memory for " + settings.BUCKETS_PER_CYCLE + " buckets of size " + expectedBucketSize);
        buckets = new FlexibleBucket[settings.BUCKETS_PER_CYCLE];
        for (int i = 0; i < settings.BUCKETS_PER_CYCLE; i++) {
            buckets[i] = new FlexibleBucket(expectedBucketSize, 8_129, contingentSizes);
        }
        bucketSizes = new int[encoder.getNrOfBuckets()];
    }

    public String index() {
        tree.addLongProperty("kmers in database", 0);
        for (int i = 0; i < encoder.getNrOfBuckets(); i += settings.BUCKETS_PER_CYCLE) {
            processedSequences.set(0);
            skippedSequences.set(0);
            int rangeStart = i;
            int rangeEnd = Math.min(i + settings.BUCKETS_PER_CYCLE, encoder.getNrOfBuckets());
            int indexStart = 0;
            int indexEnd = rangeEnd - i;

            logger.logInfo("Indexing buckets " + i + " to " + (rangeEnd - 1));
            ProgressBar progressBar = new ProgressBar(sup.getFileSize(), 20);
            new OneLineLogger("DBIndexer", 0).addElement(new RunningTime()).addElement(progressBar);

            for (FlexibleBucket bucket : buckets) {
                bucket.clear();
                bucket.fill(Long.MAX_VALUE);
            }

            readingFinished.set(false);
            Thread readerThread = new Thread(() -> batchSupplier(sup, queue, settings.SEQUENCE_BATCH_SIZE));
            readerThread.start();

            Thread[] processingThreads = new Thread[settings.MAX_THREADS];
            for (int j = 0; j < settings.MAX_THREADS; j++) {
                processingThreads[j] = new Thread(new BatchProcessor(queue, tree, buckets, encoder, readingFinished, i, settings.BUCKETS_PER_CYCLE));
                processingThreads[j].start();
            }

            try {
                while (readerThread.isAlive()) {
                    readerThread.join(500);
                    progressBar.setProgress(sup.getBytesRead());
                }
                progressBar.finish();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            readingFinished.set(true);

            for (int j = 0; j < settings.MAX_THREADS; j++) {
                try {
                    processingThreads[j].join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

//            for (int j = 0; j < buckets.length; j++) {
//                logger.logInfo("Bucket " + (j + rangeStart) + " size: " + buckets[j].size());
//            }

            logger.logInfo("Sorting");
            try (ForkJoinPool pool = new ForkJoinPool(settings.MAX_THREADS)) {
                for (int j = indexStart; j < indexEnd; j++) {
                    pool.submit(new Sorting.MsdRadixTaskFlexibleBucket(buckets[j]));
                }
            }

            logger.logInfo("Writing");

            try (CustomThreadPoolExecutor executor = new CustomThreadPoolExecutor(
                    settings.MAX_THREADS, settings.MAX_THREADS, indexEnd + 1, 3600, logger)) {
                for (int j = indexStart; j < indexEnd; j++) {
                    int finalJ = j;
                    executor.submit(() -> writeBucket(buckets[finalJ], tree, encoder, dbIndexIO.getBucketIO(rangeStart + finalJ), bucketSizes));
                }
            }
        }

        // Export tree with number of kmers that map to each node
        TreeIO.saveTree(tree, dbIndexIO.getIndexFolder().resolve("tree.txt"));

        StringBuilder report = new StringBuilder("input file: ").append(sup.getFile()).append("\n")
                .append("output directory: ").append(dbIndexIO.getIndexFolder()).append("\n")
                .append("processed sequence records: ").append(processedSequences).append("\n")
                .append("skipped sequences because length < ").append(encoder.getK()).append(": ")
                .append(skippedSequences).append("\n");
        long totalKmers = 0;
        StringBuilder bucketSizesString = new StringBuilder().append("bucket sizes:").append("\n");
        for (int i = 0; i < bucketSizes.length; i++) {
            bucketSizesString.append(i).append("\t").append(bucketSizes[i]).append("\n");
            totalKmers += bucketSizes[i];
        }
        report.append("total extracted kmers:\t").append(totalKmers).append("\n");
        report.append(bucketSizesString);
        return report.toString();
    }

    private static void writeBucket(FlexibleBucket bucket, Tree tree, Encoder encoder, BucketIO bucketIO, int[] bucketSizes) {

        // skip unused entries in the front (should not happen)
        int i = 0;
        while (i < bucket.size() && bucket.getValue(i) == Long.MAX_VALUE) {
            i++;
        }
        if (i == bucket.size()) {
            return;
        }

        long lastIndexEntry = bucket.getValue(i++);
        long lastKmer = encoder.getKmerFromIndexEntry(lastIndexEntry);
        int lastTaxId = encoder.getIdFromIndexEntry(lastIndexEntry);
        FlexibleIntArray lastTaxIds = new FlexibleIntArray(10);
        lastTaxIds.add(lastTaxId);
        try (BucketIO.BucketWriter bucketWriter = bucketIO.getBucketWriter()) {
            for ( ; i < bucket.size(); i++) {
                long indexEntry = bucket.getValue(i);
                long kmer = encoder.getKmerFromIndexEntry(indexEntry);
                int taxId = encoder.getIdFromIndexEntry(indexEntry);
                if (indexEntry != Long.MAX_VALUE) {
                    if (kmer != lastKmer) {
                        if (lastTaxIds.size() == 1) {
                            bucketWriter.write(lastIndexEntry);
                            tree.addToProperty(lastTaxId, "kmers in database", 1);
                            lastTaxIds.clear();
                        } else if (lastTaxIds.size() > 1) {
                            lastTaxId = tree.findLCA(lastTaxIds.toArray());
                            bucketWriter.write(encoder.getIndexEntry(lastTaxId, lastKmer));
                            tree.addToProperty(lastTaxId, "kmers in database", 1);
                            lastTaxIds.clear();
                        }
                        lastIndexEntry = indexEntry;
                        lastKmer = kmer;
                        lastTaxId = taxId;
                        lastTaxIds.add(taxId);
                    } else {
                        if (lastTaxId != taxId) {
                            lastTaxIds.add(taxId);
                        }
                    }
                }
            }
            if (lastTaxIds.size() > 0 && lastIndexEntry != Long.MAX_VALUE) {
                lastTaxId = tree.findLCA(lastTaxIds.toArray());
                bucketWriter.write(encoder.getIndexEntry(lastTaxId, lastKmer));
                tree.addToProperty(lastTaxId, "kmers in database", 1);
            }
            bucketSizes[bucketIO.getName()] = bucketWriter.getLength();
        }
    }

    private static void batchSupplier(SequenceSupplier<Integer, byte[]> sup,
                                      BlockingQueue<FutureSequenceRecords<Integer, byte[]>[]> queue,
                                      int batchSize) {
        try {
            FutureSequenceRecords<Integer, byte[]>[] batch = new FutureSequenceRecords[batchSize];
            FutureSequenceRecords<Integer, byte[]> futureSequenceRecords;
            int batchIndex = 0;
            sup.reset();
            while ((futureSequenceRecords = sup.next()) != null) {
                batch[batchIndex++] = futureSequenceRecords;
                if (batchIndex == batchSize) {
                    try {
                        queue.put(batch);
                        processedSequences.addAndGet(batchSize);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    batchIndex = 0;
                    batch = new FutureSequenceRecords[batchSize];
                }
            }
            try {
                queue.put(batch);
                processedSequences.addAndGet(batchIndex);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class BatchProcessor implements Runnable {
        private final Logger logger;
        private final BlockingQueue<FutureSequenceRecords<Integer, byte[]>[]> queue;
        private final Tree tree;
        private final FlexibleBucket[] buckets;
        private final Encoder encoder;
        private final AtomicBoolean finished;
        private final int startBucket;
        private final int bucketsPerCycel;
        private final int[] bucketIndices;
        private final int[] maxBucketIndices;
        private final KmerExtractor kmerExtractor;
        private int pollFailCount;

        private BatchProcessor(BlockingQueue<FutureSequenceRecords<Integer, byte[]>[]> queue, Tree tree, FlexibleBucket[] buckets, Encoder encoder, AtomicBoolean finished, int startBucket, int bucketsPerCycel) {
            this.logger = new Logger("BatchProcessor");
            this.queue = queue;
            this.tree = tree;
            this.buckets = buckets;
            this.encoder = encoder;
            this.finished = finished;
            this.startBucket = startBucket;
            this.bucketsPerCycel = bucketsPerCycel;
            bucketIndices = new int[bucketsPerCycel];
            maxBucketIndices = new int[bucketsPerCycel];
            kmerExtractor = encoder.getKmerExtractor();
        }

        @Override
        public void run() {
            try {
                long[] extractKmers;
                int currentIndexOfMatchingBucket;
                int nextFreeIndex;
                while (true) {
                    // try to get batch for processing for 5 minutes
                    FutureSequenceRecords<Integer, byte[]>[] batch = queue.poll(100, TimeUnit.MILLISECONDS);
                    if (batch == null) {
                        if (finished.get()) { // iteration finished
                            break;
                        } else {
                            if (pollFailCount > 0 && pollFailCount % 500 == 0) {
                                logger.logWarning("BatchProcessor: Polling failed (" + pollFailCount + " ms)");
                            }
                            pollFailCount++;
                            if (pollFailCount++ > 30000) {
                                throw new RuntimeException("Polling failed for 5 minutes. Filesystem too slow?");
                            }
                            continue;
                        }
                    } else {
                        pollFailCount = 0;
                    }
                    for (FutureSequenceRecords<Integer, byte[]> futureSequenceRecords : batch) {
                        if (futureSequenceRecords == null) {
                            break;
                        }
                        for (SequenceRecord<Integer, byte[]> sequenceRecord : futureSequenceRecords.getSequenceRecords()) {
                            // sequence too short
                            if (sequenceRecord.sequence().length < encoder.getK()) {
                                skippedSequences.incrementAndGet();
                                continue;
                            }
                            int id = sequenceRecord.id();
                            // taxId not in the taxonomic tree
                            if (!tree.hasNode(id)) {
                                continue;
                            }
                            extractKmers = kmerExtractor.extractKmers(sequenceRecord.sequence());
                            for (long kmer : extractKmers) {
                                int bucketOfKmer = encoder.getBucketNameFromKmer(kmer);
                                if (bucketInRange(bucketOfKmer)) {
                                    currentIndexOfMatchingBucket = bucketOfKmer - startBucket;
                                    nextFreeIndex = getNextIndexInBucket(currentIndexOfMatchingBucket);
                                    buckets[currentIndexOfMatchingBucket].set(nextFreeIndex, encoder.getIndexEntry(id, encoder.getKmerWithoutBucketName(kmer)));
                                }
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                System.out.println(e);
                throw new RuntimeException(e);
            }
        }

        private boolean bucketInRange(int bucket) {
            return bucket >= startBucket && bucket < startBucket + bucketsPerCycel;
        }

        private int getNextIndexInBucket(int bucketIndex) {
            if (bucketIndices[bucketIndex] >= maxBucketIndices[bucketIndex]) {
                Pair<Integer, Integer> contingent = buckets[bucketIndex].getContingent();
//                System.out.println("[" + Thread.currentThread().getName() + "]" + "Got range " + contingent + " for bucket " + (startBucket + bucketIndex));
                bucketIndices[bucketIndex] = contingent.first();
                maxBucketIndices[bucketIndex] = contingent.last();
            }
            return bucketIndices[bucketIndex]++;
        }
    }
}