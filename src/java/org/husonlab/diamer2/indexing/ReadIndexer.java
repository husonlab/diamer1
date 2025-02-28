package org.husonlab.diamer2.indexing;

import org.husonlab.diamer2.indexing.kmers.KmerExtractor;
import org.husonlab.diamer2.io.indexing.ReadIndexIO;
import org.husonlab.diamer2.io.seq.FutureSequenceRecords;
import org.husonlab.diamer2.io.seq.HeaderToIdReader;
import org.husonlab.diamer2.io.seq.SequenceSupplier;
import org.husonlab.diamer2.main.GlobalSettings;
import org.husonlab.diamer2.main.encoders.Encoder;
import org.husonlab.diamer2.seq.Sequence;
import org.husonlab.diamer2.seq.SequenceRecord;
import org.husonlab.diamer2.util.Pair;
import org.husonlab.diamer2.util.logging.*;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.husonlab.diamer2.indexing.Utilities.writeKmerStatistics;

/**
 * Class to create an index for sequencing reads.
 */
public class ReadIndexer {

    private final Logger logger;
    private final Phaser phaser;
    private int nrOfProcessedFurutreSequenceRecords;
    private final AtomicInteger nrOfProcessedTranslations;
    private final AtomicInteger nrOfSkippedTranslations;
    private final ArrayList<Pair<Integer, Integer>> bucketSizes;
    private final ConcurrentHashMap<Long, Integer> kmerCounts;
    private final StringBuilder report;

    private final SequenceSupplier<Integer, Character, Byte> sup;
    private final HeaderToIdReader fastqIdReader;
    private final ReadIndexIO readIndexIO;
    private final Encoder encoder;
    private final GlobalSettings settings;

    /**
     * @param sup SequenceSupplier
     * @param fastqIdReader The {@link HeaderToIdReader} used for the SequenceSupplier to be able to access the read headers.
     * @param indexDir The directory where the index will be stored.
     * @param encoder The {@link Encoder} used to encode the reads.
     * @param settings The {@link GlobalSettings} object containing the settings for the indexing process.
     */
    public ReadIndexer(SequenceSupplier<Integer, Character, Byte> sup,
                       HeaderToIdReader fastqIdReader,
                       Path indexDir,
                       Encoder encoder,
                       GlobalSettings settings) {
        logger = new Logger("ReadIndexer");
        logger.addElement(new Time());
        phaser = new Phaser(1);
        this.nrOfProcessedTranslations = new AtomicInteger(0);
        this.nrOfSkippedTranslations = new AtomicInteger(0);
        this.bucketSizes = new ArrayList<>();
        this.kmerCounts = new ConcurrentHashMap<>();
        report = new StringBuilder();

        this.sup = sup;
        this.fastqIdReader = fastqIdReader;
        this.readIndexIO = new ReadIndexIO(indexDir, encoder.getNumberOfBuckets());
        this.encoder = encoder;
        this.settings = settings;
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
                        settings.MAX_THREADS, settings.MAX_THREADS, settings.QUEUE_SIZE, 1, logger)) {

            ProgressBar progressBar = new ProgressBar(sup.getFileSize(), 20);
            ProgressLogger progressLogger = new ProgressLogger("reads");
            new OneLineLogger("ReadIndexer", 1000)
                    .addElement(new RunningTime())
                    .addElement(progressBar)
                    .addElement(progressLogger);

            final int nrOfBuckets = encoder.getNumberOfBuckets();
            for (int i = 0; i < nrOfBuckets; i += settings.BUCKETS_PER_CYCLE) {
                int bucketIndexRangeStart = i;
                int bucketIndexRangeEnd = Math.min(i + settings.BUCKETS_PER_CYCLE, nrOfBuckets);
                nrOfProcessedFurutreSequenceRecords = 0;
                nrOfSkippedTranslations.set(0);
                nrOfProcessedTranslations.set(0);
                logger.logInfo("Indexing buckets " + bucketIndexRangeStart + " - " + bucketIndexRangeEnd);
                progressBar.setProgressSilent(0);
                progressLogger.setProgressSilent(0);

                // Initialize a list for each bucket that is processed in this iteration
                final ConcurrentLinkedQueue<Long>[] bucketLists = new ConcurrentLinkedQueue[settings.BUCKETS_PER_CYCLE];
                for (int j = 0; j < settings.BUCKETS_PER_CYCLE; j++) {
                    bucketLists[j] = new ConcurrentLinkedQueue<>();
                }

                // initialize batch of FutureSequenceRecords
                // each batch is processed in a separate thread
                FutureSequenceRecords<Integer, Byte>[] batch = new FutureSequenceRecords[settings.SEQUENCE_BATCH_SIZE];

                FutureSequenceRecords<Integer, Byte> futureSequenceRecords;
                while ((futureSequenceRecords = sup.next()) != null) {
                    batch[nrOfProcessedFurutreSequenceRecords % settings.SEQUENCE_BATCH_SIZE] = futureSequenceRecords;
                    progressBar.setProgress(sup.getBytesRead());
                    progressLogger.incrementProgress();

                    if(++nrOfProcessedFurutreSequenceRecords % settings.SEQUENCE_BATCH_SIZE == 0) {
                        // submit full batch of FutureSequenceRecords to thread pool
                        phaser.register();
                        threadPoolExecutor.submit(
                                new ReadProcessor(
                                        batch, bucketLists,
                                        bucketIndexRangeStart, bucketIndexRangeEnd
                                ));
                        batch = new FutureSequenceRecords[settings.SEQUENCE_BATCH_SIZE];
                    }
                }
                // submit last batch
                phaser.register();
                threadPoolExecutor.submit(
                        new ReadProcessor(
                                Arrays.copyOfRange(batch, 0, nrOfProcessedFurutreSequenceRecords % settings.SEQUENCE_BATCH_SIZE), bucketLists,
                                bucketIndexRangeStart, bucketIndexRangeEnd
                        ));
                progressBar.finish();
                phaser.arriveAndAwaitAdvance();

                // write a read header map during the first iteration
                if (bucketIndexRangeStart == 0) {
                    logger.logInfo("Writing read header map");
                    phaser.register();
                    threadPoolExecutor.submit(() -> {
                        try {
                            readIndexIO.writeReadHeaderMapping(fastqIdReader.getHeaders());
                        } catch (RuntimeException e) {
                            throw new RuntimeException("Error writing read header map.", e);
                        } finally {
                            phaser.arriveAndDeregister();
                        }
                    });
                }
                phaser.arriveAndAwaitAdvance();
                // remove headers from memory
                fastqIdReader.removeHeaders();

                logger.logInfo("Converting, sorting and writing buckets " + bucketIndexRangeStart + " - " + bucketIndexRangeEnd);
                for (int j = 0; j < Math.min(settings.BUCKETS_PER_CYCLE, (nrOfBuckets - bucketIndexRangeStart)); j++) {
                    bucketSizes.add(new Pair<>(bucketIndexRangeStart + j, bucketLists[j].size()));
                    int finalJ = j;
                    phaser.register();
                    threadPoolExecutor.submit(() -> {
                        try {
                            readIndexIO.getBucketIO(bucketIndexRangeStart + finalJ).write(new Bucket(bucketIndexRangeStart + finalJ, bucketLists[finalJ], encoder));
                        } catch (RuntimeException | IOException e) {
                            throw new RuntimeException("Error converting and writing bucket " + finalJ, e);
                        } finally {
                            phaser.arriveAndDeregister();
                        }
                    });
                }
                phaser.arriveAndAwaitAdvance();
                sup.reset();
            }
        }
        report.append("Processed Reads: ").append(nrOfProcessedFurutreSequenceRecords).append("\n")
                .append("Processed translations: ").append(nrOfProcessedTranslations.get()).append("\n")
                .append("Skipped translations because length < ").append(encoder.getK()).append(": ").append(nrOfSkippedTranslations.get()).append("\n");
        logger.logInfo(report.toString().replaceAll("\n", "; "));
        long totalKmers = 0;
        StringBuilder bucketSizesString = new StringBuilder().append("bucket sizes: ").append("\n");
        for (Pair<Integer, Integer> bucketSize : bucketSizes) {
            totalKmers += bucketSize.last();
            bucketSizesString.append(bucketSize.first()).append("\t").append(bucketSize.last()).append("\n");
        }
        report.append("total extracted kmers: ").append(totalKmers).append("\n");
        report.append(bucketSizesString);

        if (settings.COLLECT_STATS) writeKmerStatistics(kmerCounts, readIndexIO.getIndexFolder());
        return report.toString();
    }

    /**
     * Class that extracts the kmers from a batch of reads and adds them to the corresponding bucket.
     */
    private class ReadProcessor implements Runnable {

        private final FutureSequenceRecords<Integer, Byte>[] batch;
        private final KmerExtractor kmerExtractor;
        private final ConcurrentLinkedQueue<Long>[] bucketLists;
        private final int rangeStart;
        private final int rangeEnd;

        /**
         * @param batch       batch of reads to process
         * @param bucketLists list of buckets to add kmers to
         * @param rangeStart  start of the range of buckets to process
         * @param rangeEnd    end of the range of buckets to process
         */
        public ReadProcessor(
                FutureSequenceRecords<Integer, Byte>[] batch,
                ConcurrentLinkedQueue<Long>[] bucketLists,
                int rangeStart,
                int rangeEnd) {
            this.batch = batch;
            this.kmerExtractor = encoder.getKmerExtractor();
            this.bucketLists = bucketLists;
            this.rangeStart = rangeStart;
            this.rangeEnd = rangeEnd;
        }

        @Override
        public void run() {
            try {
                for (FutureSequenceRecords<Integer, Byte> futureSequenceRecords : batch) {
                    for (SequenceRecord<Integer, Byte> record: futureSequenceRecords.getSequenceRecords()) {
                        // skip empty or too short sequences
                        if (record == null || record.sequence().length() < encoder.getK()) {
                            nrOfSkippedTranslations.incrementAndGet();
                        } else {
                            nrOfProcessedTranslations.incrementAndGet();
                            int id = record.id();
                            Sequence<Byte> sequence = record.sequence();
                            long[] kmers = kmerExtractor.extractKmers(sequence);
                            for (long kmer : kmers) {
                                int bucketName = encoder.getBucketNameFromKmer(kmer);
                                if (bucketName >= rangeStart && bucketName < rangeEnd) {
                                    if (settings.COLLECT_STATS) kmerCounts.put(kmer, kmerCounts.getOrDefault(kmer, 0) + 1);
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

