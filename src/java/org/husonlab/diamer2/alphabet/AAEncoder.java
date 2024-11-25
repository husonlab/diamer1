package org.husonlab.diamer2.alphabet;

public class AAEncoder {
    /**
     * Converts a protein sequence to the number representation in an alphabet of size 11.
     * @param sequence amino acid sequence
     * @return long representation of the sequence in a base 11 alphabet
     */
    public static long toBase11(String sequence) {
        if (sequence.length() > 18) {
            throw new IllegalArgumentException("Sequence too long. Not more than 18 amino acids can be encoded in a long.");
        }
        long result = 0;
        short length = (short) sequence.length();
        for (int i = 0; i < length; i++) {
            try {
                result += (long) (toBase11(sequence.charAt(length - i - 1))*Math.pow(11, i));
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * Converts an amino acid to a number in the base 11 alphabet.
     * @param aa amino acid (upper case)
     * @return number representation of the amino acid in the base 11 alphabet
     */
    public static short toBase11(char aa) {
        switch (aa) {
            // X unknown, B aspartate/asparagine, Z glutamate/glutamine, O pyrrolysine
            case 'B', 'D', 'E', 'K', 'N', 'O', 'Q', 'R', 'X', 'Z' -> { return 0; }
            case 'A', 'S', 'T' -> { return 1; }
            // J leucine/isoleucine
            case 'I', 'J', 'L', 'V' -> { return 2; }
            case 'G' -> { return 3; }
            case 'P' -> { return 4; }
            case 'F' -> { return 5; }
            case 'Y' -> { return 6; }
            // U selenocysteine
            case 'C', 'U' -> { return 7; }
            case 'H' -> { return 8; }
            case 'M' -> { return 9; }
            case 'W' -> { return 10; }
            default -> throw new IllegalArgumentException("Invalid amino acid: " + aa);
        }
    }
}
