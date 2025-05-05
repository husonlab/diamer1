package org.husonlab.diamer2.indexing;

import org.husonlab.diamer2.indexing.kmers.KmerExtractor;
import org.husonlab.diamer2.io.seq.FutureSequenceRecords;
import org.husonlab.diamer2.io.seq.SequenceSupplier;
import org.husonlab.diamer2.main.encoders.Encoder;
import org.husonlab.diamer2.seq.SequenceRecord;

import java.io.IOException;
import java.util.HashMap;

/**
 * Class to estimate some statistics and required memory.
 */
public class StatisticsEstimator {

    public static int estimateMaxBucketSize(SequenceSupplier<?, byte[]> sup, Encoder encoder, int numberOfSequences) {
        int bucketSizes[] = estimateBucketSizes(sup, encoder, numberOfSequences);
        int max = 0;
        for (int i = 0; i < bucketSizes.length; i++) {
            if (bucketSizes[i] > max) {
                max = bucketSizes[i];
            }
        }
        return max;
    }

    public static int[] estimateBucketSizes(SequenceSupplier<?, byte[]> sup, Encoder encoder, int numberOfSequences) {
        int bucketSizes[] = countKmers(sup, encoder, numberOfSequences);
        long bytesRead = sup.getBytesRead();
        long totalBytes = sup.getFileSize();
        for (int i = 0; i < bucketSizes.length; i++) {
            bucketSizes[i] = (int) (bucketSizes[i] * (totalBytes / bytesRead));
        }
        return bucketSizes;
    }

    public static int[] countKmers(SequenceSupplier<?, byte[]> sup, Encoder encoder, int numberOfSequences) {
//        HashMap<Long, Integer> kmerCounts = new HashMap<>();

        int[] bucketSizes = new int[encoder.getNrOfBuckets()];
        KmerExtractor kmerExtractor = encoder.getKmerExtractor();
        FutureSequenceRecords<?, byte[]> sequenceRecords;
        int n = 0;
        try {
            while ((sequenceRecords = sup.next()) != null && n++ <= numberOfSequences) {
                for (SequenceRecord<?, byte[]> sequenceRecord : sequenceRecords.getSequenceRecords()) {
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

        return bucketSizes;
    }

    public static int suggestNumberOfBucketsReads(int expectedBucketSize) {
        Runtime runtime = Runtime.getRuntime();
        long freeMemory = (runtime.maxMemory() - runtime.totalMemory()) + runtime.freeMemory();
        freeMemory -= (long) (freeMemory * 0.2); // 20 % for the JVM
        // each bucket entry = 8 byte (kmer + id) (in practice its more 12 byte for some reason)
        long memoryPerBucket = 12 * (long)expectedBucketSize;
        int numberOfBuckets = (int) (freeMemory / memoryPerBucket);
        // round down to next power of 2
//        return (int) Math.pow(2, Math.floor(Math.log(numberOfBuckets) / Math.log(2)));
        // round down to the next divisor of 1024
        return Math.ceilDiv(1024, Math.ceilDiv(1024, numberOfBuckets));
//        return numberOfBuckets;
    }

    public static int suggestNumberOfBucketsDB(int expectedBucketSize) {
        Runtime runtime = Runtime.getRuntime();
        long freeMemory = (runtime.maxMemory() - runtime.totalMemory()) + runtime.freeMemory();
        freeMemory -= (long) (freeMemory * 0.2); // 20 % for the JVM
        // each bucket entry = 8 byte (kmer) + 4 byte (id)
        long memoryPerBucket = 12 * (long)expectedBucketSize;
        int numberOfBuckets = (int) (freeMemory / memoryPerBucket);
        // round down to next power of 2
//        return (int) Math.pow(2, Math.floor(Math.log(numberOfBuckets) / Math.log(2)));
        return numberOfBuckets;
    }
}
