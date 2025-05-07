package org.husonlab.diamer2.indexing.kmers;

import java.util.Arrays;

public class KmerExtractorComplexityMaximizer extends KmerExtractor {

    private final int windowSize;
    private final long[] windowKmers;
    private final int[] windowComplexities;
    private int windowMaximizerComplexity;
    private int windowMaximizerIndex;

    /**
     * Creates a new KmerExtractor with the given encoder.
     * @param kmerEncoder the encoder to use
     */
    public KmerExtractorComplexityMaximizer(KmerEncoder kmerEncoder, int windowSize) {
        super(kmerEncoder);
        this.windowSize = windowSize;
        this.windowKmers = new long[windowSize - k + 1];
        this.windowComplexities = new int[windowSize - k + 1];
        this.windowMaximizerComplexity = 0;
        this.windowMaximizerIndex = 0;
    }

    @Override
    public long[] extractKmers(byte[] sequence) {
        int seqLength = sequence.length;
        if (seqLength < windowSize) {
            return new long[0];
        }
        kmerEncoder.reset();
        windowMaximizerIndex = 0;
        windowMaximizerComplexity = Integer.MAX_VALUE;
        // add the first k-1 characters to the encoder
        for (int i = 0; i < k - 1; i++) {
            kmerEncoder.addBack(sequence[i]);
        }

        long kmerEncoding;
        int kmerComplexity;

        // fill first maximizer window
        for (int i = k - 1; i < windowSize - 1; i++) {
            kmerEncoding = kmerEncoder.addBack(sequence[i]);
            kmerComplexity = kmerEncoder.getComplexity();
            windowKmers[i - k + 2] = kmerEncoding; // fill positions 1 to window - k + 1 (0 stays empty)
            windowComplexities[i - k + 2] = kmerComplexity;
        }

        // add the remaining characters to the encoder and store the resulting encoding
        long[] kmers = new long[seqLength - windowSize + 1]; // maximal number of maximizers that can be extracted
        int kmersIndex = 0; // position in the kmers array
        for (int i = windowSize - 1; i < seqLength; i++) {
            kmerEncoding = kmerEncoder.addBack(sequence[i]);
            kmerComplexity = kmerEncoder.getComplexity();
            // shift all entries to the left and loose the first entry (index 0)
            System.arraycopy(windowKmers, 1, windowKmers, 0, windowKmers.length - 1);
            // replace with the new kmer
            windowKmers[windowKmers.length - 1] = kmerEncoding;
            System.arraycopy(windowComplexities, 1, windowComplexities, 0, windowComplexities.length - 1);
            windowComplexities[windowComplexities.length - 1] = kmerComplexity;
            windowMaximizerIndex--;
            // case current kmer is the new maximizer
            if (kmerComplexity > windowMaximizerComplexity) {
                windowMaximizerComplexity = kmerComplexity;
                windowMaximizerIndex = windowKmers.length - 1;
            }
            // case old maximizer is out of the window (will be triggered for the first iteration)
            if (windowMaximizerIndex < 0) {
                findComplexityMaximizerIndex();
                windowMaximizerComplexity = windowComplexities[windowMaximizerIndex];
            }
            // add maximizer if it is the first or different from the last one.
            // in a constructed dataset, it might be possible that only one maximizer is extracted independent of the
            // input sequence length, but in real data this should not happen
            if (kmersIndex == 0 || windowKmers[windowMaximizerIndex] != kmers[kmersIndex - 1]) {
                kmers[kmersIndex++] = windowKmers[windowMaximizerIndex];
            }
        }
        return Arrays.copyOf(kmers, kmersIndex);
    }

    /**
     * Finds the highest index of the most complex kmer in the window.
     * Using the highest index ensures that the kmer stays in the window as long as possible.
     */
    private void findComplexityMaximizerIndex() {
        windowMaximizerComplexity = 0;
        for (int i = 0; i < windowKmers.length; i++) {
            if (windowComplexities[i] >= windowMaximizerComplexity) {
                windowMaximizerComplexity = windowComplexities[i];
                windowMaximizerIndex = i;
            }
        }
    }
}
