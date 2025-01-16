package org.husonlab.diamer2.seq.kmers;

import org.husonlab.diamer2.seq.alphabet.ReducedProteinAlphabet;

public class KmerExtractorProtein extends KmerExtractor {

    private final KmerEncoder encoder = new KmerEncoder(proteinAlphabet.getBase(), mask);

    public KmerExtractorProtein(long mask, ReducedProteinAlphabet proteinAlphabet) {
        super(mask, proteinAlphabet);
    }

    @Override
    public long[] extractKmers(String sequence) {
        int seqLength = sequence.length();
        if (seqLength < k) {
            return new long[0];
        }
        encoder.reset();
        long[] kmers = new long[sequence.length() - k + 1];
        for (int i = 0; i < k - 1; i++) {
            encoder.addBack(proteinAlphabet.encodeAA(sequence.charAt(i)));
        }
        for (int i = k - 1; i < seqLength; i++) {
            kmers[i - k + 1] = encoder.addBack(proteinAlphabet.encodeAA(sequence.charAt(i)));
        }
        return kmers;
    }
}
