package org.husonlab.diamer2.seq;

import org.husonlab.diamer2.seq.alphabet.ReducedProteinAlphabet;

public class KmerExtractorDNA extends KmerExtractor {

    KmerEncoder[] forwardEncoders;
    KmerEncoder[] reverseEncoders;

    public KmerExtractorDNA(long mask, ReducedProteinAlphabet proteinAlphabet) {
        super(mask, proteinAlphabet);
        forwardEncoders = new KmerEncoder[3];
        reverseEncoders = new KmerEncoder[3];
        int base = proteinAlphabet.getBase();
        for (int i = 0; i < 3; i++) {
            forwardEncoders[i] = new KmerEncoder(base, mask);
            reverseEncoders[i] = new KmerEncoder(base, mask);
        }
    }

    @Override
    public long[] extractKmers(String sequence) {
        int seqLength = sequence.length();
        if (seqLength < k * 3) {
            return new long[0];
        }
        for (int i = 0; i < 3; i++) {
            forwardEncoders[i].reset();
            reverseEncoders[i].reset();
        }
        long[] kmers = new long[2 * ((seqLength/3) + ((seqLength-1)/3) + ((seqLength-2)/3)) - 6 * (k - 1)];
        StringBuilder triplet = new StringBuilder(sequence.substring(0, 2));
        for (int i = 2; i < k*3 - 1; i++) {
            triplet.append(sequence.charAt(i));
            short[] encoding = proteinAlphabet.encodeDNA(triplet.toString());
            forwardEncoders[(i + 1) % 3].addBack(encoding[0]);
            reverseEncoders[(i + 1) % 3].addFront(encoding[1]);
            triplet.deleteCharAt(0);
        }
        for (int i = k*3 - 1; i <seqLength; i++) {
            triplet.append(sequence.charAt(i));
            short[] encoding = proteinAlphabet.encodeDNA(triplet.toString());
            kmers[2 * (i-k*3+1)] = forwardEncoders[(i + 1) % 3].addBack(encoding[0]);
            kmers[2 * (i-k*3+1) + 1] = reverseEncoders[(i + 1) % 3].addFront(encoding[1]);
            triplet.deleteCharAt(0);
        }
        return kmers;
    }
}
