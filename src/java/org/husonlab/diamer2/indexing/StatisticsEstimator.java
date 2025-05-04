package org.husonlab.diamer2.indexing;

import org.husonlab.diamer2.indexing.kmers.KmerExtractor;
import org.husonlab.diamer2.io.seq.FutureSequenceRecords;
import org.husonlab.diamer2.io.seq.SequenceSupplier;
import org.husonlab.diamer2.main.encoders.Encoder;
import org.husonlab.diamer2.seq.SequenceRecord;

import java.io.IOException;

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
            bucketSizes[i] = (int) ((bucketSizes[i] * totalBytes) / bytesRead);
        }
        return bucketSizes;
    }

    public static int[] countKmers(SequenceSupplier<?, byte[]> sup, Encoder encoder, int numberOfSequences) {
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
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not read input file.", e);
        }
        return bucketSizes;
    }
}
