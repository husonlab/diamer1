package org.husonlab.diamer2.alphabet;

import static org.husonlab.diamer2.alphabet.AAEncoder.toBase11;
import static org.husonlab.diamer2.alphabet.DNAEncoder.toAAAndBase11FR;

public class DNAKmerEncoder {
    private final StringBuilder codon;
    private final AAKmerEncoder[] forwardReadingFrames;
    private final AAKmerEncoder[] reverseReadingFrames;
    // Used to track the current reading frame (frame shift)
    private int position;

    /**
     * Class to encode DNA kmers dynamically in a translated base 11 alphabet.
     * The class maintains all 6 reading frames of the encoded DNA sequence and updates them
     * as new nucleotides are added.
     * @param k length of the kmer in amino acids
     * @param firstTwoNucleotides first two nucleotides of the sequence
     */
    public DNAKmerEncoder(int k, String firstTwoNucleotides) {
        if (firstTwoNucleotides.length() != 2) {
            throw new IllegalArgumentException("First two nucleotides must be of length 2");
        }
        codon = new StringBuilder(3);
        codon.append(firstTwoNucleotides.charAt(0));
        codon.append(firstTwoNucleotides.charAt(1));
        forwardReadingFrames = new AAKmerEncoder[3];
        reverseReadingFrames = new AAKmerEncoder[3];
        for (int i = 0; i < 3; i++) {
            forwardReadingFrames[i] = new AAKmerEncoder(k, 11);
            reverseReadingFrames[i] = new AAKmerEncoder(k, 11);
        }
        position = -1;
    }

    /**
     * Adds a nucleotide to the sequence and updates the reading frames.
     * @param nucleotide nucleotide to add
     * @return this object
     */
    public long[] addNucleotide(char nucleotide) {
        codon.append(nucleotide);
        position++;
        short[] encoding = toAAAndBase11FR(codon.toString());
        long encodingF = forwardReadingFrames[position % 3].addBack(encoding[0]);
        long encodingR = reverseReadingFrames[position % 3].addFront(encoding[1]);
        codon.deleteCharAt(0);
        return new long[]{encodingF, encodingR};
    }

    /**
     * Returns the encoded forward and reverse base 11 AA kmers for the current DNA sequence.
     * @return long array with the encoded kmers for the forward and reverse reading frame of the current frame shift.
     */
    public long[] getEncodedKmers() {
        return new long[]{
                forwardReadingFrames[position % 3].getEncodedKmer(),
                reverseReadingFrames[position % 3].getEncodedKmer()
        };
    }
}
