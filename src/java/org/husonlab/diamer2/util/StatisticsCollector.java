package org.husonlab.diamer2.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class StatisticsCollector {

    private final double stepSize;
    private final int[] histogram;
    private final ConcurrentHashMap<Long, Integer> kmerCounts;

    public StatisticsCollector(long maxKmerValue, int bins) {
        this.stepSize = (maxKmerValue + 1) / (double)bins;
        this.histogram = new int[bins];
        this.kmerCounts = new ConcurrentHashMap<>();
    }

    /**
     * All buckets can be added
     */
    public void addToHistogram(long kmer) {
        int bucket = (int) (kmer / stepSize);
        synchronized (histogram) {
            histogram[bucket] ++;
        }
    }

    /**
     * Limited number of buckets should be added to prevent memory issues
     */
    public void addToKmerCounts(long kmer) {
        kmerCounts.put(kmer, kmerCounts.getOrDefault(kmer, 0) + 1);
    }

    public void writeStatistics(Path path) {

        HashMap<Integer, Integer> counts = new HashMap<>();
        kmerCounts.forEach((kmer, count) -> {
            counts.put(count, counts.getOrDefault(count, 0) + 1);
        });

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(path.resolve("kmer_frequency.tsv").toString()))) {
            counts.entrySet().stream()
                    .sorted(HashMap.Entry.comparingByKey())
                    .forEach(entry -> {
                        try {
                            bw.write(entry.getKey() + "\t" + entry.getValue() + "\n");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(path.resolve("kmer_histogram.tsv").toString()))) {
            bw.write("bucket\tcount\n");
            for (int i = 0; i < histogram.length; i++) {
                bw.write(((long)((i + 1) * stepSize)) + "\t" + histogram[i] + "\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
