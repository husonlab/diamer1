package org.husonlab.diamer2.indexing;

import org.husonlab.diamer2.io.indexing.DBIndexIO;
import org.husonlab.diamer2.io.seq.FastaIdReader;
import org.husonlab.diamer2.io.seq.SequenceRecordContainer;
import org.husonlab.diamer2.io.seq.SequenceSupplier;
import org.husonlab.diamer2.io.taxonomy.TreeIO;
import org.husonlab.diamer2.main.encoders.Encoder;
import org.husonlab.diamer2.taxonomy.Tree;
import org.husonlab.diamer2.util.Pair;
import org.husonlab.diamer2.util.logging.*;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.*;

public class DBIndexer {

    private final Logger logger;
    private final Path fastaFile;
    private final Path indexDir;
    private final DBIndexIO IndexIO;
    private final Tree tree;
    private final Encoder encoder;
    private final int MAX_THREADS;
    private final int MAX_QUEUE_SIZE;
    private final int BATCH_SIZE;
    private final int bucketsPerCycle;
    private final boolean KEEP_IN_MEMORY;
    private final boolean debug;
    private final ArrayList<Pair<Integer, Integer>> bucketSizes;
    private final StringBuilder report;

    public DBIndexer(Path fastaFile,
                     Path indexDir,
                     Tree tree,
                     Encoder encoder,
                     int MAX_THREADS,
                     int MAX_QUEUE_SIZE,
                     int BATCH_SIZE,
                     int bucketsPerCycle,
                     boolean KEEP_IN_MEMORY,
                     boolean debug) {
        this.logger = new Logger("DBIndexer");
        logger.addElement(new Time());
        this.fastaFile = fastaFile;
        this.encoder = encoder;
        this.indexDir = indexDir;
        this.IndexIO = new DBIndexIO(indexDir);
        this.tree = tree;
        this.MAX_THREADS = MAX_THREADS;
        this.MAX_QUEUE_SIZE = MAX_QUEUE_SIZE;
        this.BATCH_SIZE = BATCH_SIZE;
        this.bucketsPerCycle = bucketsPerCycle;
        this.KEEP_IN_MEMORY = KEEP_IN_MEMORY;
        this.debug = debug;
        this.bucketSizes = new ArrayList<>();
        report = new StringBuilder();
    }

    /**
     * IndexIO a Sequence database and write the indexed buckets to files.
     * @throws IOException If an error occurs during reading the database or writing the buckets.
     */
    public DBIndexIO index() throws IOException {
        logger.logInfo("Indexing " + fastaFile + " to " + indexDir);
        tree.addNodeLongProperty("kmers in database", 0);

        ThreadPoolExecutor threadPoolExecutor = new CustomThreadPoolExecutor(
                MAX_THREADS,
                MAX_THREADS,
                500L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(MAX_QUEUE_SIZE),
                new ThreadPoolExecutor.CallerRunsPolicy());

        Phaser indexPhaser = new Phaser(1);

        int processedFastas = 0;

        try (SequenceSupplier<Integer, Byte> sup = new SequenceSupplier<>(new FastaIdReader(fastaFile), encoder.getAAConverter(), KEEP_IN_MEMORY)) {

            ProgressBar progressBar = new ProgressBar(sup.getFileSize(), 20);
            ProgressLogger progressLogger = new ProgressLogger("sequences");
            new OneLineLogger("DBIndexer", 1000)
                    .addElement(new RunningTime())
                    .addElement(progressBar)
                    .addElement(progressLogger);

            // Initialize Concurrent HashMaps to store the kmers and their taxIds during indexing
            final ConcurrentHashMap<Long, Integer>[] bucketMaps = new ConcurrentHashMap[bucketsPerCycle];

            final int maxBuckets = (int) Math.pow(2, encoder.getBitsOfBucketNames());
            for (int i = 0; i < maxBuckets; i += bucketsPerCycle) {

                int rangeStart = i;
                int rangeEnd = Math.min(i + bucketsPerCycle, maxBuckets);

                logger.logInfo("Indexing buckets " + rangeStart + " - " + rangeEnd);

                if (!debug) {
                    for (int j = 0; j < bucketsPerCycle; j++) {
                        bucketMaps[j] = new ConcurrentHashMap<Long, Integer>(57000000); // initial capacity 57000000
                    }
                } else {
                    for (int j = 0; j < bucketsPerCycle; j++) {
                        bucketMaps[j] = new ConcurrentHashMap<Long, Integer>();
                    }
                }

                progressBar.setProgress(0);
                progressLogger.setProgress(0);
                SequenceRecordContainer<Integer, Byte>[] batch = new SequenceRecordContainer[BATCH_SIZE];
                processedFastas = 0;
                SequenceRecordContainer<Integer, Byte> container;
                while ((container = sup.next()) != null) {
                    batch[processedFastas % BATCH_SIZE] = container;
                    progressBar.setProgress(sup.getBytesRead());
                    progressLogger.setProgress(processedFastas);
                    if (++processedFastas % BATCH_SIZE == 0) {
                        indexPhaser.register();
                        threadPoolExecutor.submit(new FastaProteinProcessor(indexPhaser, batch, encoder, bucketMaps, tree, rangeStart, rangeEnd));
                        batch = new SequenceRecordContainer[BATCH_SIZE];
                    }
                }
                indexPhaser.register();
                threadPoolExecutor.submit(new FastaProteinProcessor(indexPhaser, Arrays.copyOfRange(batch, 0, processedFastas % BATCH_SIZE), encoder, bucketMaps, tree, rangeStart, rangeEnd));
                progressBar.finish();

                indexPhaser.arriveAndAwaitAdvance();
                logger.logInfo("Processed " + processedFastas + " sequences");
                logger.logInfo("Converting, sorting and writing buckets " + rangeStart + " - " + rangeEnd);

                for (int j = 0; j < Math.min(bucketsPerCycle, (maxBuckets - rangeStart)); j++) {
                    bucketSizes.add(new Pair<>(rangeStart + j, bucketMaps[j].size()));
                    int finalJ = j;
                    indexPhaser.register();
                    threadPoolExecutor.submit(() -> {
                        try {
                            IndexIO.getBucketIO(rangeStart + finalJ).write(new Bucket(rangeStart + finalJ, bucketMaps[finalJ], encoder));
                        } catch (RuntimeException | IOException e) {
                            throw new RuntimeException("Error converting and writing bucket " + (rangeStart + finalJ), e);
                        } finally {
                            indexPhaser.arriveAndDeregister();
                        }
                    });

                    indexPhaser.register();
                    threadPoolExecutor.submit(() -> {
                        try {
                            for (int taxId: bucketMaps[finalJ].values()) {
                                tree.addToNodeProperty(taxId, "kmers in database", 1);
                            }
                        } finally {
                            indexPhaser.arriveAndDeregister();
                        }
                    });
                }
                indexPhaser.arriveAndAwaitAdvance();
                sup.reset();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        threadPoolExecutor.shutdown();
        try {
            if (threadPoolExecutor.awaitTermination(1, TimeUnit.MINUTES)) {
                logger.logInfo("Indexing complete.");
            } else {
                logger.logError("Indexing timed out.");
                threadPoolExecutor.shutdownNow();
                throw new RuntimeException("Indexing timed out.");
            }
        } catch (InterruptedException e) {
            logger.logError("Indexing interrupted.");
            threadPoolExecutor.shutdownNow();
            throw new RuntimeException("Indexing interrupted.");
        }

        // Export tree with number of kmers that map to each node
        TreeIO.saveTree(tree, indexDir.resolve("tree.txt"));

        report
                .append(java.time.LocalDateTime.now()).append("\n")
                .append("input file: ").append(fastaFile).append("\n")
                .append("output directory: ").append(indexDir).append("\n")
                .append("processed sequenceRecords: ").append(processedFastas).append("\n")
                .append("bucket sizes: ").append("\n");

        for (Pair<Integer, Integer> bucketSize : bucketSizes) {
            report.append(bucketSize.first()).append("\t").append(bucketSize.last()).append("\n");
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(indexDir.resolve("report.txt").toFile()))) {
            writer.write(report.toString());
        }

        return IndexIO;
    }
}
