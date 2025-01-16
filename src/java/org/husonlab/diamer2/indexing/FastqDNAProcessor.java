package org.husonlab.diamer2.indexing;

import org.husonlab.diamer2.seq.kmers.KmerExtractor;
import org.husonlab.diamer2.seq.SequenceRecord;
import org.husonlab.diamer2.seq.kmers.KmerExtractorDNA;
import org.husonlab.diamer2.seq.alphabet.ReducedProteinAlphabet;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Phaser;

public class FastqDNAProcessor implements Runnable {

    private final Phaser phaser;
    private final SequenceRecord[] batch;
    private final KmerExtractor KmerExtractor;
    private final ConcurrentLinkedQueue<Long>[] bucketLists;
    private final int rangeStart;
    private final int rangeEnd;
    private int index;

    public FastqDNAProcessor(
            Phaser phaser,
            SequenceRecord[] batch,
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
            for (SequenceRecord fastq : batch) {
                if (fastq == null || fastq.getSequence().length() < (15*3)) {
                    continue;
                }
                String sequence = fastq.getSequenceString();
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