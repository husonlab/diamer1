package org.husonlab.diamer2.readAssignment;

import org.husonlab.diamer2.indexing.CustomThreadPoolExecutor;
import org.husonlab.diamer2.io.DBIndexIO;
import org.husonlab.diamer2.io.ReadIndexIO;
import org.husonlab.diamer2.logging.*;
import org.husonlab.diamer2.taxonomy.Tree;

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.*;

public class ReadAssigner {
    private final Logger logger;
    private final Tree tree;
    private final DBIndexIO dbIndex;
    private final ReadIndexIO readsIndex;
    private final int MAX_THREADS;
    private HashMap<Integer, String> readHeaderMapping;
    private final Read[] reads;

    public ReadAssigner(Tree tree, int MAX_THREADS, Path dbIndex, Path readsIndex) throws Exception {
        this.logger = new Logger("ReadAssigner").addElement(new Time());
        this.tree = tree;
        this.dbIndex = new DBIndexIO(dbIndex);
        this.readsIndex = new ReadIndexIO(readsIndex);
        this.MAX_THREADS = MAX_THREADS;
        this.readHeaderMapping = this.readsIndex.getReadHeaderMapping();
        // Initialize array with all reads
        this.reads = new Read[readHeaderMapping.size()];
        for (int i = 0; i < reads.length; i++) {
            reads[i] = new Read(readHeaderMapping.get(i));
        }
    }

    /**
     * Compares the database and the read index and assigns taxIds with kmer counts to the reads.
     */
    public void assignReads() {
        logger.logInfo("Assigning reads ...");
        ProgressBar progressBar = new ProgressBar(1024, 20);
        Logger progressBarLogger = new OneLineLogger("ReadAssigner", 1000)
                .addElement(new RunningTime())
                .addElement(progressBar);

        ThreadPoolExecutor threadPoolExecutor = new CustomThreadPoolExecutor(
                MAX_THREADS,
                MAX_THREADS,
                500L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(1024),
                new ThreadPoolExecutor.CallerRunsPolicy());

        for (int i = 0; i < 1024; i++) {
            if (dbIndex.isBucketAvailable(i) && readsIndex.isBucketAvailable(i)) {
                threadPoolExecutor.submit(new BucketAssignmentProcessor(tree, reads, dbIndex, readsIndex, i));
            }
        }

        // Wait for all threads to finish and update progress bar
        threadPoolExecutor.shutdown();
        while (!threadPoolExecutor.isTerminated()) {
            progressBar.setProgress(threadPoolExecutor.getCompletedTaskCount());
            try {
                threadPoolExecutor.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        progressBarLogger.logInfo("Finished assigning reads.");
    }

    /**
     * Get the read assignments. Does only make sense to call after assignReads() has been called.
     * @return the read assignments.
     */
    public Read[] getReadAssignments() {
        return reads;
    }

    /**
     * Writes the assignment to a file.
     * @param file the file to write the assignments to.
     */
    public void writeReadAssignments(File file) throws IOException {
        logger.logInfo("Writing read assignments to file: " + file.getAbsolutePath());
        ProgressBar progressBar = new ProgressBar(reads.length, 20);
        Logger progressBarLogger = new OneLineLogger("ReadAssigner", 1000)
                .addElement(progressBar);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(reads.length + "\n");
            for (Read read : reads) {
                writer.write(read.toString() + "\n");
                progressBar.incrementProgress();
            }
        }
        progressBar.finish();
    }
}
