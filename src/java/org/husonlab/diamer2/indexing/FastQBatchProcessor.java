package org.husonlab.diamer2.indexing;

import org.husonlab.diamer2.alphabet.DNAKmerEncoder;
import org.husonlab.diamer2.seq.Sequence;
import org.husonlab.diamer2.util.Pair;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FastQBatchProcessor implements Runnable {

    private final Sequence[] batch;
    private final ConcurrentLinkedQueue<Long>[] bucketLists;
    private final int[] currentBucketRange;
    private int index;

    public FastQBatchProcessor(Sequence[] batch, ConcurrentLinkedQueue<Long>[] bucketLists, int[] currentBucketRange, int startIndex) {
        this.batch = batch;
        this.bucketLists = bucketLists;
        this.currentBucketRange = currentBucketRange;
        this.index = startIndex;
    }

    @Override
    public void run() {
        for (Sequence fastq : batch) {
            if (fastq == null || fastq.getSequence().length() < (15*3)) {
                continue;
            }
            index++;
            String sequence = fastq.getSequence();
            DNAKmerEncoder kmerEncoder = new DNAKmerEncoder(15, sequence.substring(0, 2));
            for (int i = 2; i < (15*3) - 1; i++) {
                kmerEncoder.addNucleotide(sequence.charAt(i));
            }
            for (int i = (15*3); i < sequence.length(); i++) {
                long[] encodedKmers = kmerEncoder.addNucleotide(sequence.charAt(i)).getEncodedKmers();
                int bucketId1 = (int) (encodedKmers[0] & 0b1111111111);
                int bucketId2 = (int) (encodedKmers[1] & 0b1111111111);
                if (bucketId1 < currentBucketRange[1] - currentBucketRange[0]) {
                    bucketLists[bucketId1].add(((encodedKmers[0] << 10) & 0xffffffffffc00000L ) | index);
                }
                if (bucketId2 < currentBucketRange[1] - currentBucketRange[0]) {
                    bucketLists[bucketId2].add(((encodedKmers[0] << 10) & 0xffffffffffc00000L ) | index);
                }
            }
        }
    }
}