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
    private int nrOfProcessedReads;
    private final AtomicInteger nrOfProcessedTranslations;
    private final AtomicInteger nrOfSkippedTranslations;

    /**
     * @param sup SequenceSupplier
     * @param fastqIdReader The {@link HeaderToIdReader} used for the SequenceSupplier to be able to access the read headers.
     * @param indexDir The directory where the index will be stored.
     * @param encoder The {@link Encoder} used to encode the reads.
     * @param globalSettings The {@link GlobalSettings} object containing the settings for the indexing process.
     */
    public ReadIndexer(SequenceSupplier<Integer, Character, Byte> sup,
                       HeaderToIdReader fastqIdReader,
                       Path indexDir,
                       Encoder encoder,
                       GlobalSettings globalSettings) {
        logger = new Logger("ReadIndexer");
        logger.addElement(new Time());
        this.sup = sup;
        this.fastqIdReader = fastqIdReader;
        this.readIndexIO = new ReadIndexIO(indexDir, encoder.getNumberOfBuckets());
        this.encoder = encoder;
        this.globalSettings = globalSettings;
        this.nrOfProcessedTranslations = new AtomicInteger(0);
        this.nrOfSkippedTranslations = new AtomicInteger(0);
    }

    /**
     * Indexes a file of sequencing reads.
     * @throws IOException If an error occurs during reading the file or writing the buckets.
     * <p>
     *     The method iterates over the {@link SequenceSupplier} until all buckets have been collected and stored.
     *     In each iteration, the kmers for a specific range of buckets are collected.
     *     The collection process is carried out in parallel on batches of reads.
     * </p>
     */
    public String index() throws IOException {
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

            final int nrOfBuckets = encoder.getNumberOfBuckets();
            for (int i = 0; i < nrOfBuckets; i += globalSettings.BUCKETS_PER_CYCLE) {
                nrOfProcessedReads = 0;
                nrOfSkippedTranslations.set(0);
                nrOfProcessedTranslations.set(0);
                progressBar.setProgress(0);
                int bucketIndexRangeStart = i;
                int bucketIndexRangeEnd = Math.min(i + globalSettings.BUCKETS_PER_CYCLE, nrOfBuckets);
                logger.logInfo("Indexing buckets " + bucketIndexRangeStart + " - " + bucketIndexRangeEnd);

                // Initialize a list for each bucket that is processed in this iteration
                final ConcurrentLinkedQueue<Long>[] bucketLists = new ConcurrentLinkedQueue[globalSettings.BUCKETS_PER_CYCLE];
                for (int j = 0; j < globalSettings.BUCKETS_PER_CYCLE; j++) {
                    bucketLists[j] = new ConcurrentLinkedQueue<>();
                }

                // initialize batch of FutureSequenceRecords
                // each batch is processed in a separate thread
                FutureSequenceRecords<Integer, Byte>[] batch = new FutureSequenceRecords[globalSettings.SEQUENCE_BATCH_SIZE];
                int batchIndex = 0;

                FutureSequenceRecords<Integer, Byte> futureSequenceRecords;
                while ((futureSequenceRecords = sup.next()) != null) {
                    nrOfProcessedReads++;
                    batch[batchIndex] = futureSequenceRecords;
                    progressBar.setProgress(sup.getBytesRead());
                    progressLogger.incrementProgress();

                    if (batchIndex < globalSettings.SEQUENCE_BATCH_SIZE - 1) {
                        batchIndex++;
                    } else {
                        // submit full batch of FutureSequenceRecords to thread pool
                        indexPhaser.register();
                        threadPoolExecutor.submit(
                                new ReadProcessor(
                                        indexPhaser, batch, encoder, bucketLists,
                                        bucketIndexRangeStart, bucketIndexRangeEnd,
                                        nrOfProcessedTranslations, nrOfSkippedTranslations));
                        batch = new FutureSequenceRecords[globalSettings.SEQUENCE_BATCH_SIZE];
                        batchIndex = 0;
                    }
                }
                // submit last batch
                indexPhaser.register();
                threadPoolExecutor.submit(
                        new ReadProcessor(
                                indexPhaser, Arrays.copyOfRange(batch, 0, batchIndex), encoder, bucketLists,
                                bucketIndexRangeStart, bucketIndexRangeEnd,
                                nrOfProcessedTranslations, nrOfSkippedTranslations));
                progressBar.finish();
                indexPhaser.arriveAndAwaitAdvance();

                // write a read header map during the first iteration
                if (bucketIndexRangeStart == 0) {
                    logger.logInfo("Writing read header map");
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
                // remove headers from memory
                fastqIdReader.removeHeaders();

                logger.logInfo("Converting, sorting and writing buckets " + bucketIndexRangeStart + " - " + bucketIndexRangeEnd);
                for (int j = 0; j < Math.min(globalSettings.BUCKETS_PER_CYCLE, (nrOfBuckets - bucketIndexRangeStart)); j++) {
                    int finalJ = j;
                    indexPhaser.register();
                    threadPoolExecutor.submit(() -> {
                        try {
                            readIndexIO.getBucketIO(bucketIndexRangeStart + finalJ).write(new Bucket(bucketIndexRangeStart + finalJ, bucketLists[finalJ]));
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
        String info = "Processed Reads: " + nrOfProcessedReads +
                "\nProcessed translations: " + nrOfProcessedTranslations.get() +
                "\nSkipped translations because length < " + encoder.getK() + ": " + nrOfSkippedTranslations.get();
        logger.logInfo(info.replaceAll("\n", "; "));
        return info;
    }

    /**
     * Class that extracts the kmers from a batch of reads and adds them to the corresponding bucket.
     */
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

        /**
         * @param phaser phaser to deregister after processing
         * @param batch batch of reads to process
         * @param encoder encoder to use for kmer extraction
         * @param bucketLists list of buckets to add kmers to
         * @param rangeStart start of the range of buckets to process
         * @param rangeEnd end of the range of buckets to process
         * @param nrOfProcessedTranslations number of processed read translations
         * @param nrOfSkippedTranslations number of skipped read translations
         */
        public ReadProcessor(
                Phaser phaser,
                FutureSequenceRecords<Integer, Byte>[] batch,
                Encoder encoder,
                ConcurrentLinkedQueue<Long>[] bucketLists,
                int rangeStart,
                int rangeEnd,
                AtomicInteger nrOfProcessedTranslations,
                AtomicInteger nrOfSkippedTranslations) {
            this.phaser = phaser;
            this.batch = batch;
            this.encoder = encoder;
            this.kmerExtractor = encoder.getKmerExtractor();
            this.bucketLists = bucketLists;
            this.rangeStart = rangeStart;
            this.rangeEnd = rangeEnd;
            this.processedTranslations = nrOfProcessedTranslations;
            this.skippedTranslations = nrOfSkippedTranslations;
        }

        @Override
        public void run() {
            try {
                for (FutureSequenceRecords<Integer, Byte> futureSequenceRecords : batch) {
                    for (SequenceRecord<Integer, Byte> record: futureSequenceRecords.getSequenceRecords()) {
                        // skip empty or too short sequences
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

