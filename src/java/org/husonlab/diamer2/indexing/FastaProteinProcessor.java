package org.husonlab.diamer2.indexing;

import org.husonlab.diamer2.seq.KmerExtractor;
import org.husonlab.diamer2.seq.KmerExtractorProtein;
import org.husonlab.diamer2.seq.alphabet.ReducedProteinAlphabet;
import org.husonlab.diamer2.taxonomy.Tree;
import org.husonlab.diamer2.seq.Sequence;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Phaser;

public class FastaProteinProcessor implements Runnable {
    private final Phaser phaser;
    private final Sequence[] sequences;
    private final KmerExtractor kmerExtractor;
    private final ConcurrentHashMap<Long, Integer>[] bucketMaps;
    private final Tree tree;
    private final int rangeStart;
    private final int rangeEnd;

    /**
     * Processes a batch of Sequence sequences and adds the kmers to the corresponding bucket maps.
     * @param sequences Array of Sequence sequences to process.
     * @param bucketMaps Array of ConcurrentHashMaps to store the kmers.
     * @param tree Tree to find the LCA of two taxIds.
     */
    public FastaProteinProcessor(Phaser phaser, Sequence[] sequences, long mask, ReducedProteinAlphabet alphabet, ConcurrentHashMap<Long, Integer>[] bucketMaps, Tree tree, int rangeStart, int rangeEnd) {
        this.phaser = phaser;
        this.sequences = sequences;
        this.kmerExtractor = new KmerExtractorProtein(mask, alphabet);
        this.bucketMaps = bucketMaps;
        this.tree = tree;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
    }

    @Override
    public void run() {
        try {
            for (Sequence fasta : sequences) {
                if (fasta == null || fasta.getSequence().isEmpty() || fasta.getSequence().length() < kmerExtractor.getK()) {
                    continue;
                }
                String sequence = fasta.getSequence();
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