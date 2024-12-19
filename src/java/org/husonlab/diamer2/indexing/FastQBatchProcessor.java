package org.husonlab.diamer2.indexing;

import org.husonlab.diamer2.seq.KmerExtractor;
import org.husonlab.diamer2.seq.alphabet.DNAKmerEncoder;
import org.husonlab.diamer2.seq.Sequence;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Phaser;

public class FastQBatchProcessor implements Runnable {

    private final Phaser phaser;
    private final Sequence[] batch;
    private final ConcurrentLinkedQueue<Long>[] bucketLists;
    private final int rangeStart;
    private final int rangeEnd;
    private int index;

    public FastQBatchProcessor(
            Phaser phaser,
            Sequence[] batch,
            ConcurrentLinkedQueue<Long>[] bucketLists,
            int rangeStart,
            int rangeEnd,
            int startIndex) {
        this.phaser = phaser;
        this.batch = batch;
        this.bucketLists = bucketLists;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
        this.index = startIndex;
    }

    @Override
    public void run() {
        try {
            for (Sequence fastq : batch) {
                if (fastq == null || fastq.getSequence().length() < (15*3)) {
                    continue;
                }
                String sequence = fastq.getSequence();
                DNAKmerEncoder kmerEncoder = new DNAKmerEncoder(15, sequence.substring(0, 2));

                KmerExtractor.extractKmersDNA(sequence, 15).forEach(kmer -> {
                    int bucketName1 = IndexEncoding.getBucketName(kmer[0]);
                    int bucketName2 = IndexEncoding.getBucketName(kmer[1]);
                    if (bucketName1 >= rangeStart && bucketName1 < rangeEnd) {
                        bucketLists[bucketName1 - rangeStart].add(IndexEncoding.getIndexEntry(kmer[0], index));
                    }
                    if (bucketName2 >= rangeStart && bucketName2 < rangeEnd) {
                        bucketLists[bucketName2 - rangeStart].add(IndexEncoding.getIndexEntry(kmer[1], index));
                    }
                });
                index++;
            }
        } finally {
            phaser.arriveAndDeregister();
        }
    }
}