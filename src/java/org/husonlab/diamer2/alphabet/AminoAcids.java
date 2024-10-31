package org.husonlab.diamer2.alphabet;

import org.husonlab.diamer2.seq.FASTA;

public class AminoAcids {

    public static final String AA_Alphabet = "ACDEFGHIKLMNPQRSTVWY";

    public static FASTA enforceAAAlphabet(FASTA fasta) {
        String seq = fasta.getSequence().toUpperCase();
        StringBuilder newSeq = new StringBuilder();
        for (int i = 0; i < seq.length(); i++) {
            char c = seq.charAt(i);
            if (AA_Alphabet.indexOf(c) != -1) {
                newSeq.append(c);
            }
        }
        return new FASTA(fasta.getHeader(), newSeq.toString());
    }

    public static long to11Num_15(String sequence) {
        /*
        Method to convert an amino acid sequence of length <= 15 to a long.
        Only 52 bits of the long are used.
        The amino acids are interpreted as a base 11 number.
         */
        if (sequence.length() > 15) {
            throw new IllegalArgumentException("Sequence too long.");
        }
        long result = 0;
        short length = (short) sequence.length();
        for (int i = 0; i < length; i++) {
            result += as11AndNumber(sequence.charAt(length - i - 1))*Math.pow(11, i);
        }
        return result;
    }

    public static String from11_15(long sequence) {
        /*
        Method to convert a long to an amino acid sequence of length <= 15.
        The amino acids are interpreted as a base 11 number.
         */
        StringBuilder result = new StringBuilder();
        while (sequence > 0) {
            result.append(asChar(sequence % 11));
            sequence /= 11;
        }
        return result.toString();
    }

    public static String to11(String sequence) {
        /*
        Method to convert an amino acid sequence to a sequence in an 11 based alphabet.
         */
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < sequence.length(); i++) {
            result.append(as11(sequence.charAt(i)));
        }
        return result.toString();
    }

    private static char as11(char aa) {
        /*
        Method to reduce the alphabet to 11.
        For the grouped amino acids a representative amino acid is returned.
         */
        switch (aa) {
            case 'P' -> { return 'P'; }
            case 'W' -> { return 'W'; }
            case 'Y' -> { return 'Y'; }
            case 'F' -> { return 'F'; }
            case 'M' -> { return 'M'; }
            case 'L', 'I', 'V' -> { return 'L'; }
            case 'C' -> { return 'C'; }
            case 'H' -> { return 'H'; }
            case 'K', 'R', 'Q', 'E', 'D', 'N' -> { return 'K'; }
            case 'G' -> { return 'G'; }
            case 'A', 'S', 'T' -> { return 'A'; }
            default -> throw new IllegalArgumentException("Invalid amino acid: " + aa);
        }
    }

    private static short as11AndNumber(char c) {
        /*
        Method to reduce the alphabet to 11 and convert the amino acid to a number.
         */
        switch (c) {
            case 'P' -> { return 0; }
            case 'W' -> { return 1; }
            case 'Y' -> { return 2; }
            case 'F' -> { return 3; }
            case 'M' -> { return 4; }
            case 'L', 'I', 'V' -> { return 5; }
            case 'C' -> { return 6; }
            case 'H' -> { return 7; }
            case 'K', 'R', 'Q', 'E', 'D', 'N' -> { return 8; }
            case 'G' -> { return 9; }
            case 'A', 'S', 'T' -> { return 10; }
            default -> throw new IllegalArgumentException("Invalid amino acid: " + c);
        }
    }

    private static char asChar(long n) {
        /*
        Method to convert a number to an amino acid in an 11 based alphabet.
         */
        switch ((int) n) {
            case 0 -> { return 'P'; }
            case 1 -> { return 'W'; }
            case 2 -> { return 'Y'; }
            case 3 -> { return 'F'; }
            case 4 -> { return 'M'; }
            case 5 -> { return 'L'; }
            case 6 -> { return 'C'; }
            case 7 -> { return 'H'; }
            case 8 -> { return 'K'; }
            case 9 -> { return 'G'; }
            case 10 -> { return 'A'; }
            default -> throw new IllegalArgumentException("Invalid number: " + n);
        }
    }
}
