package org.husonlab.diamer.indexing.kmers;

import java.util.Arrays;

public class KmerExtractorMinimizer extends KmerExtractor {

    private final int windowSize;
    private final long[] windowKmers;
    private final long[] windowHashes;
    private long windowMinimizerHash;
    private int windowMinimizerIndex;

    /**
     * Creates a new KmerExtractor with the given encoder.
     * @param kmerEncoder the encoder to use
     */
    public KmerExtractorMinimizer(KmerEncoder kmerEncoder, int windowSize) {
        super(kmerEncoder);
        this.windowSize = windowSize;
        this.windowKmers = new long[windowSize - k + 1];
        this.windowHashes = new long[windowSize - k + 1];
        this.windowMinimizerHash = Long.MAX_VALUE;
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
        windowMinimizerHash = Long.MAX_VALUE;
        // add the first k-1 characters to the encoder
        for (int i = 0; i < k - 1; i++) {
            kmerEncoder.add(sequence[i]);
        }

        long kmerEncoding;
        long kmerHash;

        // fill first minimizer window
        for (int i = k - 1; i < windowSize - 1; i++) {
            kmerEncoding = kmerEncoder.add(sequence[i]);
            windowKmers[i - k + 2] = kmerEncoding;
            windowHashes[i - k + 2] = hashFunction(kmerEncoding);
        }

        // add the remaining characters to the encoder and store the resulting encoding
        long[] kmers = new long[seqLength - windowSize + 1];
        int kmersIndex = 0;
        for (int i = windowSize - 1; i < seqLength; i++) {
            kmerEncoding = kmerEncoder.add(sequence[i]);
            kmerHash = hashFunction(kmerEncoding);
            System.arraycopy(windowKmers, 1, windowKmers, 0, windowKmers.length - 1);
            windowKmers[windowKmers.length - 1] = kmerEncoding;
            System.arraycopy(windowHashes, 1, windowHashes, 0, windowHashes.length - 1);
            windowHashes[windowHashes.length - 1] = kmerHash;
            windowMinimizerIndex--;
            if (kmerHash < windowMinimizerHash) {
                windowMinimizerHash = kmerHash;
                windowMinimizerIndex = windowKmers.length - 1;
            }
            if (windowMinimizerIndex < 0) {
                findMinimizer();
                windowMinimizerHash = windowHashes[windowMinimizerIndex];
            }
            if (kmersIndex == 0 || windowKmers[windowMinimizerIndex] != kmers[kmersIndex - 1]) {
                kmers[kmersIndex++] = windowKmers[windowMinimizerIndex];
            }
        }
        return Arrays.copyOf(kmers, kmersIndex);
    }

    private static int hashFunction(long value) {
        value = (value ^ (value >>> 33)) * 0xff51afd7ed558ccdL;
        value = (value ^ (value >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return (int) (value ^ (value >>> 33));
    }

    private void findMinimizer() {
        windowMinimizerHash = Long.MAX_VALUE;
        for (int i = 0; i < windowHashes.length; i++) {
            if (windowHashes[i] <= windowMinimizerHash) {
                windowMinimizerHash = windowHashes[i];
                windowMinimizerIndex = i;
            }
        }
    }
}
