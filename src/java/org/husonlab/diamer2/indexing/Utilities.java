//package org.husonlab.diamer2.indexing;
//
//import org.husonlab.diamer2.io.indexing.BucketIO;
//import org.husonlab.diamer2.io.indexing.IndexIO;
//import org.husonlab.diamer2.seq.alphabet.Base11Alphabet;
//import org.husonlab.diamer2.taxonomy.Tree;
//import org.husonlab.diamer2.util.Pair;
//import org.husonlab.diamer2.util.logging.Logger;
//import org.husonlab.diamer2.util.logging.OneLineLogger;
//import org.husonlab.diamer2.util.logging.ProgressBar;
//import org.husonlab.diamer2.util.logging.Time;
//
//import java.io.BufferedWriter;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.nio.file.Path;
//import java.util.ArrayList;
//import java.util.concurrent.ArrayBlockingQueue;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ThreadPoolExecutor;
//import java.util.concurrent.TimeUnit;
//
//public class Utilities {
//
//    @Deprecated
//    public static void analyzeDBIndex(IndexIO dbIndexIO, Tree tree, Path output, int resolution, int MAX_THREADS) {
//        Logger logger = new Logger("DBIndexAnalyzer");
//        logger.addElement(new Time());
//        logger.logInfo("Calculating index statistics ...");
//        ProgressBar progressBar = new ProgressBar(1024, 20);
//        new OneLineLogger("Index", 1000).addElement(progressBar);
//
//        final int[] globalKmerDistribution = new int[resolution];
//        final long highestIndexEnc = (new Base11Alphabet()).highestEncoding(15);
//        final float factor = (float)resolution/highestIndexEnc;
//        final ConcurrentHashMap<String, int[]> rankKmerDistribution = new ConcurrentHashMap<>();
//
//        ThreadPoolExecutor threadPoolExecutor = new CustomThreadPoolExecutor(
//                MAX_THREADS,
//                MAX_THREADS,
//                500L,
//                TimeUnit.MILLISECONDS,
//                new ArrayBlockingQueue<>(MAX_THREADS),
//                new ThreadPoolExecutor.CallerRunsPolicy());
//
//        for (int i = 0; i < 1024; i++) {
//            BucketIO bucketIO = dbIndexIO.getBucketIO(i);
//            if (bucketIO.exists()) {
//                threadPoolExecutor.submit(() -> {
//                    BucketIO.BucketReader reader = bucketIO.getBucketReader();
//                    while (reader.hasNext()) {
//                        long kmer = reader.next();
//                        int taxId = IndexEncoding.getTaxId(kmer);
//                        long kmerEnc = IndexEncoding.getKmer(kmer, bucketIO.getName());
//                        String rank = tree.idMap.get(taxId).getRank();
//                        int index = (int) (kmerEnc * factor);
//                        if (index >= resolution) {
//                            index = resolution - 1;
//                        }
//
//                        final int finalIndex = index;
//
//                        tree.idMap.get(taxId).addLongWeight(1);
//
//                        rankKmerDistribution.computeIfPresent(rank, (r, counts) -> {
//                            counts[finalIndex] += 1;
//                            return counts;
//                        });
//                        rankKmerDistribution.computeIfAbsent(rank, r -> {
//                            int[] counts = new int[resolution];
//                            counts[finalIndex] = 1;
//                            return counts;
//                        });
//
//                        synchronized (globalKmerDistribution) {
//                            globalKmerDistribution[index] += 1;
//                        }
//                    }
//                });
//            }
//            progressBar.incrementProgress();
//        }
//        threadPoolExecutor.shutdown();
//        try {
//            threadPoolExecutor.awaitTermination(1, TimeUnit.DAYS);
//        } catch (InterruptedException e) {
//            throw new RuntimeException("Error waiting for thread pool to finish.", e);
//        }
//
//        progressBar.finish();
//
//        ArrayList<Pair<String, int[]>> rankKmerDistributionList = new ArrayList<>();
//
//        rankKmerDistribution.entrySet().stream()
//            .sorted((e1, e2) -> Integer.compare(
//                java.util.Arrays.stream(e2.getValue()).sum(),
//                java.util.Arrays.stream(e1.getValue()).sum()))
//            .forEach(entry -> rankKmerDistributionList.add(new Pair<>(entry.getKey(), entry.getValue())));
//
//        try (BufferedWriter bw = new BufferedWriter(new FileWriter(output.resolve("kmer_distribution.tsv").toFile()))) {
//            StringBuilder header = new StringBuilder("range start\trange end\ttotal");
//            for (Pair<String, int[]> rankDistribution : rankKmerDistributionList) {
//                header.append("\t").append(rankDistribution.first());
//            }
//            header.append("\n");
//            bw.write(header.toString());
//            for (int i = 0; i < resolution; i++) {
//                bw.write("%d\t%d\t%d".formatted(i * highestIndexEnc, (i + 1) * highestIndexEnc - 1, globalKmerDistribution[i]));
//                for (Pair<String, int[]> rankDistribution: rankKmerDistributionList) {
//                    bw.write("\t%d".formatted(rankDistribution.last()[i]));
//                }
//                bw.newLine();
//            }
//        } catch (IOException e) {
//            throw new RuntimeException("Error writing output file.", e);
//        }
//    }
//
//}
