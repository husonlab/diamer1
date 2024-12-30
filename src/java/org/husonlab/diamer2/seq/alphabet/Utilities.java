package org.husonlab.diamer2.seq.alphabet;

import java.util.Arrays;
import java.util.HashSet;

public class Utilities {
    public static Character[] alphabetAAArray = new Character[]{
            'B', 'D', 'E', 'K', 'N', 'O', 'Q', 'R', 'X', 'Z',
            'A', 'S', 'T', 'I', 'J', 'L', 'V', 'G', 'P', 'F', 'Y', 'C', 'U', 'H', 'M', 'W'
    };

    public static HashSet<Character> alphabetAA = new HashSet<Character>(Arrays.asList(alphabetAAArray));

    /**
     * Reduces the alphabet of the sequence to the amino acids that can be encoded in the base 11 alphabet by
     * the @link{toBase11} method.
     * @param seq amino acid sequence
     * @return sequence with only amino acids that can be encoded in the base 11 alphabet
     */
    public static String enforceAlphabet(String seq) {
        StringBuilder sb = new StringBuilder();
        for (char aa : seq.toCharArray()) {
            if (alphabetAA.contains(aa)) {
                sb.append(aa);
            }
        }
        return sb.toString();
    }

    /**
     * Translates a DNA nucleotide to its complement.
     * @param nucleotide DNA nucleotide
     * @return complement of the nucleotide
     */
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
    public static String reverseComplement(String sequence) {
        StringBuilder sb = new StringBuilder();
        for (int i = sequence.length() - 1; i >= 0; i--) {
            sb.append(reverseComplement(sequence.charAt(i)));
        }
        return sb.toString();
    }
}
