package org.husonlab.diamer2.seq.alphabet;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Utility methods for working with different alphabets.
 */
public class Utilities {

    /**
     * Array with all 26 letters of the alphabet that occur in amino acid sequences.
     * <p>extended amino acid alphabet:
     *     <li><a href="https://www.ddbj.nig.ac.jp/ddbj/code-e.html">DDBJ</a></li>
     *     <li><a href="https://www.ncbi.nlm.nih.gov/CBBResearch/Yu/logoddslogo/apidocs/weblogo/seq.html">NCBI</a></li>
     * </p>
     */
    @Deprecated
    public static Character[] alphabetAAArray = new Character[]{
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
        'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'
    };

    /**
     * HashSet to efficiently check if a character is in the extended amino acid alphabet.
     */
    @Deprecated
    public static HashSet<Character> alphabetAA = new HashSet<Character>(Arrays.asList(alphabetAAArray));

    /**
     * Reduces the alphabet of the sequence to the extended amino acid alphabet.
     * @param seq amino acid sequence
     * @return sequence containing only characters from the extended amino acid alphabet
     */
    @Deprecated
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
