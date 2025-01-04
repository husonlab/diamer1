package org.husonlab.diamer2.indexing;

import org.husonlab.diamer2.io.DBIndexIO;
import org.husonlab.diamer2.io.SequenceSupplier;
import org.husonlab.diamer2.logging.*;
import org.husonlab.diamer2.seq.Sequence;
import org.husonlab.diamer2.seq.alphabet.ReducedProteinAlphabet;
import org.husonlab.diamer2.taxonomy.Tree;

import java.io.*;
import java.nio.file.Path;
import java.util.concurrent.*;

public class DBIndexer {

    private final Logger logger;
    private final File fastaFile;
    private final Path indexDir;
    private final DBIndexIO DBIndexIO;
    private final Tree tree;
    private final long mask;
    private final ReducedProteinAlphabet alphabet;
    private final int MAX_THREADS;
    private final int MAX_QUEUE_SIZE;
    private final int BATCH_SIZE;
    private final int bucketsPerCycle;
    private final boolean debug;

    public DBIndexer(File fastaFile,
                     Path indexDir,
                     Tree tree,
                     long mask,
                     ReducedProteinAlphabet alphabet,
                     int MAX_THREADS,
                     int MAX_QUEUE_SIZE,
                     int BATCH_SIZE,
                     int bucketsPerCycle,
                     boolean debug) {
        this.logger = new Logger("DBIndexer");
        this.fastaFile = fastaFile;
        this.mask = mask;
        this.alphabet = alphabet;
        this.indexDir = indexDir;
        this.DBIndexIO = new DBIndexIO(indexDir);
        this.tree = tree;
        this.MAX_THREADS = MAX_THREADS;
        this.MAX_QUEUE_SIZE = MAX_QUEUE_SIZE;
        this.BATCH_SIZE = BATCH_SIZE;
        this.bucketsPerCycle = bucketsPerCycle;
        this.debug = debug;
    }

    /**
     * DBIndexIO a Sequence database and write the indexed buckets to files.
     * @throws IOException If an error occurs during reading the database or writing the buckets.
     */
    public DBIndexIO index() throws IOException {
        logger.logInfo("Indexing " + fastaFile + " to " + indexDir);

        ThreadPoolExecutor threadPoolExecutor = new CustomThreadPoolExecutor(
                MAX_THREADS,
                MAX_THREADS,
                500L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(MAX_QUEUE_SIZE),
                new ThreadPoolExecutor.CallerRunsPolicy());

        Phaser indexPhaser = new Phaser(1);


        try (SequenceSupplier sup = SequenceSupplier.getFastaSupplier(fastaFile, true)) {

            ProgressBar progressBar = new ProgressBar(sup.getFileSize(), 20);
            Message progressMessage = new Message("");
            new OneLineLogger("DBIndexer", 1000)
                    .addElement(progressBar)
                    .addElement(progressMessage);

            // Initialize Concurrent HashMaps to store the kmers and their taxIds during indexing
            final ConcurrentHashMap<Long, Integer>[] bucketMaps = new ConcurrentHashMap[bucketsPerCycle];

            for (int i = 0; i < 1024; i += bucketsPerCycle) {

                int rangeStart = i;
                int rangeEnd = Math.min(i + bucketsPerCycle, 1024);

                progressMessage.setMessage(" Indexing buckets " + rangeStart + " - " + rangeEnd);

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
                Sequence[] batch = new Sequence[BATCH_SIZE];
                int processedFastas = 0;
                Sequence seq;
                while ((seq = sup.next()) != null) {
                    batch[processedFastas % BATCH_SIZE] = seq;
                    progressBar.setProgress(sup.getBytesRead());

                    if (++processedFastas % BATCH_SIZE == 0) {
                        indexPhaser.register();
                        threadPoolExecutor.submit(new FastaProteinBatchProcessor(indexPhaser, batch, mask, alphabet, bucketMaps, tree, rangeStart, rangeEnd));
                        batch = new Sequence[BATCH_SIZE];
                    }
                }
                indexPhaser.register();
                threadPoolExecutor.submit(new FastaProteinBatchProcessor(indexPhaser, batch, mask, alphabet, bucketMaps, tree, rangeStart, rangeEnd));
                progressBar.finish();

                indexPhaser.arriveAndAwaitAdvance();
                logger.logInfo("Converting, sorting and writing buckets " + rangeStart + " - " + rangeEnd);

                for (int j = 0; j < bucketsPerCycle; j++) {
                    int finalJ = j;
                    indexPhaser.register();
                    threadPoolExecutor.submit(() -> {
                        try {
                            DBIndexIO.getBucketIO(rangeStart + finalJ).write(new Bucket(rangeStart + finalJ, bucketMaps[finalJ]));
                        } catch (RuntimeException | IOException e) {
                            throw new RuntimeException("Error converting and writing bucket " + (rangeStart + finalJ), e);
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
        return DBIndexIO;
    }
}
