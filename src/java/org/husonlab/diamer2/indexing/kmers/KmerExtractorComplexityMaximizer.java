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

        // fill first minimizer window
        for (int i = k - 1; i < windowSize - 1; i++) {
            kmerEncoding = kmerEncoder.addBack(sequence[i]);
            kmerComplexity = kmerEncoder.getComplexity();
            windowKmers[i - k + 2] = kmerEncoding;
            windowComplexities[i - k + 2] = kmerComplexity;
        }

        // add the remaining characters to the encoder and store the resulting encoding
        long[] kmers = new long[seqLength - windowSize + 1];
        int kmersIndex = 0;
        for (int i = windowSize - 1; i < seqLength; i++) {
            kmerEncoding = kmerEncoder.addBack(sequence[i]);
            kmerComplexity = kmerEncoder.getComplexity();
            System.arraycopy(windowKmers, 1, windowKmers, 0, windowKmers.length - 1);
            windowKmers[windowKmers.length - 1] = kmerEncoding;
            System.arraycopy(windowComplexities, 1, windowComplexities, 0, windowComplexities.length - 1);
            windowComplexities[windowComplexities.length - 1] = kmerComplexity;
            windowMaximizerIndex--;
            if (kmerComplexity > windowMaximizerComplexity) {
                windowMaximizerComplexity = kmerComplexity;
                windowMaximizerIndex = windowKmers.length - 1;
            }
            if (windowMaximizerIndex < 0) {
                findComplexityMaximizerIndex();
                windowMaximizerComplexity = windowComplexities[windowMaximizerIndex];
            }
            if (kmersIndex == 0 || windowKmers[windowMaximizerIndex] != kmers[kmersIndex - 1]) {
                kmers[kmersIndex++] = windowKmers[windowMaximizerIndex];
            }
        }
        return Arrays.copyOf(kmers, kmersIndex);
    }

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
