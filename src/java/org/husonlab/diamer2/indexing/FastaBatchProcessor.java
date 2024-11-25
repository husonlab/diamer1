package org.husonlab.diamer2.indexing;

import org.husonlab.diamer2.alphabet.AAEncoder;
import org.husonlab.diamer2.alphabet.AAKmerEncoder;
import org.husonlab.diamer2.graph.Tree;
import org.husonlab.diamer2.seq.Sequence;

import java.util.concurrent.ConcurrentHashMap;

import static org.husonlab.diamer2.alphabet.AAEncoder.toBase11;

public class FastaBatchProcessor implements Runnable {
    private final Sequence[] sequences;
    private final ConcurrentHashMap<Long, Integer>[] bucketMaps;
    private final Tree tree;
    private final int[] currentBucketRange;

    /**
     * Processes a batch of Sequence sequences and adds the kmers to the corresponding bucket maps.
     * @param sequences Array of Sequence sequences to process.
     * @param bucketMaps Array of ConcurrentHashMaps to store the kmers.
     * @param tree Tree to find the LCA of two taxIds.
     * @param currentBucketRange Range of buckets that corresponds to the provided bucketMaps.
     */
    public FastaBatchProcessor(Sequence[] sequences, ConcurrentHashMap<Long, Integer>[] bucketMaps, Tree tree, int[] currentBucketRange) {
        this.sequences = sequences;
        this.bucketMaps = bucketMaps;
        this.tree = tree;
        this.currentBucketRange = currentBucketRange;
    }

    @Override
    public void run() {
        final AAKmerEncoder encoder = new AAKmerEncoder(15, 11);
        for (Sequence fasta : sequences) {
            if (fasta == null || fasta.getSequence().isEmpty() || fasta.getSequence().length() < 15) {
                continue;
            }
            String sequence = fasta.getSequence();
            int taxId = Integer.parseInt(fasta.getHeader().split(" ")[0]);
            for (int i = 0; i < sequence.length(); i++) {
                if (i < 14) {
                    encoder.addBack(AAEncoder.toBase11(sequence.charAt(i)));
                } else {
                    long kmerEnc =  encoder.addBack(AAEncoder.toBase11(sequence.charAt(i)));
                    int bucketId = (int) (kmerEnc & 0b1111111111);
                    if (bucketId >= currentBucketRange[0] && bucketId < currentBucketRange[1]) {
                        bucketMaps[bucketId - currentBucketRange[0]].computeIfPresent(kmerEnc, (k, v) -> tree.findLCA(v, taxId));
                        bucketMaps[bucketId - currentBucketRange[0]].computeIfAbsent(kmerEnc, k -> taxId);
                    }
                }
            }
        }
    }
}