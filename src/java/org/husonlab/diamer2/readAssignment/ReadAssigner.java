package org.husonlab.diamer2.readAssignment;

import org.husonlab.diamer2.indexing.CustomThreadPoolExecutor;
import org.husonlab.diamer2.io.BucketReader;
import org.husonlab.diamer2.io.DBIndexIO;
import org.husonlab.diamer2.io.ReadIndexIO;
import org.husonlab.diamer2.logging.Logger;

import java.io.*;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.concurrent.*;

import static org.husonlab.diamer2.indexing.Indexer.shutdownThreadPoolExecutor;

public class ReadAssigner {
    private final Logger logger;
    private final DBIndexIO dbIndex;
    private final ReadIndexIO readsIndex;
    private final int MAX_THREADS;
    private Read[] reads;

    public ReadAssigner(int MAX_THREADS, Path dbIndex, Path readsIndex) throws Exception {
        this.logger = new Logger("ReadAssigner", true);
        this.dbIndex = new DBIndexIO(dbIndex);
        this.readsIndex = new ReadIndexIO(readsIndex);
        this.MAX_THREADS = MAX_THREADS;
    }

    public void assignReads() {
        logger.logInfo("Assigning reads ...");
        ThreadPoolExecutor threadPoolExecutor = new CustomThreadPoolExecutor(
                MAX_THREADS,
                MAX_THREADS,
                500L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(MAX_THREADS),
                new ThreadPoolExecutor.CallerRunsPolicy());

        for (int i = 0; i < 1024; i++) {
            if (dbIndex.isBucketAvailable(i) && !readsIndex.isBucketAvailable(i)) {
                threadPoolExecutor.submit(new BucketAssignmentProcessor(reads, dbIndex, readsIndex, i));
            }
        }

        System.out.println("[ReadAssigner] Waiting for threads to finish ...");
        shutdownThreadPoolExecutor(threadPoolExecutor);
        System.out.println("[ReadAssigner] Finished assigning reads.");
    }

    public Read[] getReadAssignments() {
        return reads;
    }

    public void writeReadAssignments(File file) throws IOException {
        System.out.println("[ReadAssigner] Writing read assignments to file: " + file.getAbsolutePath());
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(reads.length + "\n");
            for (Read read : reads) {
                writer.write(read.toString() + "\n");
            }
        }
    }

    public record Read(String header, ConcurrentLinkedQueue<ReadAssignment> readAssignments) {

        public void addReadAssignment(int taxId) {
            for (ReadAssignment assignment : readAssignments) {
                if (assignment.getTaxId() == taxId) {
                    assignment.incrementCount();
                    return;
                }
            }
            readAssignments.add(new ReadAssignment(taxId, 1));
        }

        public LinkedList<ReadAssignment> getSortedAssignments() {
            LinkedList<ReadAssignment> sortedAssignments = new LinkedList<>(readAssignments);
            sortedAssignments.sort((a1, a2) -> Integer.compare(a2.getCount(), a1.getCount()));
            return sortedAssignments;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(header).append("\t");
            getSortedAssignments().forEach(readAssignment -> sb
                    .append(readAssignment.getTaxId())
                    .append(":")
                    .append(readAssignment.getCount()).append(" "));
            sb.deleteCharAt(sb.length() - 1);
            return sb.toString();
        }
    }

    public static class ReadAssignment {
        private final int taxId;
        private int count;

        public ReadAssignment(int taxId, int count) {
            this.taxId = taxId;
            this.count = count;
        }

        public void incrementCount() {
                count++;
            }

        public int getTaxId() {
            return taxId;
        }

        public int getCount() {
            return count;
        }

        @Override
        public String toString() {
            return taxId + ": " + count;
        }
    }
}
