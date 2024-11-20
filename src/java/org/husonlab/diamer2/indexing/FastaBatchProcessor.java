package org.husonlab.diamer2.indexing;

import org.husonlab.diamer2.graph.Tree;
import org.husonlab.diamer2.seq.FASTA;

import java.util.concurrent.ConcurrentHashMap;

import static org.husonlab.diamer2.alphabet.AAEncoder.toBase11AndNumber;

public class FastaBatchProcessor implements Runnable {
    private final FASTA[] fastas;
    private final ConcurrentHashMap<Long, Integer>[] bucketMaps;
    private final Tree tree;
    private final int[] currentBucketRange;

    /**
     * Processes a batch of FASTA sequences and adds the kmers to the corresponding bucket maps.
     * @param fastas Array of FASTA sequences to process.
     * @param bucketMaps Array of ConcurrentHashMaps to store the kmers.
     * @param tree Tree to find the LCA of two taxIds.
     * @param currentBucketRange Range of buckets that corresponds to the provided bucketMaps.
     */
    public FastaBatchProcessor(FASTA[] fastas, ConcurrentHashMap<Long, Integer>[] bucketMaps, Tree tree, int[] currentBucketRange) {
        this.fastas = fastas;
        this.bucketMaps = bucketMaps;
        this.tree = tree;
        this.currentBucketRange = currentBucketRange;
    }

    @Override
    public void run() {
        for (FASTA fasta : fastas) {
            if (fasta == null) {
                continue;
            }
            String sequence = fasta.getSequence();
            int taxId = Integer.parseInt(fasta.getHeader().split(" ")[0].substring(1));
            for (int i = 0; i + 15 <= sequence.length(); i++) {
                String kmer = sequence.substring(i, i + 15);
                long kmerEnc = toBase11AndNumber(kmer);
                int bucketId = (int) (kmerEnc & 0b1111111111);
                if (bucketId < currentBucketRange[1] - currentBucketRange[0]) {
                    bucketMaps[bucketId].computeIfAbsent(kmerEnc, k -> taxId);
                    bucketMaps[bucketId].computeIfPresent(kmerEnc, (k, v) -> tree.findLCA(v, taxId));
                }
            }
        }
    }
}