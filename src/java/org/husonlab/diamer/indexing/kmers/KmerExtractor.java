package org.husonlab.diamer.indexing.kmers;

import java.util.Arrays;

/**
 * Class to extract and encode of kmers from a sequence.
 */
public class KmerExtractor {
    protected final KmerEncoder kmerEncoder;
    protected final int k;

    /**
     * Creates a new KmerExtractor with the given encoder.
     *
     * @param kmerEncoder the encoder to use
     */
    public KmerExtractor(KmerEncoder kmerEncoder) {
        this.kmerEncoder = kmerEncoder;
        this.k = this.kmerEncoder.getK();
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
            kmerEncoder.add(sequence[i]);
        }
        // add the remaining characters to the encoder and store the resulting encoding
        long kmerEncoding;
        long[] kmers = new long[seqLength - k + 1];
        int kmersIndex = 0;
        for (int i = k - 1; i < seqLength; i++) {
            kmerEncoding = kmerEncoder.add(sequence[i]);
            kmers[kmersIndex++] = kmerEncoding;
        }
        return Arrays.copyOf(kmers, kmersIndex);
    }
}