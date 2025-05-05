package org.husonlab.diamer2.indexing;

import org.husonlab.diamer2.indexing.kmers.KmerExtractor;
import org.husonlab.diamer2.io.seq.FutureSequenceRecords;
import org.husonlab.diamer2.io.seq.SequenceSupplier;
import org.husonlab.diamer2.main.GlobalSettings;
import org.husonlab.diamer2.main.encoders.Encoder;
import org.husonlab.diamer2.seq.SequenceRecord;
import org.husonlab.diamer2.taxonomy.Tree;
import org.husonlab.diamer2.util.logging.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class DBAnalyzer {

    private final Logger logger;
    private final SequenceSupplier<Integer, byte[]> sup;
    private final Tree tree;
    private final Encoder encoder;
    private final GlobalSettings settings;
    BlockingQueue<FutureSequenceRecords<Integer, byte[]>[]> queue;
    private AtomicBoolean finished = new AtomicBoolean(false);
    private static final AtomicInteger processedSequenceRecords = new AtomicInteger(0);
    private static final AtomicInteger skippedSequenceRecords = new AtomicInteger(0);
    private final long[] bucketSizes;
    private long totalKmers;
    private long maxBucketSize;
    private final long[] charCounts;
    private long totalCharCount;

    public DBAnalyzer(SequenceSupplier<Integer, byte[]> sup,
                      Tree tree,
                      Encoder encoder,
                      GlobalSettings settings) {
        logger = new Logger("DBAnalyzer");
        logger.addElement(new Time());
        this.sup = sup;
        this.tree = tree;
        this.encoder = encoder;
        this.settings = settings;
        this.bucketSizes = new long[encoder.getNrOfBuckets()];
        this.charCounts = new long[encoder.getTargetAlphabet().getBase()];
        queue = new ArrayBlockingQueue<>(settings.MAX_IO_THREADS * 10, false);
    }

    public String analyze() {
        logger.logInfo("Calculating bucket sizes for: " + sup.getFile());
        ProgressBar progressBar = new ProgressBar(sup.getFileSize(), 20);
        new OneLineLogger("DBAnalyzer", 0).addElement(new RunningTime()).addElement(progressBar);
        Thread readerThread = new Thread(() -> batchSupplier(sup, queue, settings.SEQUENCE_BATCH_SIZE));
        readerThread.start();

        BatchProcessor[] batchProcessors = new BatchProcessor[settings.MAX_IO_THREADS];
        Thread[] processingThreads = new Thread[settings.MAX_IO_THREADS];
        for (int j = 0; j < settings.MAX_IO_THREADS; j++) {
            batchProcessors[j] = new BatchProcessor(queue, tree, encoder, finished);
            processingThreads[j] = new Thread(batchProcessors[j]);
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
        finished.set(true);

        for (int j = 0; j < settings.MAX_IO_THREADS; j++) {
            try {
                processingThreads[j].join();
                long[] threadBucketSizes = batchProcessors[j].getBucketSizes();
                for (int i = 0; i < threadBucketSizes.length; i++) {
                    bucketSizes[i] += threadBucketSizes[i];
                    totalKmers += threadBucketSizes[i];
                }
                long[] threadCharCounts = batchProcessors[j].getCharCounts();
                for (int i = 0; i < threadCharCounts.length; i++) {
                    charCounts[i] += threadCharCounts[i];
                    totalCharCount += threadCharCounts[i];
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        StringBuilder report = new StringBuilder("input file: ").append(sup.getFile()).append("\n")
                .append("output directory: ").append(encoder.getDBIndexIO().getIndexFolder()).append("\n")
                .append("processed sequenceRecords: ").append(processedSequenceRecords).append("\n");
        StringBuilder bucketSizesString = new StringBuilder().append("raw bucket sizes: ").append("\n");
        for (int i = 0; i < bucketSizes.length; i++) {
            if (bucketSizes[i] > maxBucketSize) {
                maxBucketSize = bucketSizes[i];
            }
            bucketSizesString.append(i).append("\t").append(bucketSizes[i]).append("\n");
        }
        StringBuilder charCountsString = new StringBuilder().append("char counts: ").append("\n");
        StringBuilder charCountsStringRelative = new StringBuilder().append("char counts relative: ").append("\n");
        for (int i = 0; i < charCounts.length; i++) {
            charCountsString.append(i).append("\t").append(charCounts[i]).append("\n");
            charCountsStringRelative.append(i).append("\t").append(charCounts[i] / (double)totalCharCount).append("\n");
        }
        report.append("total raw kmers:\t").append(totalKmers).append("\n");
        report.append("max bucket size:\t").append(maxBucketSize).append("\n");
        report.append(bucketSizesString);
        report.append("total char count:\t").append(totalCharCount).append("\n");
        report.append(charCountsString);
        report.append(charCountsStringRelative);
        return report.toString();
    }

    public long getTotalCharCount() {
        return totalCharCount;
    }

    public long[] getCharCounts() {
        return charCounts;
    }

    public long getTotalKmers() {
        return totalKmers;
    }

    public long[] getBucketSizes() {
        return bucketSizes;
    }

    public long getMaxBucketSize() {
        return maxBucketSize;
    }

    public int suggestNrOfBuckets() {
        Runtime runtime = Runtime.getRuntime();
        long freeMemory = (runtime.maxMemory() - runtime.totalMemory()) + runtime.freeMemory();
        freeMemory -= (long) (freeMemory * 0.2); // 20 % for the JVM
        // each bucket entry = 8 byte (kmer) + 4 byte (id) + 10 %
        long bucketArraySizes = (maxBucketSize + 1_000L * settings.MAX_IO_THREADS);
        return (int) Math.min(Math.floor(freeMemory / (bucketArraySizes * 14d)), encoder.getNrOfBuckets());
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
        private final Logger logger;
        private final BlockingQueue<FutureSequenceRecords<Integer, byte[]>[]> queue;
        private final long[] bucketSizes;
        private final long[] charCounts;
        private final Tree tree;
        private final Encoder encoder;
        private final AtomicBoolean finished;
        private final KmerExtractor kmerExtractor;
        private int pollFailCount;

        private BatchProcessor(BlockingQueue<FutureSequenceRecords<Integer, byte[]>[]> queue, Tree tree, Encoder encoder, AtomicBoolean finished) {
            this.logger = new Logger("BatchProcessor");
            this.queue = queue;
            this.bucketSizes = new long[encoder.getNrOfBuckets()];
            this.charCounts = new long[encoder.getTargetAlphabet().getBase()];
            this.tree = tree;
            this.encoder = encoder;
            this.finished = finished;
            kmerExtractor = encoder.getKmerExtractor();
        }

        @Override
        public void run() {
            try {
                long[] extractedKmers;
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
                            int taxId = sequenceRecord.id();
                            if (!tree.hasNode(taxId)) {
                                skippedSequenceRecords.incrementAndGet();
                                break;
                            }
                            for (byte c : sequenceRecord.sequence()) {
                                charCounts[c]++;
                            }
                            extractedKmers = kmerExtractor.extractKmers(sequenceRecord.sequence());
                            for (long kmer : extractedKmers) {
                                int bucketOfKmer = encoder.getBucketNameFromKmer(kmer);
                                bucketSizes[bucketOfKmer]++;
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        public long[] getBucketSizes() {
            return bucketSizes;
        }
        public long[] getCharCounts() {
            return charCounts;
        }
    }
}