package org.husonlab.diamer2.indexing;

import org.husonlab.diamer2.indexing.kmers.KmerExtractor;
import org.husonlab.diamer2.io.indexing.BucketIO;
import org.husonlab.diamer2.io.indexing.DBIndexIO;
import org.husonlab.diamer2.io.seq.FutureSequenceRecords;
import org.husonlab.diamer2.io.seq.SequenceSupplier;
import org.husonlab.diamer2.io.taxonomy.TreeIO;
import org.husonlab.diamer2.main.GlobalSettings;
import org.husonlab.diamer2.main.encoders.Encoder;
import org.husonlab.diamer2.seq.SequenceRecord;
import org.husonlab.diamer2.taxonomy.Tree;
import org.husonlab.diamer2.util.Pair;
import org.husonlab.diamer2.util.logging.Logger;
import org.husonlab.diamer2.util.logging.OneLineLogger;
import org.husonlab.diamer2.util.logging.ProgressBar;
import org.husonlab.diamer2.util.logging.RunningTime;

import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.husonlab.diamer2.indexing.Sorting2.radixInPlaceParallel;

public class DBIndexer2 {

    private final Logger logger;
    private final static int expectedKmerCount = 300_000_000;
    private final static int contingentSizes = 100_000;
    private final SequenceSupplier<Integer, byte[]> sup;
    private final Tree tree;
    private final Encoder encoder;
    private final GlobalSettings settings;
    private final int parallelism;
    BlockingQueue<FutureSequenceRecords<Integer, byte[]>[]> queue;
    private final ContingentDistributor distributor;
    private long[][] kmers;
    private int[][] taxIds;
    private AtomicBoolean finished = new AtomicBoolean(false);
    private final DBIndexIO dbIndexIO;
    private final int[] bucketSizes;
    private static final AtomicInteger processedSequenceRecords = new AtomicInteger(0);
    private static final AtomicInteger skippedSequenceRecords = new AtomicInteger(0);

    public DBIndexer2(SequenceSupplier<Integer, byte[]> sup,
                     Tree tree,
                     Encoder encoder,
                     GlobalSettings settings) {
        logger = new Logger("DBIndexer2");
        logger.addElement(new RunningTime());
        this.sup = sup;
        this.tree = tree;
        this.encoder = encoder;
        dbIndexIO = encoder.getDBIndexIO();
        this.settings = settings;
        parallelism = Math.min(settings.MAX_THREADS, Math.min(settings.BUCKETS_PER_CYCLE, encoder.getNrOfBuckets()));
        queue = new ArrayBlockingQueue<>(parallelism * 10, false);
        distributor = new ContingentDistributor(parallelism, expectedKmerCount, contingentSizes);
        kmers = new long[settings.BUCKETS_PER_CYCLE][];
        taxIds = new int[settings.BUCKETS_PER_CYCLE][];
        for (int i = 0; i < settings.BUCKETS_PER_CYCLE; i++) {
            kmers[i] = new long[expectedKmerCount];
            taxIds[i] = new int[expectedKmerCount];
        }
        bucketSizes = new int[encoder.getNrOfBuckets()];
    }

    public String index() {
        tree.addLongProperty("kmers in database", 0);

        for (int i = 0; i < encoder.getNrOfBuckets(); i += settings.BUCKETS_PER_CYCLE) {
            processedSequenceRecords.set(0);
            skippedSequenceRecords.set(0);
            int rangeStart = i;
            int rangeEnd = Math.min(i + settings.BUCKETS_PER_CYCLE, encoder.getNrOfBuckets());
            int indexEnd = rangeEnd - i;

            logger.logInfo("Indexing buckets " + i + " to " + (rangeEnd - 1));
            ProgressBar progressBar = new ProgressBar(sup.getFileSize(), 20);
            new OneLineLogger("DBIndexer2", 0).addElement(new RunningTime()).addElement(progressBar);

            for (int j = 0; j < settings.BUCKETS_PER_CYCLE; j++) {
                Arrays.fill(taxIds[j], -1);
            }
            distributor.reset();

            Thread readerThread = new Thread(() -> batchSupplier(sup, queue, settings.SEQUENCE_BATCH_SIZE));
            readerThread.start();
//            Thread readerThread = Thread.startVirtualThread(() -> batchSupplier(sup, queue, settings.SEQUENCE_BATCH_SIZE));


            Thread[] processingThreads = new Thread[settings.MAX_THREADS];
            for (int j = 0; j < settings.MAX_THREADS; j++) {
                processingThreads[j] = new Thread(new BatchProcessor(queue, kmers, taxIds, distributor, tree, encoder, finished, i, settings.BUCKETS_PER_CYCLE));
                processingThreads[j].start();
//                processingThreads[j] = Thread.startVirtualThread(new BatchProcessor(queue, kmers, taxIds, distributor, tree, encoder, finished, i, parallelism));
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
            finished.set(true);

            for (int j = 0; j < settings.MAX_THREADS; j++) {
                try {
                    processingThreads[j].join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            logger.logInfo("Sorting");
            Thread[] sortingThreads = new Thread[indexEnd];
            for (int j = 0; j < indexEnd; j++) {
                int finalJ = j;
                sortingThreads[j] = new Thread(() -> radixInPlaceParallel(kmers[finalJ], taxIds[finalJ], settings.MAX_THREADS));
                sortingThreads[j].start();
            }

            for (int j = 0; j < indexEnd; j++) {
                try {
                    sortingThreads[j].join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            logger.logInfo("Writing");
            Thread[] writingThreads = new Thread[indexEnd];
            for (int j = 0; j < indexEnd; j++) {
                int finalJ = j;
                writingThreads[j] = new Thread(() -> writeBucket(kmers[finalJ], taxIds[finalJ], tree, encoder, dbIndexIO.getBucketIO(rangeStart + finalJ), bucketSizes));
                writingThreads[j].start();
//                writingThreads[j] = Thread.startVirtualThread(() -> writeBucket(kmers[finalJ], taxIds[finalJ], tree, encoder, dbIndexIO.getBucketIO(rangeStart + finalJ), bucketSizes));
            }

            for (int j = 0; j < indexEnd; j++) {
                try {
                    writingThreads[j].join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        // Export tree with number of kmers that map to each node
        TreeIO.saveTree(tree, dbIndexIO.getIndexFolder().resolve("tree.txt"));

        StringBuilder report = new StringBuilder("input file: ").append(sup.getFile()).append("\n")
                .append("output directory: ").append(dbIndexIO.getIndexFolder()).append("\n")
                .append("processed sequenceRecords: ").append(processedSequenceRecords).append("\n");
        long totalKmers = 0;
        StringBuilder bucketSizesString = new StringBuilder().append("bucket sizes: ").append("\n");
        for (int i = 0; i < bucketSizes.length; i++) {
            bucketSizesString.append(i).append("\t").append(bucketSizes[i]).append("\n");
            totalKmers += bucketSizes[i];
        }
        report.append("total extracted kmers: ").append(totalKmers).append("\n");
        report.append(bucketSizesString);
        return report.toString();
    }

    private static void writeBucket(long[] kmers, int[] taxIds, Tree tree, Encoder encoder, BucketIO bucketIO, int[] bucketSizes) {
        int lastKmerStartIndex = 0;
        long lastKmer = kmers[0];
        int lastTaxId = taxIds[0];
        try (BucketIO.BucketWriter bucketWriter = bucketIO.getBucketWriter()) {
            for (int i = 1; i < kmers.length; i++) {
                if (lastKmer != kmers[i] || taxIds[i] == -1) {
                    if (i - lastKmerStartIndex > 1) {
                        lastTaxId = tree.findLCA(Arrays.copyOfRange(taxIds, lastKmerStartIndex, i));
                    }
                    if (lastTaxId != -1) {
                        bucketWriter.write(encoder.getIndex(lastTaxId, lastKmer));
                        tree.addToProperty(lastTaxId, "kmers in database", 1);
                    }
                    lastKmerStartIndex = i;
                    lastKmer = kmers[i];
                    lastTaxId = taxIds[i];
                }
            }
            if (kmers.length - lastKmerStartIndex > 1) {
                lastTaxId = tree.findLCA(Arrays.copyOfRange(taxIds, lastKmerStartIndex, kmers.length));
            }
            if (lastTaxId != -1) {
                bucketWriter.write(encoder.getIndex(lastTaxId, lastKmer));
                tree.addToProperty(lastTaxId, "kmers in database", 1);
            }

            bucketSizes[bucketIO.getName()] = bucketWriter.getLength();
//            System.out.println("Bucket size " + bucketIO.getName() + ": " + bucketWriter.getLength());
        }
    }

    private static void batchSupplier(SequenceSupplier<Integer, byte[]> sup,
                                      BlockingQueue<FutureSequenceRecords<Integer, byte[]>[]> queue,
                                      int batchSize) {
        try {
            FutureSequenceRecords<Integer, byte[]>[] batch = new FutureSequenceRecords[batchSize];
            FutureSequenceRecords<Integer, byte[]> futureSequenceRecords;
            int batchIndex = 0;
            while ((futureSequenceRecords = sup.next()) != null) {
                batch[batchIndex++] = futureSequenceRecords;
                if (batchIndex == batchSize) {
                    try {
                        queue.put(batch);
                        processedSequenceRecords.addAndGet(batchSize);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    batchIndex = 0;
                    batch = new FutureSequenceRecords[batchSize];
                }
            }
            try {
                queue.put(batch);
                processedSequenceRecords.addAndGet(batchIndex);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            sup.reset();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class BatchProcessor implements Runnable {

        private final BlockingQueue<FutureSequenceRecords<Integer, byte[]>[]> queue;
        private final long[][] kmers;
        private final int[][] taxIds;
        private final ContingentDistributor contingentDistributor;
        private final Tree tree;
        private final Encoder encoder;
        private final AtomicBoolean finished;
        private final int startBucket;
        private final int bucketsPerCycel;
        private final int[] bucketIndices;
        private final int[] maxBucketIndices;
        private final KmerExtractor kmerExtractor;
        private int pollFailCount;

        private BatchProcessor(BlockingQueue<FutureSequenceRecords<Integer, byte[]>[]> queue, long[][] kmers, int[][] taxIds, ContingentDistributor contingentDistributor, Tree tree, Encoder encoder, AtomicBoolean finished, int startBucket, int bucketsPerCycel) {
            this.queue = queue;
            this.kmers = kmers;
            this.taxIds = taxIds;
            this.contingentDistributor = contingentDistributor;
            this.tree = tree;
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
                while (true) {
                    FutureSequenceRecords<Integer, byte[]>[] batch = queue.poll(2, TimeUnit.SECONDS);
                    if (batch == null) {
                        if (finished.get()) {
                            break;
                        } else {
                            pollFailCount++;
                            if (pollFailCount++ > 10) {
                                throw new RuntimeException("Polling failed too many times. Supplier too slow?");
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
                            int taxId = sequenceRecord.id();
                            if (!tree.hasNode(taxId)) {
                                skippedSequenceRecords.incrementAndGet();
                                break;
                            }
                            for (long kmer : kmerExtractor.extractKmers(sequenceRecord.sequence())) {
                                int bucketOfKmer = encoder.getBucketNameFromKmer(kmer);
                                if (bucketInRange(bucketOfKmer)) {
                                    int currentIndexOfMatchingBucket = bucketOfKmer - startBucket;
                                    int nextFreeIndex = getNextIndexInBucket(currentIndexOfMatchingBucket);
                                    kmers[currentIndexOfMatchingBucket][nextFreeIndex] = kmer;
                                    taxIds[currentIndexOfMatchingBucket][nextFreeIndex] = taxId;
                                }
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        private boolean bucketInRange(int bucket) {
            return bucket >= startBucket && bucket < startBucket + bucketsPerCycel;
        }

        private int getNextIndexInBucket(int bucketIndex) {
            if (bucketIndices[bucketIndex] >= maxBucketIndices[bucketIndex]) {
                Pair<Integer, Integer> contingent = contingentDistributor.getContingent(bucketIndex);
//                System.out.println("[" + Thread.currentThread().getName() + "]" + "Got range " + contingent + " for bucket " + (startBucket + bucketIndex));
                bucketIndices[bucketIndex] = contingent.first();
                maxBucketIndices[bucketIndex] = contingent.last();
            }
            return bucketIndices[bucketIndex]++;
        }
    }

    private static class ContingentDistributor {
        private final int[] counters;
        private final Object[] counterLocks;
        private final int arraySizes;
        private final int contingentSizes;
        public ContingentDistributor(int numberOfArrays, int arraySizes, int contingentSizes) {
            counters = new int[numberOfArrays];
            counterLocks = new Object[numberOfArrays];
            for (int i = 0; i < numberOfArrays; i++) {
                counterLocks[i] = new Object();
            }
            this.arraySizes = arraySizes;
            this.contingentSizes = contingentSizes;
        }

        public Pair<Integer, Integer> getContingent(int arrayIndex) {
            int count;
            synchronized (counterLocks[arrayIndex]) {
                count = counters[arrayIndex]++;
            }
            int leftIndex = count * contingentSizes;
            if (leftIndex >= arraySizes) {
                throw new RuntimeException("Out of indices to distribute.");
            }
            return new Pair<>(leftIndex, count * contingentSizes + contingentSizes);
        }

        public void reset() {
            Arrays.fill(counters, 0);
        }
    }
}
