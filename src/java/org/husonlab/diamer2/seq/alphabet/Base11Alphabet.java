package org.husonlab.diamer2.seq.alphabet;

import java.util.HashMap;
import java.util.LinkedList;

public class Base11Alphabet implements ReducedProteinAlphabet {
    private static final int base = 11;

    @Override
    public int getBase() {
        return base;
    }

    /**
     * Converts an amino acid to a number in the base 11 alphabet.
     * @param aa amino acid (upper case)
     * @return number representation of the amino acid in the base 11 alphabet
     */
    public short encodeAA(char aa) {
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

    /**
     * Translates a DNA codon to the base 11 alphabet.
     * @param codon DNA codon
     * @return number representation of the codon in base 11 encoding
     */
    public static short codonToAAAndBase11(String codon) {
        switch (codon) {
            case "GAT", "GAC",                                  // D
                 "GAA", "GAG",                                  // E
                 "AAA", "AAG",                                  // K
                 "AAT", "AAC",                                  // N
                 "CAA", "CAG",                                  // Q
                 "CGT", "CGC", "CGA", "CGG", "AGA", "AGG" -> {  // R
                return 0;
            }
            case "GCT", "GCC", "GCA", "GCG",                    // A
                 "TCT", "TCC", "TCA", "TCG", "AGT", "AGC",      // S
                 "ACT", "ACC", "ACA", "ACG" -> {                // T
                return 1;
            }
            case "ATT", "ATC", "ATA",                           // I
                 "TTA", "TTG", "CTT", "CTC", "CTA", "CTG",      // L
                 "GTT", "GTC", "GTA", "GTG" -> {                // V
                return 2;
            }
            case "GGT", "GGC", "GGA", "GGG" -> {                // G
                return 3;
            }
            case "CCT", "CCC", "CCA", "CCG" -> {                // P
                return 4;
            }
            case "TTT", "TTC" -> {                              // F
                return 5;
            }
            case "TAT", "TAC" -> {                              // Y
                return 6;
            }
            case "TGT", "TGC" -> {                              // C
                return 7;
            }
            case "CAT", "CAC" -> {                              // H
                return 8;
            }
            case "ATG" -> {                                     // M
                return 9;
            }
            case "TGG" -> {                                     // W
                return 10;
            }
            // todo: handle stop codons
            case "TAA", "TAG", "TGA" -> {
                return 0;
            }
            default -> throw new IllegalArgumentException("Invalid codon: " + codon);
        }
    }

    /**
     * Uses the toAAAndBase11AndNumber as reference to generate a mapping from DNA codons to amino acids for forward and
     * reverse reading direction in base 11 encoding.
     */
    public static void generateToAAAndBase11AndNumberFR() {
        String[] codons = {"AAA", "AAC", "AAG", "AAT", "ACA", "ACC", "ACG", "ACT", "AGA", "AGC", "AGG", "AGT",
                "ATA", "ATC", "ATG", "ATT", "CAA", "CAC", "CAG", "CAT", "CCA", "CCC", "CCG", "CCT", "CGA", "CGC",
                "CGG", "CGT", "CTA", "CTC", "CTG", "CTT", "GAA", "GAC", "GAG", "GAT", "GCA", "GCC", "GCG", "GCT",
                "GGA", "GGC", "GGG", "GGT", "GTA", "GTC", "GTG", "GTT", "TAA", "TAC", "TAG", "TAT", "TCA", "TCC",
                "TCG", "TCT", "TGA", "TGC", "TGG", "TGT", "TTA", "TTC", "TTG", "TTT"};
        HashMap<String, LinkedList<String>> encodingToCodons = new HashMap<>();
        for (String codon : codons) {
            String reverse = Utilities.reverseComplement(codon);
            int codonEncoding = codonToAAAndBase11(codon);
            int reverseEncoding = codonToAAAndBase11(reverse);
            String encoding = "(short)" + codonEncoding + ", " + "(short)" + reverseEncoding;
            encodingToCodons.computeIfAbsent(encoding, k -> new LinkedList<>());
            encodingToCodons.computeIfPresent(encoding, (k, v) -> { v.add(codon); return v; });
        }
        for (String encoding : encodingToCodons.keySet()) {
            StringBuilder sb = new StringBuilder();
            sb.append("case ");
            for (String codon : encodingToCodons.get(encoding)) {
                sb.append("\"").append(codon).append("\", ");
            }
            sb.delete(sb.length() - 2, sb.length());
            sb.append(" -> { return new short[]{").append(encoding).append("}; }");
            System.out.println(sb);
        }
    }

    @Override
    public short[] encodeDNA(String codon) {
        switch (codon) {
            case "ACC", "GCC", "TCC" -> { return new short[]{(short)1, (short)3}; }
            case "ATA", "GTA" -> { return new short[]{(short)2, (short)6}; }
            case "AGA", "CGA", "CGC", "CGT", "TGA" -> { return new short[]{(short)0, (short)1}; }
            case "ACT", "AGC", "AGT", "GCT" -> { return new short[]{(short)1, (short)1}; }
            case "ACG", "GCG", "TCA", "TCG", "TCT" -> { return new short[]{(short)1, (short)0}; }
            case "AAC", "AAG", "AAT", "CAA", "CAG", "GAC", "GAG", "GAT", "TAA", "TAG" -> { return new short[]{(short)0, (short)2}; }
            case "GGG" -> { return new short[]{(short)3, (short)4}; }
            case "AAA", "GAA" -> { return new short[]{(short)0, (short)5}; }
            case "GTG" -> { return new short[]{(short)2, (short)8}; }
            case "AGG", "CGG" -> { return new short[]{(short)0, (short)4}; }
            case "ATG" -> { return new short[]{(short)9, (short)8}; }
            case "TGG" -> { return new short[]{(short)10, (short)4}; }
            case "CCC" -> { return new short[]{(short)4, (short)3}; }
            case "TTC", "TTT" -> { return new short[]{(short)5, (short)0}; }
            case "CCG", "CCT" -> { return new short[]{(short)4, (short)0}; }
            case "CCA" -> { return new short[]{(short)4, (short)10}; }
            case "CAT" -> { return new short[]{(short)8, (short)9}; }
            case "TAC", "TAT" -> { return new short[]{(short)6, (short)2}; }
            case "TGC", "TGT" -> { return new short[]{(short)7, (short)1}; }
            case "GGA", "GGC", "GGT" -> { return new short[]{(short)3, (short)1}; }
            case "CAC" -> { return new short[]{(short)8, (short)2}; }
            case "ACA", "GCA" -> { return new short[]{(short)1, (short)7}; }
            case "ATC", "ATT", "CTA", "CTC", "CTG", "CTT", "GTC", "GTT", "TTA", "TTG" -> { return new short[]{(short)2, (short)0}; }
            default -> throw new IllegalArgumentException("Invalid codon: " + codon);
        }
    }
}
