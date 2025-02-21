package org.husonlab.diamer2.indexing;

import org.husonlab.diamer2.io.seq.FutureSequenceRecords;
import org.husonlab.diamer2.seq.Sequence;
import org.husonlab.diamer2.seq.SequenceRecord;
import org.husonlab.diamer2.main.encoders.Encoder;
import org.husonlab.diamer2.indexing.kmers.KmerEncoder;
import org.husonlab.diamer2.indexing.kmers.KmerExtractor;
import org.husonlab.diamer2.taxonomy.Tree;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Phaser;

public class FastaProteinProcessor implements Runnable {
    private final Phaser phaser;
    private final FutureSequenceRecords<Integer, Byte>[] containers;
    private final KmerExtractor kmerExtractor;
    private final ConcurrentHashMap<Long, Integer>[] bucketMaps;
    private final Tree tree;
    private final int rangeStart;
    private final int rangeEnd;
    private final Encoder encoder;

    /**
     * Processes a batch of Sequence sequences and adds the kmers to the corresponding bucket maps.
     * @param containers Array of Sequence sequences to process.
     * @param bucketMaps Array of ConcurrentHashMaps to store the kmers.
     * @param tree Tree to find the LCA of two taxIds.
     */
    public FastaProteinProcessor(Phaser phaser, FutureSequenceRecords<Integer, Byte>[] containers, Encoder encoder, ConcurrentHashMap<Long, Integer>[] bucketMaps, Tree tree, int rangeStart, int rangeEnd) {
        this.phaser = phaser;
        this.containers = containers;
        this.kmerExtractor = new KmerExtractor(new KmerEncoder(encoder.getTargetAlphabet().getBase(), encoder.getMask()));
        this.bucketMaps = bucketMaps;
        this.tree = tree;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
        this.encoder = encoder;
    }

    @Override
    public void run() {
        try {
            for (FutureSequenceRecords<Integer, Byte> container : containers) {
                for (SequenceRecord<Integer, Byte> record: container.getSequenceRecords()) {
                    if (record == null || record.sequence().length() < kmerExtractor.getK() || !tree.hasNode(record.id())) {
                        continue;
                    }
                    Sequence<Byte> sequence = record.sequence();
                    int taxId = record.id();
                    long[] kmers = kmerExtractor.extractKmers(sequence);
                    for (long kmerEnc : kmers) {
                        int bucketName = encoder.getBucketNameFromKmer(kmerEnc);
                        if (bucketName >= rangeStart && bucketName < rangeEnd) {
                            bucketMaps[bucketName - rangeStart].computeIfPresent(kmerEnc, (k, v) -> tree.findLCA(v, taxId));
                            bucketMaps[bucketName - rangeStart].computeIfAbsent(kmerEnc, k -> taxId);
                        }
                    }
                }
            }
        } finally {
            phaser.arriveAndDeregister();
        }
    }
}