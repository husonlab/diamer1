package org.husonlab.diamer2.alphabet;

import org.husonlab.diamer2.util.Pair;

import java.util.LinkedList;
import java.util.stream.Collectors;

import static org.husonlab.diamer2.alphabet.AAEncoder.toBase11;
import static org.husonlab.diamer2.alphabet.DNAEncoder.toAAAndBase11AndNumberFR;

public class DNAKmerEncoder {
    private final StringBuilder codon;
    private final ReadingFrame[] forwardReadingFrames;
    private final ReadingFrame[] reverseReadingFrames;
    private int position;

    public DNAKmerEncoder(int k, String firstTwoNucleotides) {
        if (firstTwoNucleotides.length() != 2) {
            throw new IllegalArgumentException("First two nucleotides must be of length 2");
        }
        codon = new StringBuilder(3);
        codon.append(firstTwoNucleotides.charAt(0));
        codon.append(firstTwoNucleotides.charAt(1));
        forwardReadingFrames = new ReadingFrame[3];
        reverseReadingFrames = new ReadingFrame[3];
        for (int i = 0; i < 3; i++) {
            forwardReadingFrames[i] = new ReadingFrame(k, 11);
            reverseReadingFrames[i] = new ReadingFrame(k, 11);
        }
        position = -1;
    }

    public DNAKmerEncoder addNucleotide(char nucleotide) {
        codon.append(nucleotide);
        position++;
        short[] encoding = toAAAndBase11AndNumberFR(codon.toString());
        forwardReadingFrames[position % 3].addBack(encoding[0]);
        reverseReadingFrames[position % 3].addFront(encoding[0]);
        codon.deleteCharAt(0);
        return this;
    }

    public long[] getEncodedKmers() {
        return new long[]{
                forwardReadingFrames[position % 3].getEncodedKmer(),
                reverseReadingFrames[position % 3].getEncodedKmer()
        };
    }

    public static class ReadingFrame {
        private final int k;
        private final int alphabetSize;
        private final LinkedList<Short> kmer;
        private long encodedKmer;

        public ReadingFrame(int k, int alphabetSize) {
            this.k = k;
            this.alphabetSize = alphabetSize;
            kmer = new LinkedList<>();
            for (int i = 0; i < k; i++) {
                kmer.addFirst((short) 0);
            }
            encodedKmer = 0;
        }

        public ReadingFrame addFront(short aaEncoding) {
            encodedKmer = (encodedKmer - kmer.pollLast()) / alphabetSize + aaEncoding * (long)Math.pow(alphabetSize, k - 1);
            kmer.addFirst(aaEncoding);
            return this;
        }

        public ReadingFrame addBack(short aaEncoding) {
            encodedKmer = (encodedKmer - kmer.pollFirst() * (long)Math.pow(alphabetSize, k - 1)) * alphabetSize + aaEncoding;
            kmer.addLast(aaEncoding);
            return this;
        }

        public long getEncodedKmer() {
            return encodedKmer;
        }
    }
}
