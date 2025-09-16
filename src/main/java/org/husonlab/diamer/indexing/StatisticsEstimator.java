package org.husonlab.diamer.indexing;

import org.husonlab.diamer.indexing.kmers.KmerExtractor;
import org.husonlab.diamer.io.seq.FutureSequenceRecords;
import org.husonlab.diamer.io.seq.SequenceSupplier;
import org.husonlab.diamer.main.encoders.Encoder;
import org.husonlab.diamer.seq.SequenceRecord;
import org.husonlab.diamer.util.Pair;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;

/**
 * Class to estimate some statistics and required memory.
 */
public class StatisticsEstimator {
    private final int sampleSize;
    private final Path file;
    private final int[] estimatedBucketSizes;
    private final int maxBucketSize;
    private final HashMap<Byte, Long> charCounts;
    private final double[] charFrequencies;

    public StatisticsEstimator(SequenceSupplier<?, byte[]> sup, Encoder encoder, int numberOfSequences) {
        this.sampleSize = numberOfSequences;
        this.file = sup.getFile();
        Pair<int[], HashMap<Byte, Long>> kmersAncChars = countKmersAndChars(sup, encoder, numberOfSequences);
        this.estimatedBucketSizes = kmersAncChars.first();
        this.charCounts = kmersAncChars.last();
        this.maxBucketSize = calculateMaxBucketSize(estimatedBucketSizes);
        this.charFrequencies = calculateCharFrequencies(charCounts, encoder);
    }

    public static Pair<int[], HashMap<Byte, Long>> countKmersAndChars(SequenceSupplier<?, byte[]> sup, Encoder encoder, int numberOfSequences) {
//        HashMap<Long, Integer> kmerCounts = new HashMap<>();

        HashMap<Byte, Long> charCounts = new HashMap<>();
        int[] bucketSizes = new int[encoder.getNrOfBuckets()];
        KmerExtractor kmerExtractor = encoder.getKmerExtractor();
        FutureSequenceRecords<?, byte[]> sequenceRecords;
        int n = 0;
        try {
            while ((sequenceRecords = sup.next()) != null && n++ <= numberOfSequences) {
                for (SequenceRecord<?, byte[]> sequenceRecord : sequenceRecords.getSequenceRecords()) {
                    // Count characters
                    for (byte b : sequenceRecord.sequence()) {
                        charCounts.put(b, charCounts.getOrDefault(b, 0L) + 1);
                    }
                    // Count kmers
                    for (long kmer : kmerExtractor.extractKmers(sequenceRecord.sequence())) {
                        int bucket = encoder.getBucketNameFromKmer(kmer);
                        bucketSizes[bucket]++;

//                        if (bucket == 695) {
//                            kmerCounts.put(kmer, kmerCounts.getOrDefault(kmer, 0) + 1);
//                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not read input file.", e);
        }

//        // Print the kmer counts for bucket 695 in sorted order
//        kmerCounts.entrySet().stream()
//                .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
//                .forEach(entry -> System.out.println("Kmer: " + entry.getKey() + ", Count: " + entry.getValue()));

        // estimate the total number of kmers
        long bytesRead = sup.getBytesRead();
        long totalBytes = sup.getFileSize();
        for (int i = 0; i < bucketSizes.length; i++) {
            bucketSizes[i] = (int) (bucketSizes[i] * (totalBytes / bytesRead));
        }

        return new Pair<>(bucketSizes, charCounts);
    }

    private static int calculateMaxBucketSize(int[] bucketSizes) {
        int max = 0;
        for (int i = 0; i < bucketSizes.length; i++) {
            if (bucketSizes[i] > max) {
                max = bucketSizes[i];
            }
        }
        return max;
    }

    private static double[] calculateCharFrequencies(HashMap<Byte, Long> charCounts, Encoder encoder) {
        int total = 0;
        for (Long count : charCounts.values()) {
            total += count;
        }
        double[] frequencies = new double[encoder.getTargetAlphabet().getBase()];
        for (int i = 0; i < frequencies.length; i++) {
            frequencies[i] = (double) charCounts.getOrDefault((byte)i, 0L) / total;
        }
        return frequencies;
    }

    public int getSuggestedNumberOfBuckets() {
        Runtime runtime = Runtime.getRuntime();
        long freeMemory = (runtime.maxMemory() - runtime.totalMemory()) + runtime.freeMemory();
        freeMemory -= (long) (freeMemory * 0.2); // 20 % for the JVM
        // each bucket entry = 8 byte (kmer + id) (in practice its more 12 byte for some reason)
        long memoryPerBucket = 12 * (long)maxBucketSize;
        int numberOfBuckets = (int) (freeMemory / memoryPerBucket);
        // round down to next power of 2
//        return (int) Math.pow(2, Math.floor(Math.log(numberOfBuckets) / Math.log(2)));
        // round down to the next divisor of 1024
        return Math.ceilDiv(1024, Math.ceilDiv(1024, numberOfBuckets));
//        return numberOfBuckets;
    }

    public int[] getEstimatedBucketSizes() {
        return estimatedBucketSizes;
    }

    public int getMaxBucketSize() {
        return maxBucketSize;
    }

    public HashMap<Byte, Long> getCharCounts() {
        return charCounts;
    }

    public double[] getCharFrequencies() {
        return charFrequencies;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nStatistics on the first ").append(sampleSize)
                .append(" sequences of the input file: ").append(file).append("\n");
        sb.append("Character counts:\n");
        for (int i = 0; i < charFrequencies.length; i++) {
            sb.append(i).append(": ").append(charCounts.getOrDefault((byte) i, 0L)).append("\n");
        }
        sb.append("Character frequencies:\n");
        for (int i = 0; i < charFrequencies.length; i++) {
            sb.append(i).append(": ").append(charFrequencies[i]).append("\n");
        }
        sb.append("Max estimated bucket size: ").append(maxBucketSize).append("\n");
        sb.append("Estimated bucket sizes:\n");
        for (int i = 0; i < estimatedBucketSizes.length; i++) {
            sb.append(i).append(": ").append(estimatedBucketSizes[i]).append("\n");
        }
        return sb.toString();
    }
}
