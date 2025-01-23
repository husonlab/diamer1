package org.husonlab.diamer2.indexing;

import org.husonlab.diamer2.seq.Sequence;
import org.husonlab.diamer2.seq.SequenceRecord;
import org.husonlab.diamer2.main.encodingSettings.EncodingSettings;
import org.husonlab.diamer2.indexing.kmers.KmerEncoder;
import org.husonlab.diamer2.indexing.kmers.KmerExtractor;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Phaser;

public class FastqDNAProcessor implements Runnable {

    private final Phaser phaser;
    private final SequenceRecord<Integer, Byte>[] batch;
    private final EncodingSettings encodingSettings;
    private final KmerExtractor kmerExtractor;
    private final ConcurrentLinkedQueue<Long>[] bucketLists;
    private final int rangeStart;
    private final int rangeEnd;

    public FastqDNAProcessor(
            Phaser phaser,
            SequenceRecord<Integer, Byte>[] batch,
            EncodingSettings encodingSettings,
            ConcurrentLinkedQueue<Long>[] bucketLists,
            int rangeStart,
            int rangeEnd) {
        this.phaser = phaser;
        this.batch = batch;
        this.encodingSettings = encodingSettings;
        this.kmerExtractor = new KmerExtractor(new KmerEncoder(encodingSettings.getTargetAlphabet().getBase(), encodingSettings.getMask()));
        this.bucketLists = bucketLists;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
    }

    @Override
    public void run() {
        try {
            for (SequenceRecord<Integer, Byte> fastq : batch) {
                if (fastq == null || fastq.sequence().length() < (15*3)) {
                    continue;
                }
                int id = fastq.id();
                Sequence<Byte> sequence = fastq.sequence();
                long[] kmers = kmerExtractor.extractKmers(sequence);
                for (long kmer : kmers) {
                    int bucketName = encodingSettings.getBucketNameFromKmer(kmer);
                    if (bucketName >= rangeStart && bucketName < rangeEnd) {
                        bucketLists[bucketName - rangeStart].add(encodingSettings.getIndex(id, kmer));
                    }
                }
            }
        } finally {
            phaser.arriveAndDeregister();
        }
    }
}