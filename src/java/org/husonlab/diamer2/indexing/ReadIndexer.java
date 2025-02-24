package org.husonlab.diamer2.indexing;

import org.husonlab.diamer2.indexing.kmers.KmerEncoder;
import org.husonlab.diamer2.indexing.kmers.KmerExtractor;
import org.husonlab.diamer2.io.indexing.ReadIndexIO;
import org.husonlab.diamer2.io.seq.FutureSequenceRecords;
import org.husonlab.diamer2.io.seq.HeaderToIdReader;
import org.husonlab.diamer2.io.seq.SequenceSupplier;
import org.husonlab.diamer2.main.GlobalSettings;
import org.husonlab.diamer2.main.encoders.Encoder;
import org.husonlab.diamer2.seq.Sequence;
import org.husonlab.diamer2.seq.SequenceRecord;
import org.husonlab.diamer2.util.logging.*;

import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class to create an index for sequencing reads.
 */
public class ReadIndexer {

    private final Logger logger;
    private final SequenceSupplier<Integer, Character, Byte> sup;
    private final HeaderToIdReader fastqIdReader;
    private final ReadIndexIO readIndexIO;
    private final Encoder encoder;
    private final GlobalSettings globalSettings;
    private int processedReads;
    private final AtomicInteger processedTranslations;
    private final AtomicInteger skippedTranslations;

    public ReadIndexer(SequenceSupplier<Integer, Character, Byte> sup,
                       HeaderToIdReader fastqIdReader,
                       Path indexDir,
                       Encoder encoder,
                       GlobalSettings globalSettings) {
        this.logger = new Logger("ReadIndexer");
        logger.addElement(new Time());

        this.sup = sup;
        this.fastqIdReader = fastqIdReader;
        this.readIndexIO = new ReadIndexIO(indexDir, encoder.getNumberOfBuckets());
        this.encoder = encoder;
        this.globalSettings = globalSettings;
        this.processedTranslations = new AtomicInteger(0);
        this.skippedTranslations = new AtomicInteger(0);
    }

    /**
     * Indexes a file of sequencing reads.
     * @throws IOException If an error occurs during reading the file or writing the buckets.
     */
    public ReadIndexIO index() throws IOException {
        logger.logInfo("Indexing " + sup.getFile() + " to " + readIndexIO.getIndexFolder());

        try (ThreadPoolExecutor threadPoolExecutor = new CustomThreadPoolExecutor(
                        globalSettings.MAX_THREADS, globalSettings.MAX_THREADS, globalSettings.QUEUE_SIZE, 1, logger)) {
            Phaser indexPhaser = new Phaser(1);

            ProgressBar progressBar = new ProgressBar(sup.getFileSize(), 20);
            ProgressLogger progressLogger = new ProgressLogger("reads");
            new OneLineLogger("ReadIndexer", 1000)
                    .addElement(new RunningTime())
                    .addElement(progressBar)
                    .addElement(progressLogger);

            // Initialize Concurrent LinkedQueue to store index entries of each bucket
            final ConcurrentLinkedQueue<Long>[] bucketLists = new ConcurrentLinkedQueue[globalSettings.BUCKETS_PER_CYCLE];

            final int maxBuckets = encoder.getNumberOfBuckets();
            for (int i = 0; i < maxBuckets; i += globalSettings.BUCKETS_PER_CYCLE) {
                processedReads = 0;
                skippedTranslations.set(0);
                processedTranslations.set(0);
                int rangeStart = i;
                int rangeEnd = Math.min(i + globalSettings.BUCKETS_PER_CYCLE, maxBuckets);
                logger.logInfo("Indexing buckets " + rangeStart + " - " + rangeEnd);
                progressBar.setProgress(0);

                // Initialize a list for each bucket that is processed in this iteration
                for (int j = 0; j < globalSettings.BUCKETS_PER_CYCLE; j++) {
                    bucketLists[j] = new ConcurrentLinkedQueue<>();
                }

                // initialize batch of FutureSequenceRecords
                FutureSequenceRecords<Integer, Byte>[] batch = new FutureSequenceRecords[globalSettings.SEQUENCE_BATCH_SIZE];
                int batchIndex = 0;

                FutureSequenceRecords<Integer, Byte> futureSequenceRecords;
                while ((futureSequenceRecords = sup.next()) != null) {
                    processedReads ++;
                    batch[batchIndex] = futureSequenceRecords;
                    progressBar.setProgress(sup.getBytesRead());
                    progressLogger.incrementProgress();

                    // submit full batch of FutureSequenceRecords to thread pool
                    if (batchIndex > globalSettings.SEQUENCE_BATCH_SIZE - 2) {
                        indexPhaser.register();
                        threadPoolExecutor.submit(
                                new ReadProcessor(indexPhaser, batch, encoder, bucketLists, rangeStart, rangeEnd,
                                        processedTranslations, skippedTranslations)
                        );
                        batch = new FutureSequenceRecords[globalSettings.SEQUENCE_BATCH_SIZE];
                        batchIndex = 0;
                    } else {
                        batchIndex++;
                    }
                }
                // submit last batch
                indexPhaser.register();
                threadPoolExecutor.submit(
                        new ReadProcessor(
                                indexPhaser,
                                Arrays.copyOfRange(batch, 0, batchIndex),
                                encoder,
                                bucketLists,
                                rangeStart,
                                rangeEnd, processedTranslations, skippedTranslations));
                progressBar.finish();
                indexPhaser.arriveAndAwaitAdvance();
                logger.logInfo("Converting, sorting and writing buckets " + rangeStart + " - " + rangeEnd);

                // write read header map during first iteration
                if (rangeStart == 0) {
                    indexPhaser.register();
                    threadPoolExecutor.submit(() -> {
                        try {
                            readIndexIO.writeReadHeaderMapping(fastqIdReader.getHeaders());
                        } catch (RuntimeException e) {
                            throw new RuntimeException("Error writing read header map.", e);
                        } finally {
                            indexPhaser.arriveAndDeregister();
                        }
                    });
                }
                indexPhaser.arriveAndAwaitAdvance();

                // sort and write buckets
                for (int j = 0; j < Math.min(globalSettings.BUCKETS_PER_CYCLE, (maxBuckets - rangeStart)); j++) {
                    int finalJ = j;
                    indexPhaser.register();
                    threadPoolExecutor.submit(() -> {
                        try {
                            readIndexIO.getBucketIO(rangeStart + finalJ).write(new Bucket(rangeStart + finalJ, bucketLists[finalJ]));
                        } catch (RuntimeException | IOException e) {
                            throw new RuntimeException("Error converting and writing bucket " + finalJ, e);
                        } finally {
                            indexPhaser.arriveAndDeregister();
                        }
                    });
                }
                indexPhaser.arriveAndAwaitAdvance();
                sup.reset();
            }
        }
        logger.logInfo("Processed Reads: " + processedReads);
        logger.logInfo("Processed translations: " + processedTranslations.get());
        logger.logInfo("Skipped translations because length < " + encoder.getK() + ": " + skippedTranslations.get());
        return readIndexIO;
    }

    private static class ReadProcessor implements Runnable {

        private final Phaser phaser;
        private final FutureSequenceRecords<Integer, Byte>[] batch;
        private final Encoder encoder;
        private final KmerExtractor kmerExtractor;
        private final ConcurrentLinkedQueue<Long>[] bucketLists;
        private final int rangeStart;
        private final int rangeEnd;
        private final AtomicInteger processedTranslations;
        private final AtomicInteger skippedTranslations;

        public ReadProcessor(
                Phaser phaser,
                FutureSequenceRecords<Integer, Byte>[] batch,
                Encoder encoder,
                ConcurrentLinkedQueue<Long>[] bucketLists,
                int rangeStart,
                int rangeEnd,
                AtomicInteger processedTranslations,
                AtomicInteger skippedTranslations) {
            this.phaser = phaser;
            this.batch = batch;
            this.encoder = encoder;
            this.kmerExtractor = new KmerExtractor(new KmerEncoder(encoder.getTargetAlphabet().getBase(), encoder.getMask()));
            this.bucketLists = bucketLists;
            this.rangeStart = rangeStart;
            this.rangeEnd = rangeEnd;
            this.processedTranslations = processedTranslations;
            this.skippedTranslations = skippedTranslations;
        }

        @Override
        public void run() {
            try {
                for (FutureSequenceRecords<Integer, Byte> futureSequenceRecords : batch) {
                    for (SequenceRecord<Integer, Byte> record: futureSequenceRecords.getSequenceRecords()) {
                        if (record == null || record.sequence().length() < encoder.getK()) {
                            skippedTranslations.incrementAndGet();
                        } else {
                            processedTranslations.incrementAndGet();
                            int id = record.id();
                            Sequence<Byte> sequence = record.sequence();
                            long[] kmers = kmerExtractor.extractKmers(sequence);
                            for (long kmer : kmers) {
                                int bucketName = encoder.getBucketNameFromKmer(kmer);
                                if (bucketName >= rangeStart && bucketName < rangeEnd) {
                                    bucketLists[bucketName - rangeStart].add(encoder.getIndex(id, kmer));
                                }
                            }
                        }
                    }
                }
            } finally {
                phaser.arriveAndDeregister();
            }
        }
    }
}

