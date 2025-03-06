package org.husonlab.diamer2.seq.alphabet;

/**
 * Utility methods for working with different alphabets.
 */
public class Utilities {

    /**
     * Translates a DNA nucleotide to its complement.
     * @param nucleotide DNA nucleotide
     * @return complement of the nucleotide
     */
    @Deprecated
    public static char reverseComplement(char nucleotide) {
        switch (nucleotide) {
            case 'A' -> { return 'T'; }
            case 'T' -> { return 'A'; }
            case 'C' -> { return 'G'; }
            case 'G' -> { return 'C'; }
            default -> throw new IllegalArgumentException("Invalid nucleotide: " + nucleotide);
        }
    }

    /**
     * Translates a DNA sequence to its complement.
     * @param sequence DNA sequence
     * @return reverse complement of the sequence
     */
    @Deprecated
    public static String reverseComplement(String sequence) {
        StringBuilder sb = new StringBuilder();
        for (int i = sequence.length() - 1; i >= 0; i--) {
            sb.append(reverseComplement(sequence.charAt(i)));
        }
        return sb.toString();
    }
}
