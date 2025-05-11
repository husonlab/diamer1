package org.husonlab.diamer.indexing.kmers;

import java.util.Arrays;

public abstract class KmerExtractorFiltered extends KmerExtractor {
    public KmerExtractorFiltered(KmerEncoder kmerEncoder) {
        super(kmerEncoder);
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
            kmerEncoder.add(sequence[i]);
        }
        // add the remaining characters to the encoder and store the resulting encoding
        long kmerEncoding;
        long[] kmers = new long[seqLength - k + 1];
        int kmersIndex = 0;
        for (int i = k - 1; i < seqLength; i++) {
            kmerEncoding = kmerEncoder.add(sequence[i]);
            if (keep(kmerEncoding)) {
                kmers[kmersIndex++] = kmerEncoding;
            }
        }
        return Arrays.copyOf(kmers, kmersIndex);
    }

    public abstract boolean keep(long kmer);
}
