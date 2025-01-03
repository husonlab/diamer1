package org.husonlab.diamer2.indexing;

import org.husonlab.diamer2.io.*;
import org.husonlab.diamer2.logging.*;
import org.husonlab.diamer2.seq.Sequence;
import org.husonlab.diamer2.seq.alphabet.ReducedProteinAlphabet;

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.*;

public class ReadIndexer {

    private final Logger logger;
    private final File fastqFile;
    private final Path indexDir;
    private final ReadIndexIO readIndexIO;
    private final long mask;
    private final ReducedProteinAlphabet alphabet;
    private final int MAX_THREADS;
    private final int MAX_QUEUE_SIZE;
    private final int BATCH_SIZE;
    private final int bucketsPerCycle;

    public ReadIndexer(File fastqFile,
                     Path indexDir,
                     long mask,
                     ReducedProteinAlphabet alphabet,
                     int MAX_THREADS,
                     int MAX_QUEUE_SIZE,
                     int BATCH_SIZE,
                     int bucketsPerCycle) {
        this.logger = new Logger("ReadIndexer");
        this.fastqFile = fastqFile;
        this.indexDir = indexDir;
        this.readIndexIO = new ReadIndexIO(indexDir);
        this.mask = mask;
        this.alphabet = alphabet;
        this.MAX_THREADS = MAX_THREADS;
        this.MAX_QUEUE_SIZE = MAX_QUEUE_SIZE;
        this.BATCH_SIZE = BATCH_SIZE;
        this.bucketsPerCycle = bucketsPerCycle;
    }

    /**
     * Indexes a FASTQ file of sequencing reads.
     * @throws IOException If an error occurs during reading the file or writing the buckets.
     */
    public IndexIO index() throws IOException {
        logger.logInfo("Indexing " + fastqFile + " to " + indexDir);

        ThreadPoolExecutor threadPoolExecutor = new CustomThreadPoolExecutor(
                MAX_THREADS,
                MAX_THREADS,
                500L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(MAX_QUEUE_SIZE),
                new ThreadPoolExecutor.CallerRunsPolicy());
        Phaser indexPhaser = new Phaser(1);

        // HashMap to store readId to header mapping to be able to go back from id to header during read assignment
        HashMap<Integer, String> readHeaderMap = new HashMap<>();

        try (SequenceSupplier sup = SequenceSupplier.getFastqSupplier(fastqFile, true)) {

            ProgressBar progressBar = new ProgressBar(sup.getFileSize(), 20);
            Message progressMessage = new Message("");
            new OneLineLogger("ReadIndexer", 1000)
                    .addElement(progressBar)
                    .addElement(progressMessage);

            // Initialize Concurrent LinkedQueue to store index entries of each bucket
            final ConcurrentLinkedQueue<Long>[] bucketLists = new ConcurrentLinkedQueue[bucketsPerCycle];

            for (int i = 0; i < 1024; i += bucketsPerCycle) {
                int rangeStart = i;
                int rangeEnd = Math.min(i + bucketsPerCycle, 1024);
                int readId = 0;

                progressMessage.setMessage(" Indexing buckets " + rangeStart + " - " + rangeEnd);

                for (int j = 0; j < bucketsPerCycle; j++) {
                    bucketLists[j] = new ConcurrentLinkedQueue<>();
                }

                progressBar.setProgress(0);
                Sequence[] batch = new Sequence[BATCH_SIZE];
                int batchPosition = 0;
                Sequence seq;
                while ((seq = sup.next()) != null) {
                    if (i == 0) {
                        readHeaderMap.put(readId, seq.getHeader());
                    }
                    batch[batchPosition] = seq;
                    progressBar.setProgress(sup.getBytesRead());

                    if (batchPosition == BATCH_SIZE - 1) {
                        indexPhaser.register();
                        threadPoolExecutor.submit(
                                new FastqDNAProcessor(
                                        indexPhaser,
                                        batch,
                                        mask,
                                        alphabet,
                                        bucketLists,
                                        rangeStart,
                                        rangeEnd,
                                        readId - BATCH_SIZE)
                        );
                        batch = new Sequence[BATCH_SIZE];
                        batchPosition = 0;
                    } else {
                        batchPosition++;
                    }
                    readId++;
                }
                indexPhaser.register();
                threadPoolExecutor.submit(
                        new FastqDNAProcessor(
                                indexPhaser,
                                batch,
                                mask,
                                alphabet,
                                bucketLists,
                                rangeStart,
                                rangeEnd,
                                (readId - 1) - (readId - 1) % BATCH_SIZE));
                progressBar.finish();

                indexPhaser.arriveAndAwaitAdvance();
                logger.logInfo("Converting, sorting and writing buckets " + rangeStart + " - " + rangeEnd);

                if (rangeStart == 0) {
                    indexPhaser.register();
                    threadPoolExecutor.submit(() -> {
                        try {
                            readIndexIO.writeReadHeaderMapping(readHeaderMap);
                        } catch (RuntimeException e) {
                            throw new RuntimeException("Error writing read header map.", e);
                        } finally {
                            indexPhaser.arriveAndDeregister();
                        }
                    });
                }

                indexPhaser.arriveAndAwaitAdvance();

                for (int j = 0; j < bucketsPerCycle; j++) {
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
        return readIndexIO;
    }
}

