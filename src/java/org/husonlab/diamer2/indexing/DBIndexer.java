package org.husonlab.diamer2.indexing;

import org.husonlab.diamer2.indexing.kmers.KmerExtractor;
import org.husonlab.diamer2.io.indexing.DBIndexIO;
import org.husonlab.diamer2.io.seq.FutureSequenceRecords;
import org.husonlab.diamer2.io.seq.SequenceSupplier;
import org.husonlab.diamer2.io.taxonomy.TreeIO;
import org.husonlab.diamer2.main.GlobalSettings;
import org.husonlab.diamer2.main.encoders.Encoder;
import org.husonlab.diamer2.seq.Sequence;
import org.husonlab.diamer2.seq.SequenceRecord;
import org.husonlab.diamer2.taxonomy.Tree;
import org.husonlab.diamer2.util.Pair;
import org.husonlab.diamer2.util.logging.*;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.husonlab.diamer2.indexing.Utilities.writeKmerStatistics;

public class DBIndexer {

    private final Logger logger;
    private final Phaser phaser;
    private int nrOfProcessedFutureSequenceRecords;
    private final AtomicInteger nrOfProcessedSequenceRecords;
    private final AtomicInteger nrOfSkippedSequenceRecords;
    private final ArrayList<Pair<Integer, Integer>> bucketSizes;
    private final ConcurrentHashMap<Long, Integer> kmerCounts;
    private final StringBuilder report;

    private final SequenceSupplier<Integer, Character, Byte> sup;
    private final Path indexDir;
    private final DBIndexIO dbIndexIO;
    private final Tree tree;
    private final Encoder encoder;
    private final GlobalSettings settings;

    public DBIndexer(SequenceSupplier<Integer, Character, Byte> sup,
                     Path indexDir,
                     Tree tree,
                     Encoder encoder,
                     GlobalSettings settings) {
        this.logger = new Logger("DBIndexer");
        logger.addElement(new Time());
        phaser = new Phaser(1);
        nrOfProcessedFutureSequenceRecords = 0;
        nrOfProcessedSequenceRecords = new AtomicInteger(0);
        nrOfSkippedSequenceRecords = new AtomicInteger(0);
        this.bucketSizes = new ArrayList<>();
        this.kmerCounts = new ConcurrentHashMap<>();
        report = new StringBuilder();

        this.sup = sup;
        this.encoder = encoder;
        this.settings = settings;
        this.indexDir = indexDir;
        this.dbIndexIO = new DBIndexIO(indexDir, encoder.getNumberOfBuckets());
        this.tree = tree;
    }

    /**
     * IndexIO a Sequence database and write the indexed buckets to files.
     * @throws IOException If an error occurs during reading the database or writing the buckets.
     */
    public String index() throws IOException {
        logger.logInfo("Indexing " + sup.getFile() + " to " + indexDir);
        tree.addNodeLongProperty("kmers in database", 0);

        try (ThreadPoolExecutor threadPoolExecutor = new CustomThreadPoolExecutor(settings.MAX_THREADS,
                settings.MAX_THREADS, settings.QUEUE_SIZE, 1, logger)) {

            ProgressBar progressBar = new ProgressBar(sup.getFileSize(), 20);
            ProgressLogger progressLogger = new ProgressLogger("sequences");
            new OneLineLogger("DBIndexer", 1000)
                    .addElement(new RunningTime())
                    .addElement(progressBar)
                    .addElement(progressLogger);

            // Initialize Concurrent HashMaps to store the kmers and their taxIds during indexing
            final ConcurrentHashMap<Long, Integer>[] bucketMaps = new ConcurrentHashMap[settings.BUCKETS_PER_CYCLE];

            final int maxBuckets = encoder.getNumberOfBuckets();
            for (int i = 0; i < maxBuckets; i += settings.BUCKETS_PER_CYCLE) {
                int bucketIndexRangeStart = i;
                int bucketIndexRangeEnd = Math.min(i + settings.BUCKETS_PER_CYCLE, maxBuckets);
                nrOfProcessedFutureSequenceRecords = 0;
                nrOfProcessedSequenceRecords.set(0);
                nrOfSkippedSequenceRecords.set(0);
                logger.logInfo("Indexing buckets " + bucketIndexRangeStart + " - " + bucketIndexRangeEnd);
                progressBar.setProgressSilent(0);
                progressLogger.setProgressSilent(0);

                if (!settings.DEBUG) {
                    for (int j = 0; j < settings.BUCKETS_PER_CYCLE; j++) {
                        bucketMaps[j] = new ConcurrentHashMap<Long, Integer>(57000000); // initial capacity 57000000
                    }
                } else {
                    for (int j = 0; j < settings.BUCKETS_PER_CYCLE; j++) {
                        bucketMaps[j] = new ConcurrentHashMap<Long, Integer>();
                    }
                }
                FutureSequenceRecords<Integer, Byte>[] batch = new FutureSequenceRecords[settings.SEQUENCE_BATCH_SIZE];
                FutureSequenceRecords<Integer, Byte> futureSequenceRecords;
                while ((futureSequenceRecords = sup.next()) != null) {
                    batch[nrOfProcessedFutureSequenceRecords % settings.SEQUENCE_BATCH_SIZE] = futureSequenceRecords;
                    progressBar.setProgress(sup.getBytesRead());
                    progressLogger.setProgress(nrOfProcessedFutureSequenceRecords);
                    // submit full batch
                    if (++nrOfProcessedFutureSequenceRecords % settings.SEQUENCE_BATCH_SIZE == 0) {
                        phaser.register();
                        threadPoolExecutor.submit(new FastaProteinProcessor(batch, bucketMaps,
                                bucketIndexRangeStart, bucketIndexRangeEnd));
                        batch = new FutureSequenceRecords[settings.SEQUENCE_BATCH_SIZE];
                    }
                }
                phaser.register();
                // submit last batch
                threadPoolExecutor.submit(new FastaProteinProcessor(
                        Arrays.copyOfRange(batch, 0, nrOfProcessedFutureSequenceRecords % settings.SEQUENCE_BATCH_SIZE),
                        bucketMaps, bucketIndexRangeStart, bucketIndexRangeEnd));
                progressBar.finish();

                phaser.arriveAndAwaitAdvance();
                logger.logInfo("Converting, sorting and writing buckets " + bucketIndexRangeStart + " - " + bucketIndexRangeEnd);

                for (int j = 0; j < Math.min(settings.BUCKETS_PER_CYCLE, (maxBuckets - bucketIndexRangeStart)); j++) {
                    bucketSizes.add(new Pair<>(bucketIndexRangeStart + j, bucketMaps[j].size()));
                    int finalJ = j;
                    phaser.register();
                    threadPoolExecutor.submit(() -> {
                        try {
                            dbIndexIO.getBucketIO(bucketIndexRangeStart + finalJ).write(new Bucket(bucketIndexRangeStart + finalJ, bucketMaps[finalJ], encoder));
                        } catch (RuntimeException | IOException e) {
                            throw new RuntimeException("Error converting and writing bucket " + (bucketIndexRangeStart + finalJ), e);
                        } finally {
                            phaser.arriveAndDeregister();
                        }
                    });

                    phaser.register();
                    threadPoolExecutor.submit(() -> {
                        try {
                            for (int taxId: bucketMaps[finalJ].values()) {
                                tree.addToNodeProperty(taxId, "kmers in database", 1);
                            }
                        } finally {
                            phaser.arriveAndDeregister();
                        }
                    });
                }
                phaser.arriveAndAwaitAdvance();
                sup.reset();
            }
        }

        // Export tree with number of kmers that map to each node
        TreeIO.saveTree(tree, indexDir.resolve("tree.txt"));

        report
                .append("input file: ").append(sup.getFile()).append("\n")
                .append("output directory: ").append(indexDir).append("\n")
                .append("processed sequenceRecords: ").append(nrOfProcessedFutureSequenceRecords).append("\n");
        long totalKmers = 0;
        StringBuilder bucketSizesString = new StringBuilder().append("bucket sizes: ").append("\n");
        for (Pair<Integer, Integer> bucketSize : bucketSizes) {
            totalKmers += bucketSize.last();
            bucketSizesString.append(bucketSize.first()).append("\t").append(bucketSize.last()).append("\n");
        }
        report.append("total extracted kmers: ").append(totalKmers).append("\n");
        report.append(bucketSizesString);

        if (settings.COLLECT_STATS) writeKmerStatistics(kmerCounts, dbIndexIO.getIndexFolder());
        return report.toString();
    }

    /**
     * Runnable to compare a read- and a database bucket.
     */
    private class FastaProteinProcessor implements Runnable {
        private final FutureSequenceRecords<Integer, Byte>[] containers;
        private final KmerExtractor kmerExtractor;
        private final ConcurrentHashMap<Long, Integer>[] bucketMaps;
        private final int rangeStart;
        private final int rangeEnd;

        /**
         * Processes a batch of Sequence sequences and adds the kmers to the corresponding bucket maps.
         * @param containers Array of Sequence sequences to process.
         * @param bucketMaps Array of ConcurrentHashMaps to store the kmers.
         */
        public FastaProteinProcessor(FutureSequenceRecords<Integer, Byte>[] containers, ConcurrentHashMap<Long, Integer>[] bucketMaps, int rangeStart, int rangeEnd) {
            this.containers = containers;
            this.kmerExtractor = encoder.getKmerExtractor();
            this.bucketMaps = bucketMaps;
            this.rangeStart = rangeStart;
            this.rangeEnd = rangeEnd;
        }

        @Override
        public void run() {
            try {
                for (FutureSequenceRecords<Integer, Byte> container : containers) {
                    for (SequenceRecord<Integer, Byte> record: container.getSequenceRecords()) {
                        if (record == null || record.sequence().length() < kmerExtractor.getK() || !tree.hasNode(record.id())) {
                            continue;
                        }
                        Sequence<Byte> sequence = record.sequence();
                        int taxId = record.id();
                        long[] kmers = kmerExtractor.extractKmers(sequence);
                        for (long kmerEnc : kmers) {
                            int bucketName = encoder.getBucketNameFromKmer(kmerEnc);
                            if (bucketName >= rangeStart && bucketName < rangeEnd) {
                                if (settings.COLLECT_STATS) kmerCounts.put(kmerEnc, kmerCounts.getOrDefault(kmerEnc, 0) + 1);
                                bucketMaps[bucketName - rangeStart].computeIfPresent(kmerEnc, (k, v) -> tree.findLCA(v, taxId));
                                bucketMaps[bucketName - rangeStart].computeIfAbsent(kmerEnc, k -> taxId);
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
