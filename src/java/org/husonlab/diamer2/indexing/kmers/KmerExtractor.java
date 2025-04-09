package org.husonlab.diamer2.indexing.kmers;

import java.util.Arrays;

/**
 * Class to extract and encode of kmers from a sequence.
 */
public class KmerExtractor {
    private final KmerEncoder kmerEncoder;
    private final int k;
    private final Filter filter;

    // for minimizers
    private final static int windowSize = 20;
    private final long[] windowKmers;
    private final double[] windowProbabilities;
    private int windowMinimizerIndex;
    private double windowMinimizerProbability;

    private final int[] windowComplexities;
    private int windowMinimizerComplexity;

    private final long[] windowHashes;
    private int windowHashMinimizer;

    /**
     * Creates a new KmerExtractor with the given encoder.
     *
     * @param kmerEncoder the encoder to use
     * @param filter
     */
    public KmerExtractor(KmerEncoder kmerEncoder, Filter filter) {
        this.kmerEncoder = kmerEncoder;
        this.k = this.kmerEncoder.getK();
        this.filter = filter;

        // for minimizers
        this.windowKmers = new long[windowSize - k + 1];
        this.windowProbabilities = new double[windowSize - k + 1];
        this.windowComplexities = new int[windowSize - k + 1];
        this.windowMinimizerIndex = 0;
        this.windowMinimizerProbability = 0;
        this.windowMinimizerComplexity = Integer.MAX_VALUE;
    }

    /**
     * @return the length of the mask (including spaces)
     */
    public long getK() {
        return k;
    }

    /**
     * @return the number of spaces between the bits of the mask
     */
    public long getS() {
        return kmerEncoder.getS();
    }

    /**
     * Extracts the kmers from the given sequence.
     * <p>Together with the provided {@link KmerEncoder} the mask will be shifted over the sequence and all kmers will
     * be extracted and converted to a number. The most significant bit mask position will correspond to the most
     * significant position in the kmer to number conversion.</p>
     * @param sequence the sequence to extract the kmers from
     * @return the extracted kmers
     */
    public long[] extractKmers(byte[] sequence) {
        int seqLength = sequence.length;
        if (seqLength < k) {
            return new long[0];
        }
        kmerEncoder.reset();
        // add the first k-1 characters to the encoder
        for (int i = 0; i < k - 1; i++) {
            kmerEncoder.addBack(sequence[i]);
        }
        // add the remaining characters to the encoder and store the resulting encoding
        long kmerEncoding;
        long[] kmers = new long[seqLength - k + 1];
        int kmersIndex = 0;
        for (int i = k - 1; i < seqLength; i++) {
            kmerEncoding = kmerEncoder.addBack(sequence[i]);
            if (filter.keep(kmerEncoding)) {
                kmers[kmersIndex++] = kmerEncoding;
            }
        }
        return Arrays.copyOf(kmers, kmersIndex);
    }

    /**
     * Extracts the minimizers from the given sequence.
     */
    public long[] extractProbabilityMinimizer(byte[] sequence) {
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

    public long[] extractComplexityMaximizer(byte[] sequence) {
        int seqLength = sequence.length;
        if (seqLength < windowSize) {
            return new long[0];
        }
        kmerEncoder.reset();
        windowMinimizerIndex = 0;
        windowMinimizerComplexity = Integer.MAX_VALUE;
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
            windowComplexities[windowProbabilities.length - 1] = kmerComplexity;
            windowMinimizerIndex--;
            if (kmerComplexity > windowMinimizerComplexity) {
                windowMinimizerComplexity = kmerComplexity;
                windowMinimizerIndex = windowKmers.length - 1;
            }
            if (windowMinimizerIndex < 0) {
                findComplexityMaximizerIndex();
                windowMinimizerComplexity = windowComplexities[windowMinimizerIndex];
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

    private void findComplexityMaximizerIndex() {
        windowMinimizerComplexity = 0;
        for (int i = 0; i < windowKmers.length; i++) {
            if (windowComplexities[i] >= windowMinimizerComplexity) {
                windowMinimizerComplexity = windowComplexities[i];
                windowMinimizerIndex = i;
            }
        }
    }

    public static int hashFunction(long value) {
        value = (value ^ (value >>> 33)) * 0xff51afd7ed558ccdL;
        value = (value ^ (value >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return (int) (value ^ (value >>> 33));
    }

    public interface Filter {
        boolean keep(long kmer);
    }
}