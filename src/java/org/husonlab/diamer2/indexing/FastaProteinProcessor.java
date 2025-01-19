package org.husonlab.diamer2.indexing;

import org.husonlab.diamer2.seq.Sequence;
import org.husonlab.diamer2.seq.encoder.Encoder;
import org.husonlab.diamer2.seq.kmers.KmerExtractor;
import org.husonlab.diamer2.seq.kmers.KmerExtractorProtein;
import org.husonlab.diamer2.seq.SequenceRecord;
import org.husonlab.diamer2.seq.alphabet.ReducedProteinAlphabet;
import org.husonlab.diamer2.taxonomy.Tree;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Phaser;

public class FastaProteinProcessor implements Runnable {
    private final Phaser phaser;
    private final SequenceRecord<Short>[] sequenceRecords;
    private final KmerExtractor<Short> kmerExtractor;
    private final ConcurrentHashMap<Long, Integer>[] bucketMaps;
    private final Tree tree;
    private final int rangeStart;
    private final int rangeEnd;

    /**
     * Processes a batch of Sequence sequences and adds the kmers to the corresponding bucket maps.
     * @param sequenceRecords Array of Sequence sequences to process.
     * @param bucketMaps Array of ConcurrentHashMaps to store the kmers.
     * @param tree Tree to find the LCA of two taxIds.
     */
    public FastaProteinProcessor(Phaser phaser, SequenceRecord<Short>[] sequenceRecords, Encoder encoder, ConcurrentHashMap<Long, Integer>[] bucketMaps, Tree tree, int rangeStart, int rangeEnd) {
        this.phaser = phaser;
        this.sequenceRecords = sequenceRecords;
        this.kmerExtractor = new KmerExtractorProtein(encoder);
        this.bucketMaps = bucketMaps;
        this.tree = tree;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
    }

    @Override
    public void run() {
        try {
            for (SequenceRecord<Short> fasta : sequenceRecords) {
                if (fasta == null || fasta.getSequence().length() < kmerExtractor.getK()) {
                    continue;
                }
                Sequence<Short> sequence = fasta.getSequence();
                int taxId = Integer.parseInt(fasta.getHeader().substring(1).split(" ")[0]);
                long[] kmers = kmerExtractor.extractKmers(sequence);
                for (long kmerEnc : kmers) {
                    int bucketName = IndexEncoding.getBucketName(kmerEnc);
                    if (bucketName >= rangeStart && bucketName < rangeEnd) {
                        bucketMaps[bucketName - rangeStart].computeIfPresent(kmerEnc, (k, v) -> tree.findLCA(v, taxId));
                        bucketMaps[bucketName - rangeStart].computeIfAbsent(kmerEnc, k -> taxId);
                    }
                }
            }
        } finally {
            phaser.arriveAndDeregister();
        }
    }
}