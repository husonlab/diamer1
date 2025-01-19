package org.husonlab.diamer2.seq.kmers;

import org.husonlab.diamer2.seq.Sequence;
import org.husonlab.diamer2.seq.encoder.Encoder;

public class KmerExtractorProtein extends KmerExtractor<Short> {

    public KmerExtractorProtein(Encoder encoder) {
        super(encoder);
    }

    @Override
    public long[] extractKmers(Sequence<Short> sequence) {
        int seqLength = sequence.length();
        if (seqLength < k) {
            return new long[0];
        }
        kmerEncoder.reset();
        long[] kmers = new long[sequence.length() - k + 1];
        for (int i = 0; i < k - 1; i++) {
            kmerEncoder.addBack(sequence.get(i));
        }
        for (int i = k - 1; i < seqLength; i++) {
            kmers[i - k + 1] = kmerEncoder.addBack(sequence.get(i));
        }
        return kmers;
    }
}
