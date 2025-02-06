package org.husonlab.diamer2.indexing;

import org.husonlab.diamer2.io.indexing.DBIndexIO;
import org.husonlab.diamer2.io.indexing.ReadIndexIO;
import org.husonlab.diamer2.io.seq.FastqIdReader;
import org.husonlab.diamer2.io.seq.SequenceRecordContainer;
import org.husonlab.diamer2.io.seq.SequenceSupplier;
import org.husonlab.diamer2.main.encoders.Encoder;
import org.husonlab.diamer2.util.logging.*;

import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.*;

public class ReadIndexer {

    private final Logger logger;
    private final Path fastqFile;
    private final ReadIndexIO readIndexIO;
    private final Encoder encoder;
    private final int bucketsPerCycle;
    private final int MAX_THREADS;
    private final int MAX_QUEUE_SIZE;
    private final int BATCH_SIZE;
    private final boolean KEEP_IN_MEMORY;

    public ReadIndexer(Path fastqFile,
                       Path indexDir,
                       Encoder encoder,
                       int bucketsPerCycle, int MAX_THREADS,
                       int MAX_QUEUE_SIZE,
                       int BATCH_SIZE,
                       boolean KEEP_IN_MEMORY) {
        this.logger = new Logger("ReadIndexer");
        logger.addElement(new Time());
        this.fastqFile = fastqFile;
        this.readIndexIO = new ReadIndexIO(indexDir);
        this.encoder = encoder;
        this.bucketsPerCycle = bucketsPerCycle;
        this.MAX_THREADS = MAX_THREADS;
        this.MAX_QUEUE_SIZE = MAX_QUEUE_SIZE;
        this.BATCH_SIZE = BATCH_SIZE;
        this.KEEP_IN_MEMORY = KEEP_IN_MEMORY;
    }

    /**
     * Indexes a FASTQ file of sequencing reads.
     * @throws IOException If an error occurs during reading the file or writing the buckets.
     */
    public DBIndexIO index() throws IOException {
        logger.logInfo("Indexing " + fastqFile + " to " + readIndexIO.getIndexFolder());

        ThreadPoolExecutor threadPoolExecutor = new CustomThreadPoolExecutor(
                MAX_THREADS,
                MAX_THREADS,
                500L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(MAX_QUEUE_SIZE),
                new ThreadPoolExecutor.CallerRunsPolicy());
        Phaser indexPhaser = new Phaser(1);

        try (FastqIdReader fastqIdReader = new FastqIdReader(fastqFile);
             SequenceSupplier<Integer, Byte> sup = new SequenceSupplier<>(fastqIdReader, encoder.getDNAConverter(), KEEP_IN_MEMORY)) {

            ProgressBar progressBar = new ProgressBar(sup.getFileSize(), 20);
            Message progressMessage = new Message("");
            new OneLineLogger("ReadIndexer", 1000)
                    .addElement(new RunningTime())
                    .addElement(progressBar)
                    .addElement(progressMessage);

            // Initialize Concurrent LinkedQueue to store index entries of each bucket
            final ConcurrentLinkedQueue<Long>[] bucketLists = new ConcurrentLinkedQueue[bucketsPerCycle];

            final int maxBuckets = (int) Math.pow(2, encoder.getBitsOfBucketNames());
            for (int i = 0; i < maxBuckets; i += bucketsPerCycle) {
                int rangeStart = i;
                int rangeEnd = Math.min(i + bucketsPerCycle, maxBuckets);
                int readId = 0;

                progressMessage.setMessage(" Indexing buckets " + rangeStart + " - " + rangeEnd);

                for (int j = 0; j < bucketsPerCycle; j++) {
                    bucketLists[j] = new ConcurrentLinkedQueue<>();
                }

                progressBar.setProgress(0);
                SequenceRecordContainer<Integer, Byte>[] batch = new SequenceRecordContainer[BATCH_SIZE];
                int batchPosition = 0;
                SequenceRecordContainer<Integer, Byte> container;
                while ((container = sup.next()) != null) {
                    batch[batchPosition] = container;
                    progressBar.setProgress(sup.getBytesRead());

                    if (batchPosition == BATCH_SIZE - 1) {
                        indexPhaser.register();
                        threadPoolExecutor.submit(
                                new FastqDNAProcessor(
                                        indexPhaser,
                                        batch,
                                        encoder,
                                        bucketLists,
                                        rangeStart,
                                        rangeEnd)
                        );
                        batch = new SequenceRecordContainer[BATCH_SIZE];
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
                                Arrays.copyOfRange(batch, 0, batchPosition),
                                encoder,
                                bucketLists,
                                rangeStart,
                                rangeEnd));
                progressBar.finish();

                indexPhaser.arriveAndAwaitAdvance();
                logger.logInfo("Converting, sorting and writing buckets " + rangeStart + " - " + rangeEnd);

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

                for (int j = 0; j < Math.min(bucketsPerCycle, (maxBuckets - rangeStart)); j++) {
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

