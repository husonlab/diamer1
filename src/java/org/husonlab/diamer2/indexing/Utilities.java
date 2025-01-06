package org.husonlab.diamer2.indexing;

import org.husonlab.diamer2.io.indexing.BucketIO;
import org.husonlab.diamer2.io.indexing.DBIndexIO;
import org.husonlab.diamer2.taxonomy.Tree;
import org.husonlab.diamer2.util.logging.Logger;
import org.husonlab.diamer2.util.logging.OneLineLogger;
import org.husonlab.diamer2.util.logging.ProgressBar;
import org.husonlab.diamer2.util.logging.Time;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Utilities {

    public static void analyzeDBIndex(DBIndexIO dbIndexIO, Tree tree, Path output, int resolution, int MAX_THREADS) {
        Logger logger = new Logger("DBIndexAnalyzer");
        logger.addElement(new Time());
        logger.logInfo("Calculating index statistics ...");
        ProgressBar progressBar = new ProgressBar(1024, 20);
        new OneLineLogger("Index", 1000).addElement(progressBar);

        final int[] globalKmerDistribution = new int[resolution];
        final float factor = 4177248169415650f / resolution;
        final ConcurrentHashMap<String, int[]> rankKmerDistribution = new ConcurrentHashMap<>();

        ThreadPoolExecutor threadPoolExecutor = new CustomThreadPoolExecutor(
                MAX_THREADS,
                MAX_THREADS,
                500L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(1024),
                new ThreadPoolExecutor.CallerRunsPolicy());

        for (int i = 0; i < 1024; i++) {
            BucketIO bucketIO = dbIndexIO.getBucketIO(i);
            if (bucketIO.exists()) {
                threadPoolExecutor.submit(() -> {
                    BucketIO.BucketReader reader = bucketIO.getBucketReader();
                    while (reader.hasNext()) {
                        long kmer = reader.next();
                        int taxId = IndexEncoding.getTaxId(kmer);
                        long kmerEnc = IndexEncoding.getKmer(kmer, bucketIO.getName());
                        String rank = tree.idMap.get(taxId).getRank();
                        int index = (int) (kmerEnc / factor);
                        if (index >= 100) {
                            System.out.println("Index out of bounds: " + index);
                            System.out.println(Long.toBinaryString(kmer) + " " + Long.toBinaryString(kmer).length());
                            System.out.println(Long.toBinaryString(taxId) + " " + Long.toBinaryString(taxId).length());
                            System.out.println(Long.toBinaryString(kmerEnc) + " " + Long.toBinaryString(kmerEnc).length());
                            System.out.println(factor);
                        }

                        tree.idMap.get(taxId).addWeight(1);

                        rankKmerDistribution.computeIfPresent(rank, (r, counts) -> {
                            counts[index] += 1;
                            return counts;
                        });
                        rankKmerDistribution.computeIfAbsent(rank, r -> {
                            int[] counts = new int[resolution];
                            counts[index] = 1;
                            return counts;
                        });

                        synchronized (globalKmerDistribution) {
                            globalKmerDistribution[index] += 1;
                        }
                    }
                });
            }
            progressBar.incrementProgress();
        }
        threadPoolExecutor.shutdown();
        try {
            threadPoolExecutor.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            throw new RuntimeException("Error waiting for thread pool to finish.", e);
        }

        progressBar.finish();
    }

}
