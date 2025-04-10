package org.husonlab.diamer2.indexing.kmers;

import java.util.Arrays;

public class KmerExtractorFiltered extends KmerExtractor {

    private final Filter filter;

    /**
     * Creates a new KmerExtractor with the given encoder.
     *
     * @param kmerEncoder the encoder to use
     * @param filter
     */
    public KmerExtractorFiltered(KmerEncoder kmerEncoder, Filter filter) {
        super(kmerEncoder);
        this.filter = filter;
    }

    @Override
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

    public interface Filter {
        boolean keep(long kmer);
    }
}
