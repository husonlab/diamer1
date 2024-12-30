package org.husonlab.diamer2.indexing;

import org.husonlab.diamer2.seq.KmerExtractor;
import org.husonlab.diamer2.seq.Sequence;
import org.husonlab.diamer2.seq.KmerExtractorDNA;
import org.husonlab.diamer2.seq.alphabet.ReducedProteinAlphabet;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Phaser;

public class FastqDNAProcessor implements Runnable {

    private final Phaser phaser;
    private final Sequence[] batch;
    private final KmerExtractor KmerExtractor;
    private final ConcurrentLinkedQueue<Long>[] bucketLists;
    private final int rangeStart;
    private final int rangeEnd;
    private int index;

    public FastqDNAProcessor(
            Phaser phaser,
            Sequence[] batch,
            long mask,
            ReducedProteinAlphabet alphabet,
            ConcurrentLinkedQueue<Long>[] bucketLists,
            int rangeStart,
            int rangeEnd,
            int startIndex) {
        this.phaser = phaser;
        this.batch = batch;
        this.KmerExtractor = new KmerExtractorDNA(mask, alphabet);
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
                long[] kmers = KmerExtractor.extractKmers(sequence);
                for (long kmer : kmers) {
                    int bucketName = IndexEncoding.getBucketName(kmer);
                    if (bucketName >= rangeStart && bucketName < rangeEnd) {
                        bucketLists[bucketName - rangeStart].add(IndexEncoding.getIndexEntry(kmer, index));
                    }
                }
                index++;
            }
        } finally {
            phaser.arriveAndDeregister();
        }
    }
}