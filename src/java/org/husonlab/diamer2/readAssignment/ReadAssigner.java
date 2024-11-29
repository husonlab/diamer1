package org.husonlab.diamer2.readAssignment;

import org.husonlab.diamer2.graph.Tree;
import org.husonlab.diamer2.indexing.CustomThreadPoolExecutor;

import java.io.*;
import java.nio.Buffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.concurrent.*;

import static org.husonlab.diamer2.indexing.Indexer.shutdownThreadPoolExecutor;

public class ReadAssigner {
    private final Tree tree;
    private final int MAX_THREADS;
    private final LinkedList<int[]> bucketRangesToProcess;
    private int[] currentBucketRange = new int[2];
    private ReadAssignment[] readAssignments;

    public ReadAssigner(Tree tree, int MAX_THREADS) {
        // Initialize the parameters of the Indexer
        this.tree = tree;
        this.MAX_THREADS = MAX_THREADS;
        this.bucketRangesToProcess = new LinkedList<>();
        // Divide the 1024 buckets into ranges to process in each cycle
        int i = 0;
        while (i < 1024) {
            int[] bucketRange = new int[2];
            bucketRange[0] = i;
            bucketRange[1] = Math.min(i + MAX_THREADS, 1024);
            bucketRangesToProcess.add(bucketRange);
            i += MAX_THREADS;
        }
    }

    public void readHeaderIndex(Path readIndex) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(readIndex.resolve("header_index.txt").toFile()))) {
            int length = Integer.parseInt(reader.readLine());
            readAssignments = new ReadAssignment[length];
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t");
                int readId = Integer.parseInt(parts[0]);
                String header = parts[1];
                readAssignments[readId] = new ReadAssignment(header, new ConcurrentHashMap<Integer, Integer>());
            }
        }
    }

    public void assignReads(Path dbIndex, Path readsIndex) {
        System.out.println("[ReadAssigner] Assigning reads ...");

        ThreadPoolExecutor threadPoolExecutor = new CustomThreadPoolExecutor(
                MAX_THREADS,
                MAX_THREADS,
                500L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(MAX_THREADS),
                new ThreadPoolExecutor.CallerRunsPolicy());

        for (int i = 0; i < 1024; i++) {
            threadPoolExecutor.execute(new BucketAssignmentProcessor(i, readAssignments, dbIndex, readsIndex));
        }

        System.out.println("[ReadAssigner] Waiting for threads to finish ...");
        shutdownThreadPoolExecutor(threadPoolExecutor);
        System.out.println("[ReadAssigner] Finished assigning reads.");
    }

    public ReadAssignment[] getReadAssignments() {
        return readAssignments;
    }

    public void writeReadAssignments(File file) throws IOException {
        System.out.println("[ReadAssigner] Writing read assignments to file: " + file.getAbsolutePath());
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(readAssignments.length + "\n");
            for (ReadAssignment readAssignment : readAssignments) {
                writer.write(readAssignment.toString() + "\n");
            }
        }
    }

    public record ReadAssignment(String header, ConcurrentHashMap<Integer, Integer> taxIds) {
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(header).append("\t");
            taxIds.forEach((taxId, count) -> sb.append(taxId).append(":").append(count).append(" "));
            sb.deleteCharAt(sb.length() - 1);
            return sb.toString();
        }
    }
}
