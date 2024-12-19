package org.husonlab.diamer2.seq;

import org.husonlab.diamer2.seq.alphabet.AAEncoder;
import org.husonlab.diamer2.seq.alphabet.AAKmerEncoder;
import org.husonlab.diamer2.seq.alphabet.DNAKmerEncoder;

import java.util.stream.Stream;

public class KmerExtractor {
    public static Stream<Long> extractKmersProtein(String sequence, int kmerSize) {
        if (sequence.length() < kmerSize) {
            return Stream.empty();
        } else {
            AAKmerEncoder encoder = new AAKmerEncoder(15, 11);
            for (int i = 0; i < 14; i++) {
                encoder.addBack(AAEncoder.toBase11(sequence.charAt(i)));
            }
            return sequence.substring(14).chars().mapToObj(c -> (char) c).map(AAEncoder::toBase11)
                    .map(encoder::addBack);
        }
    }

    public static Stream<long[]> extractKmersDNA(String sequence, int kmerSize) {
        if (sequence.length() < kmerSize * 3) {
            return Stream.empty();
        } else {
            DNAKmerEncoder encoder = new DNAKmerEncoder(15, sequence.substring(0, 2));
            for (int i = 2; i < 15 * 3 - 1; i++) {
                encoder.addNucleotide(sequence.charAt(i));
            }
            return sequence.substring(15 * 3 - 1).chars().mapToObj(c -> (char) c)
                    .map(encoder::addNucleotide);
        }
    }
}
