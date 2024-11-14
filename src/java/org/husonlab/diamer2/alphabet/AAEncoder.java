package org.husonlab.diamer2.alphabet;

public class AAEncoder {
    /**
     * Converts a protein sequence to an alphabet of size 11 and the reduced sequence further to a long.
     * @param sequence amino acid sequence
     * @return long representation of the sequence in a base 11 alphabet
     */
    public static long base11(String sequence) {
        if (sequence.length() > 15) {
            throw new IllegalArgumentException("Sequence too long.");
        }
        long result = 0;
        short length = (short) sequence.length();
        for (int i = 0; i < length; i++) {
            result += base11andNumber(sequence.charAt(length - i - 1))*Math.pow(11, i);
        }
        return result;
    }

    /**
     * Converts an amino acid to a number in the base 11 alphabet.
     * @param aa amino acid (upper case)
     * @return number representation of the amino acid in the base 11 alphabet
     */
    private static short base11andNumber(char aa) {
        switch (aa) {
            case 'P' -> { return 0; }
            case 'W' -> { return 1; }
            case 'Y' -> { return 2; }
            case 'F' -> { return 3; }
            case 'M' -> { return 4; }
            case 'L', 'I', 'V', 'J' -> { return 5; } // J leucine/isoleucine
            case 'C', 'U' -> { return 6; } // U selenocysteine
            case 'H' -> { return 7; }
            case 'K', 'R', 'Q', 'E', 'D', 'N', 'B', 'Z', 'O', 'X' -> { return 8; } // X unknown, B aspartate/asparagine, Z glutamate/glutamine, O pyrrolysine
            case 'G' -> { return 9; }
            case 'A', 'S', 'T' -> { return 10; }
            default -> throw new IllegalArgumentException("Invalid amino acid: " + aa);
        }
    }

    /**
     * Converts a protein sequence to an alphabet of size 11.
     * @param aa amino acid
     * @return amino acid in the base 11 alphabet
     */
    private static char base11(char aa) {
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
}
