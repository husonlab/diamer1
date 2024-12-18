package org.husonlab.diamer2.indexing;

import org.husonlab.diamer2.io.CountingInputStream;
import org.husonlab.diamer2.io.FASTAReader;
import org.husonlab.diamer2.io.FASTQReader;
import org.husonlab.diamer2.io.IndexIO;
import org.husonlab.diamer2.logging.*;
import org.husonlab.diamer2.seq.Sequence;

import java.io.*;
import java.nio.file.Path;
import java.util.concurrent.*;

public class ReadIndexer {

    private final Logger logger;
    private final File fastqFile;
    private final Path indexDir;
    private final IndexIO indexIO;
    private final int MAX_THREADS;
    private final int MAX_QUEUE_SIZE;
    private final int BATCH_SIZE;
    private final int bucketsPerCycle;

    public ReadIndexer(File fastqFile,
                     Path indexDir,
                     int MAX_THREADS,
                     int MAX_QUEUE_SIZE,
                     int BATCH_SIZE,
                     int bucketsPerCycle,
                     boolean debug) {
        this.logger = new Logger("ReadIndexer");
        this.fastqFile = fastqFile;
        this.indexDir = indexDir;
        this.indexIO = new IndexIO(indexDir);
        this.MAX_THREADS = MAX_THREADS;
        this.MAX_QUEUE_SIZE = MAX_QUEUE_SIZE;
        this.BATCH_SIZE = BATCH_SIZE;
        this.bucketsPerCycle = bucketsPerCycle;
    }

    /**
     * DBIndexIO a Sequence database and write the indexed buckets to files.
     * @throws IOException If an error occurs during reading the database or writing the buckets.
     */
    public IndexIO index() throws IOException {
        logger.logInfo("Indexing " + fastqFile + " to " + indexDir);
        ProgressBar progressBar = new ProgressBar(fastqFile.length(), 20);
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
            int processedReads = 0;
            indexPhaser.register();

            progressMessage.setMessage("Indexing buckets " + rangeStart + " - " + rangeEnd);

            // Initialize Concurrent LinkedQueue to store index entries of each bucket
            final ConcurrentLinkedQueue<Long>[] bucketLists = new ConcurrentLinkedQueue[bucketsPerCycle];
            for (int j = 0; j < bucketsPerCycle; j++) {
                bucketLists[j] = new ConcurrentLinkedQueue<>();
            }

            try (CountingInputStream cis = new CountingInputStream(new FileInputStream(fastqFile));
                 BufferedReader br = new BufferedReader(new InputStreamReader(cis));
                 FASTQReader fr = new FASTQReader(br)) {
                progressBar.setProgress(0);
                Sequence[] batch = new Sequence[BATCH_SIZE];
                int batchPosition = 0;
                Sequence seq;
                while ((seq = fr.next()) != null) {
                    processedReads++;
                    batch[batchPosition] = seq;
                    progressBar.setProgress(cis.getReadBytes());

                    if (batchPosition == BATCH_SIZE - 1) {
                        threadPoolExecutor.submit(
                                new FastQBatchProcessor(
                                        indexPhaser,
                                        batch,
                                        bucketLists,
                                        rangeStart,
                                        rangeEnd,
                                        processedReads - BATCH_SIZE)
                        );
                        batch = new Sequence[BATCH_SIZE];
                        batchPosition = 0;
                    } else {
                        batchPosition++;
                    }
                }
                threadPoolExecutor.submit(
                        new FastQBatchProcessor(
                                indexPhaser,
                                batch,
                                bucketLists,
                                rangeStart,
                                rangeEnd,
                                processedReads - processedReads % BATCH_SIZE));
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
                        indexIO.getBucketIO(finalJ + rangeStart).write(new Bucket(finalJ, bucketLists[finalJ]));
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

