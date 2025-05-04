package org.husonlab.diamer2.indexing;

import org.husonlab.diamer2.indexing.kmers.KmerExtractor;
import org.husonlab.diamer2.io.indexing.BucketIO;
import org.husonlab.diamer2.io.indexing.ReadIndexIO;
import org.husonlab.diamer2.io.seq.FastqIdReader;
import org.husonlab.diamer2.io.seq.FutureSequenceRecords;
import org.husonlab.diamer2.io.seq.SequenceSupplier;
import org.husonlab.diamer2.main.GlobalSettings;
import org.husonlab.diamer2.main.encoders.Encoder;
import org.husonlab.diamer2.seq.SequenceRecord;
import org.husonlab.diamer2.util.FlexibleBucket;
import org.husonlab.diamer2.util.Pair;
import org.husonlab.diamer2.util.logging.*;

import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ReadIndexer2 {

    private final Logger logger;
    /**
     * Expected size of Bucket because of multithreading.
     */
    private final int expectedBucketSize;
    private final static int contingentSizes = 1_024;
    private final SequenceSupplier<Integer, byte[]> sup;
    private final FastqIdReader fastqIdReader;
    private final Encoder encoder;
    private final GlobalSettings settings;
    BlockingQueue<FutureSequenceRecords<Integer, byte[]>[]> queue;
    private final FlexibleBucket[] buckets;
    private AtomicBoolean readingFinished = new AtomicBoolean(false);
    private final ReadIndexIO readIndexIO;
    private final int[] bucketSizes;
    private static final AtomicInteger processedReads = new AtomicInteger(0);
    private static final AtomicInteger processedTranslations = new AtomicInteger(0);
    private static final AtomicInteger skippedTranslations = new AtomicInteger(0);

    public ReadIndexer2(SequenceSupplier<Integer, byte[]> sup,
                        FastqIdReader fastqIdReader,
                        long maxBucketSize,
                        Encoder encoder,
                        GlobalSettings settings) {
        logger = new Logger("DBIndexer2");
        logger.addElement(new Time()).addElement(new RunningTime());
        this.expectedBucketSize = (int) maxBucketSize + contingentSizes * settings.MAX_THREADS;
        this.sup = sup;
        this.fastqIdReader = fastqIdReader;
        this.encoder = encoder;
        readIndexIO = encoder.getReadIndexIO();
        this.settings = settings;
        queue = new ArrayBlockingQueue<>(settings.MAX_THREADS * 10, false);
        logger.logInfo("Allocating memory for " + settings.BUCKETS_PER_CYCLE + " buckets of size " + expectedBucketSize);
        buckets = new FlexibleBucket[settings.BUCKETS_PER_CYCLE];
        for (int i = 0; i < settings.BUCKETS_PER_CYCLE; i++) {
            buckets[i] = new FlexibleBucket(expectedBucketSize, expectedBucketSize, contingentSizes);
        }
        bucketSizes = new int[encoder.getNrOfBuckets()];
    }

    public String index() {
        for (int i = 0; i < encoder.getNrOfBuckets(); i += settings.BUCKETS_PER_CYCLE) {
            processedReads.set(0);
            int rangeStart = i;
            int rangeEnd = Math.min(i + settings.BUCKETS_PER_CYCLE, encoder.getNrOfBuckets());
            int indexStart = 0;
            int indexEnd = rangeEnd - i;

            logger.logInfo("Indexing buckets " + i + " to " + (rangeEnd - 1));
            ProgressBar progressBar = new ProgressBar(sup.getFileSize(), 20);
            new OneLineLogger("ReadIndexer2", 0).addElement(new RunningTime()).addElement(progressBar);

            for (FlexibleBucket bucket : buckets) {
                bucket.clear();
                bucket.fill(Long.MAX_VALUE);
            }

            readingFinished.set(false);
            Thread readerThread = new Thread(() -> batchSupplier(sup, queue, settings.SEQUENCE_BATCH_SIZE));
            readerThread.start();

            Thread[] processingThreads = new Thread[settings.MAX_THREADS];
            for (int j = 0; j < settings.MAX_THREADS; j++) {
                processingThreads[j] = new Thread(new BatchProcessor(queue, buckets, encoder, readingFinished, i, settings.BUCKETS_PER_CYCLE));
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

            // write a read header map during the first iteration
            if (rangeStart == 0) {
                logger.logInfo("Writing read header map");
                new Thread(() -> {
                    try {
                        readIndexIO.writeReadHeaderMapping(fastqIdReader.getHeaders());
                        // remove headers from memory
                        fastqIdReader.removeHeaders();
                    } catch (RuntimeException e) {
                        throw new RuntimeException("Error writing read header map.", e);
                    }
                }).start();
            }

            for (int j = 0; j < settings.MAX_THREADS; j++) {
                try {
                    processingThreads[j].join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

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
                    executor.submit(() -> writeBucket(buckets[finalJ], encoder, readIndexIO.getBucketIO(rangeStart + finalJ), bucketSizes));
                }
            }
        }

        StringBuilder report = new StringBuilder("input file: ").append(sup.getFile()).append("\n")
                .append("output directory: ").append(readIndexIO.getIndexFolder()).append("\n")
                .append("processed reads: ").append(processedReads).append("\n")
                .append("processed translations: ").append(processedTranslations).append("\n")
                .append("skipped translations because length < ").append(encoder.getK()).append(": ")
                .append(skippedTranslations).append("\n");
        long totalKmers = 0;
        StringBuilder bucketSizesString = new StringBuilder().append("bucket sizes: ").append("\n");
        for (int i = 0; i < bucketSizes.length; i++) {
            bucketSizesString.append(i).append("\t").append(bucketSizes[i]).append("\n");
            totalKmers += bucketSizes[i];
        }
        report.append("total extracted kmers:\t").append(totalKmers).append("\n");
        report.append(bucketSizesString);
        return report.toString();
    }

    private static void writeBucket(FlexibleBucket bucket, Encoder encoder, BucketIO bucketIO, int[] bucketSizes) {
        int lastKmerStartIndex = 0;
        try (BucketIO.BucketWriter bucketWriter = bucketIO.getBucketWriter()) {
            for (int i = 0; i < bucket.size(); i++) {
                long indexEntry = bucket.getValue(i);
                if (indexEntry != Long.MAX_VALUE) {
                    bucketWriter.write(indexEntry);
                }
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
                        processedReads.addAndGet(batchSize);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    batchIndex = 0;
                    batch = new FutureSequenceRecords[batchSize];
                }
            }
            try {
                queue.put(batch);
                processedReads.addAndGet(batchIndex);
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
        private final FlexibleBucket[] buckets;
        private final Encoder encoder;
        private final AtomicBoolean finished;
        private final int startBucket;
        private final int bucketsPerCycel;
        private final int[] bucketIndices;
        private final int[] maxBucketIndices;
        private final KmerExtractor kmerExtractor;
        private int pollFailCount;

        private BatchProcessor(BlockingQueue<FutureSequenceRecords<Integer, byte[]>[]> queue, FlexibleBucket[] buckets, Encoder encoder, AtomicBoolean finished, int startBucket, int bucketsPerCycel) {
            this.logger = new Logger("BatchProcessor");
            this.queue = queue;
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
                    FutureSequenceRecords<Integer, byte[]>[] batch = queue.poll(100, TimeUnit.MILLISECONDS);
                    if (batch == null) {
                        if (finished.get()) {
                            break;
                        } else {
                            if (pollFailCount > 0 && pollFailCount % 500 == 0) {
                                logger.logWarning("BatchProcessor: Polling failed (" + pollFailCount + " ms)");
                            }
                            pollFailCount++;
                            if (pollFailCount++ > 30000) {
                                throw new RuntimeException("Polling failed for 5 minutes. Supplier too slow?");
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
                            if (sequenceRecord.sequence().length < encoder.getK()) {
                                skippedTranslations.incrementAndGet();
                                continue;
                            } else {
                                processedTranslations.incrementAndGet();
                            }
                            int id = sequenceRecord.id();
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