package org.husonlab.diamer2.indexing;

import org.husonlab.diamer2.seq.Sequence;
import org.husonlab.diamer2.seq.SequenceRecord;
import org.husonlab.diamer2.seq.encoder.Encoder;
import org.husonlab.diamer2.seq.kmers.KmerExtractor;
import org.husonlab.diamer2.seq.HeaderSequenceRecord;
import org.husonlab.diamer2.seq.kmers.KmerExtractorProtein;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Phaser;

public class FastqDNAProcessor implements Runnable {

    private final Phaser phaser;
    private final SequenceRecord<Integer, Short>[] batch;
    private final Encoder encoder;
    private final KmerExtractor<Short> KmerExtractor;
    private final ConcurrentLinkedQueue<Long>[] bucketLists;
    private final int rangeStart;
    private final int rangeEnd;

    public FastqDNAProcessor(
            Phaser phaser,
            SequenceRecord<Integer, Short>[] batch,
            Encoder encoder,
            ConcurrentLinkedQueue<Long>[] bucketLists,
            int rangeStart,
            int rangeEnd) {
        this.phaser = phaser;
        this.batch = batch;
        this.encoder = encoder;
        this.KmerExtractor = new KmerExtractorProtein(encoder);
        this.bucketLists = bucketLists;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
    }

    @Override
    public void run() {
        try {
            for (SequenceRecord<Integer, Short> fastq : batch) {
                if (fastq == null || fastq.getSequence().length() < (15*3)) {
                    continue;
                }
                int id = fastq.getId();
                Sequence<Short> sequence = fastq.getSequence();
                long[] kmers = KmerExtractor.extractKmers(sequence);
                for (long kmer : kmers) {
                    int bucketName = encoder.getBucketName(kmer);
                    if (bucketName >= rangeStart && bucketName < rangeEnd) {
                        bucketLists[bucketName - rangeStart].add(encoder.getIndex(id, kmer));
                    }
                }
            }
        } finally {
            phaser.arriveAndDeregister();
        }
    }
}