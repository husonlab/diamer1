package org.husonlab.diamer2.indexing;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class Utilities {
    public static void writeKmerStatistics(ConcurrentHashMap<Long, Integer> kmerCounts, Path path) {
        final AtomicLong maxKmerValue = new AtomicLong(0);
        HashMap<Integer, Integer> counts = new HashMap<>();
        kmerCounts.forEach((kmer, count) -> {
            if (kmer > maxKmerValue.get()) {
                maxKmerValue.set(kmer);
            }
            counts.put(count, counts.getOrDefault(count, 0) + 1);
        });
        final double stepSize = (maxKmerValue.get() + 1) / 1000.0;
        final int[] histogram = new int[1000];
        kmerCounts.forEach((kmer, count) -> {
            int bucket = (int) (kmer / stepSize);
            histogram[bucket] += count;
        });

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(path.resolve("kmer_frequency.tsv").toString()))) {
            bw.write("frequency\tcount\n");
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