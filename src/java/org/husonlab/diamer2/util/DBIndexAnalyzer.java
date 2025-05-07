package org.husonlab.diamer2.util;

import org.husonlab.diamer2.indexing.CustomThreadPoolExecutor;
import org.husonlab.diamer2.indexing.StatisticsCollector;
import org.husonlab.diamer2.io.indexing.BucketIO;
import org.husonlab.diamer2.io.indexing.DBIndexIO;
import org.husonlab.diamer2.main.GlobalSettings;
import org.husonlab.diamer2.main.encoders.Encoder;
import org.husonlab.diamer2.taxonomy.Node;
import org.husonlab.diamer2.taxonomy.Tree;
import org.husonlab.diamer2.util.logging.Logger;
import org.husonlab.diamer2.util.logging.OneLineLogger;
import org.husonlab.diamer2.util.logging.ProgressBar;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class DBIndexAnalyzer {
    private final Path output;
    private final Encoder encoder;
    private final GlobalSettings settings;
    private final Tree tree;
    private final ConcurrentHashMap<String, StatisticsCollector> statisticsCollectors;
    private final HashMap<String, Long> kmerPerRank;
    private final DBIndexIO dbIndexIO;

    public DBIndexAnalyzer(Encoder encoder, GlobalSettings settings) {
        this.output = settings.OUTPUT;
        this.encoder = encoder;
        this.settings = settings;
        this.dbIndexIO = encoder.getDBIndexIO();
        this.tree = dbIndexIO.getTree();
        this.statisticsCollectors = new ConcurrentHashMap<>();
        this.kmerPerRank = new HashMap<>();
    }

    public String analyze() {
        if (settings.ONLY_STANDARD_RANKS) {
            tree.reduceToStandardRanks();
        }

        Thread kmerPerRankCollector = getKmerPerRankCollector();

        ProgressBar progressBar = new ProgressBar(encoder.getNrOfBuckets(), 20);
        new OneLineLogger("AnalyzeDBIndex", 500).addElement(progressBar);

        try (CustomThreadPoolExecutor executor = new CustomThreadPoolExecutor(
                1, settings.MAX_THREADS, settings.MAX_THREADS * 2, 5, new Logger("AnalyzeDBIndex"))) {
            for (int i = 0; i < encoder.getNrOfBuckets(); i++) {
                final BucketIO.BucketReader bucketReader = dbIndexIO.getBucketReader(i);
                int finalI = i;
                executor.submit(() -> {
                    for (int j = 0; j < bucketReader.getLength(); j++) {
                        long kmerEnc = bucketReader.next();
                        int taxId = encoder.getIdFromIndexEntry(kmerEnc);
                        long kmer = encoder.getKmerFromIndexEntry(finalI, kmerEnc);
                        Node node = tree.getNode(taxId);
                        if (statisticsCollectors.containsKey(node.getRank())) {
                            StatisticsCollector statisticsCollector = statisticsCollectors.get(node.getRank());
                            statisticsCollector.addToHistogram(kmer);
                        } else {
                            StatisticsCollector statisticsCollector = new StatisticsCollector(encoder.getMaxKmerValue(), 1000);
                            statisticsCollector.addToHistogram(kmer);
                            statisticsCollectors.put(node.getRank(), statisticsCollector);
                        }
                    }
                    progressBar.incrementProgress();
                });
            }
        }
        progressBar.finish();

        statisticsCollectors.forEach((rank, statisticsCollector) -> {
            statisticsCollector.writeKmerHistogram(output.resolve(rank + "_kmer_histogram.tsv"));
        });

        try {
            kmerPerRankCollector.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return "";
    }

    @NotNull
    private Thread getKmerPerRankCollector() {
        Thread kmerPerRankCollector = new Thread(() -> {
            tree.idMap.forEach((taxId, node) -> {
                kmerPerRank.put(node.getRank(), kmerPerRank.getOrDefault(node.getRank(), 0L) + tree.getLongProperty(taxId, "kmers in database"));
            });
            try(BufferedWriter bw = new BufferedWriter(new java.io.FileWriter(output.resolve("kmer_per_rank.tsv").toString()))){
                bw.write("rank\tkmers\n");
                kmerPerRank.forEach((rank, kmers) -> {
                    try {
                        bw.write(rank + "\t" + kmers + "\n");
                    } catch (java.io.IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        });
        kmerPerRankCollector.setName("KmerPerRankCollector");
        kmerPerRankCollector.start();
        return kmerPerRankCollector;
    }
}
