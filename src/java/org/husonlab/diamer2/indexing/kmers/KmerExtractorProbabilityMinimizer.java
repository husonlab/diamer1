package org.husonlab.diamer2.indexing.kmers;

import java.util.Arrays;

public class KmerExtractorProbabilityMinimizer extends KmerExtractor {

    private final int windowSize;
    private final long[] windowKmers;
    private final double[] windowProbabilities;
    private double windowMinimizerProbability;
    private int windowMinimizerIndex;

    /**
     * Creates a new KmerExtractor with the given encoder.
     *
     * @param kmerEncoder the encoder to use
     */
    public KmerExtractorProbabilityMinimizer(KmerEncoder kmerEncoder, int windowSize) {
        super(kmerEncoder);
        this.windowSize = windowSize;
        this.windowKmers = new long[windowSize - k + 1];
        this.windowProbabilities = new double[windowSize - k + 1];
        this.windowMinimizerProbability = Double.MAX_VALUE;
        this.windowMinimizerIndex = 0;
    }

    /**
     * Extracts the minimizers from the given sequence.
     */
    @Override
    public long[] extractKmers(byte[] sequence) {
        int seqLength = sequence.length;
        if (seqLength < windowSize) {
            return new long[0];
        }
        kmerEncoder.reset();
        windowMinimizerIndex = 0;
        windowMinimizerProbability = 0;
        // add the first k-1 characters to the encoder
        for (int i = 0; i < k - 1; i++) {
            kmerEncoder.addBack(sequence[i]);
        }

        long kmerEncoding;
        double kmerProbability;

        // fill first minimizer window
        for (int i = k - 1; i < windowSize - 1; i++) {
            kmerEncoding = kmerEncoder.addBack(sequence[i]);
            kmerProbability = kmerEncoder.getProbability();
            windowKmers[i - k + 2] = kmerEncoding;
            windowProbabilities[i - k + 2] = kmerProbability;
        }

        // add the remaining characters to the encoder and store the resulting encoding
        long[] kmers = new long[seqLength - windowSize + 1];
        int kmersIndex = 0;
        for (int i = windowSize - 1; i < seqLength; i++) {
            kmerEncoding = kmerEncoder.addBack(sequence[i]);
            kmerProbability = kmerEncoder.getProbability();
            System.arraycopy(windowKmers, 1, windowKmers, 0, windowKmers.length - 1);
            windowKmers[windowKmers.length - 1] = kmerEncoding;
            System.arraycopy(windowProbabilities, 1, windowProbabilities, 0, windowProbabilities.length - 1);
            windowProbabilities[windowProbabilities.length - 1] = kmerProbability;
            windowMinimizerIndex--;
            if (kmerProbability < windowMinimizerProbability) {
                windowMinimizerProbability = kmerProbability;
                windowMinimizerIndex = windowKmers.length - 1;
            }
            if (windowMinimizerIndex < 0) {
                findProbabilityMinimizerIndex();
                windowMinimizerProbability = windowProbabilities[windowMinimizerIndex];
            }
            if (kmersIndex == 0 || windowKmers[windowMinimizerIndex] != kmers[kmersIndex - 1]) {
                kmers[kmersIndex++] = windowKmers[windowMinimizerIndex];
            }
        }
        return Arrays.copyOf(kmers, kmersIndex);
    }

    private void findProbabilityMinimizerIndex() {
        windowMinimizerProbability = Double.MAX_VALUE;
        for (int i = 0; i < windowKmers.length; i++) {
            if (windowProbabilities[i] <= windowMinimizerProbability) {
                windowMinimizerProbability = windowProbabilities[i];
                windowMinimizerIndex = i;
            }
        }
    }
}
