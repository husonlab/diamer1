package org.husonlab.diamer2.indexing;

import org.husonlab.diamer2.io.CountingInputStream;
import org.husonlab.diamer2.io.FASTAReader;
import org.husonlab.diamer2.io.IndexIO;
import org.husonlab.diamer2.logging.*;
import org.husonlab.diamer2.seq.Sequence;
import org.husonlab.diamer2.taxonomy.Tree;

import java.io.*;
import java.nio.file.Path;
import java.util.concurrent.*;

public class DBIndexer {

    private final Logger logger;
    private final File fastaFile;
    private final Path indexDir;
    private final IndexIO indexIO;
    private final Tree tree;
    private final int MAX_THREADS;
    private final int MAX_QUEUE_SIZE;
    private final int BATCH_SIZE;
    private final int bucketsPerCycle;
    private final boolean debug;

    public DBIndexer(File fastaFile,
                     Path indexDir,
                     Tree tree,
                     int MAX_THREADS,
                     int MAX_QUEUE_SIZE,
                     int BATCH_SIZE,
                     int bucketsPerCycle,
                     boolean debug) {
        this.logger = new Logger("DBIndexer");
        this.fastaFile = fastaFile;
        this.indexDir = indexDir;
        this.indexIO = new IndexIO(indexDir);
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
    public IndexIO index() throws IOException {
        logger.logInfo("Indexing " + fastaFile + " to " + indexDir);
        ProgressBar progressBar = new ProgressBar(fastaFile.length(), 20);
        Message progressMessage = new Message("");
        new OneLineLogger("Indexer", 1000)
                .addElement(progressBar)
                .addElement(progressMessage);

        ThreadPoolExecutor threadPoolExecutor = new CustomThreadPoolExecutor(
                MAX_THREADS,
                MAX_THREADS,
                500L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(MAX_QUEUE_SIZE),
                new ThreadPoolExecutor.CallerRunsPolicy());

        Phaser indexPhaser = new Phaser(0);

        for (int i = 0; i < 1024; i += bucketsPerCycle) {
            int rangeStart = i;
            int rangeEnd = Math.min(i + bucketsPerCycle, 1024);
            indexPhaser.register();

            progressMessage.setMessage("Indexing buckets " + rangeStart + " - " + rangeEnd);

            // Initialize Concurrent HashMaps to store the kmers and their taxIds during indexing
            final ConcurrentHashMap<Long, Integer>[] bucketMaps = new ConcurrentHashMap[bucketsPerCycle];
            if (!debug) {
                for (int j = 0; j < bucketsPerCycle; j++) {
                    bucketMaps[j] = new ConcurrentHashMap<Long, Integer>(57000000); // initial capacity 57000000
                }
            } else {
                for (int j = 0; j < bucketsPerCycle; j++) {
                    bucketMaps[j] = new ConcurrentHashMap<Long, Integer>();
                }
            }

            try (CountingInputStream cis = new CountingInputStream(new FileInputStream(fastaFile));
                 BufferedReader br = new BufferedReader(new InputStreamReader(cis));
                 FASTAReader fr = new FASTAReader(br)) {
                progressBar.setProgress(0);
                Sequence[] batch = new Sequence[BATCH_SIZE];
                int batchPosition = 0;
                Sequence seq;
                while ((seq = fr.next()) != null) {
                    batch[batchPosition] = seq;
                    progressBar.setProgress(cis.getReadBytes());

                    if (batchPosition == BATCH_SIZE - 1) {
                        threadPoolExecutor.submit(new FastaBatchProcessor(indexPhaser, batch, bucketMaps, tree, rangeStart, rangeEnd));
                        batch = new Sequence[BATCH_SIZE];
                        batchPosition = 0;
                    } else {
                        batchPosition++;
                    }
                }
                threadPoolExecutor.submit(new FastaBatchProcessor(indexPhaser, batch, bucketMaps, tree, rangeStart, rangeEnd));
                progressBar.finish();
            }

            indexPhaser.arriveAndAwaitAdvance();
            logger.logInfo("Converting, sorting and writing buckets " + rangeStart + " - " + rangeEnd);
            indexPhaser.register();

            for (int j = 0; j < bucketsPerCycle; j++) {
                int finalJ = j;
                threadPoolExecutor.submit(() -> {
                    try {
                        indexPhaser.register();
                        indexIO.getBucketIO(finalJ + rangeStart).write(new Bucket(finalJ, bucketMaps[finalJ]));
                    } catch (RuntimeException | IOException e) {
                        throw new RuntimeException("Error converting and writing bucket " + finalJ, e);
                    } finally {
                        indexPhaser.arriveAndDeregister();
                    }
                });
            }
            indexPhaser.arriveAndAwaitAdvance();
        }
        return indexIO;
    }
}
