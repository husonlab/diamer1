package org.husonlab.diamer2.readAssignment;

import org.husonlab.diamer2.indexing.CustomThreadPoolExecutor;
import org.husonlab.diamer2.io.IndexIO;
import org.husonlab.diamer2.io.ReadIndexIO;
import org.husonlab.diamer2.logging.*;
import org.husonlab.diamer2.taxonomy.Tree;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.*;

public class ReadAssigner {
    private final Logger logger;
    private final Tree tree;
    private final IndexIO dbIndex;
    private final ReadIndexIO readsIndex;
    private final int MAX_THREADS;
    private final String[] readHeaderMapping;

    public ReadAssigner(Tree tree, int MAX_THREADS, Path dbIndexPath, Path readsIndexPath) {
        this.logger = new Logger("ReadAssigner").addElement(new Time());
        this.tree = tree;
        this.dbIndex = new IndexIO(dbIndexPath);
        this.readsIndex = new ReadIndexIO(readsIndexPath);
        this.MAX_THREADS = MAX_THREADS;
        if(readsIndex.existsReadHeaderMapping()) {
            this.readHeaderMapping = this.readsIndex.getReadHeaderMapping();
        } else {
            throw new RuntimeException("Read header mapping file is missing from the reads index folder.");
        }
        if(dbIndex.bucketMissing() || readsIndex.bucketMissing()) {
            logger.logWarning("At least one index file is missing, proceeding with available buckets.");
        }
    }

    /**
     * Compares the database and the read index and assigns taxIds with kmer counts to the reads.
     */
    public ReadAssignment assignReads() {
        logger.logInfo("Assigning readAssignment ...");
        ProgressBar progressBar = new ProgressBar(1024, 20);
        Logger progressBarLogger = new OneLineLogger("ReadAssigner", 1000)
                .addElement(new RunningTime())
                .addElement(progressBar);

        final ReadAssignment readAssignment = new ReadAssignment(tree, readHeaderMapping);
        ThreadPoolExecutor threadPoolExecutor = new CustomThreadPoolExecutor(
                MAX_THREADS,
                MAX_THREADS,
                500L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(1024),
                new ThreadPoolExecutor.CallerRunsPolicy());

        int bucketsSkipped = 0;

        for (int i = 0; i < 1024; i++) {
            if (dbIndex.isBucketAvailable(i) && readsIndex.isBucketAvailable(i)) {
                threadPoolExecutor.submit(new BucketProcessor(readAssignment, dbIndex, readsIndex, i));
            } else {
                bucketsSkipped++;
            }
        }

        // Wait for all threads to finish and update progress bar
        threadPoolExecutor.shutdown();
        while (!threadPoolExecutor.isTerminated()) {
            progressBar.setProgress(threadPoolExecutor.getCompletedTaskCount() + bucketsSkipped);
            try {
                threadPoolExecutor.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        progressBar.finish();
        progressBarLogger.logInfo("Finished assigning readAssignment.");
        return readAssignment;
    }
}
