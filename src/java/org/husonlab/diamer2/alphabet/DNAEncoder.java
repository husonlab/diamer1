package org.husonlab.diamer2.alphabet;

import java.util.HashMap;
import java.util.LinkedList;

public class DNAEncoder {

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

    /**
     * Translates a DNA codon to an amino acid.
     * @param codon DNA codon
     * @return corresponding amino acid
     */
    public static char toAA(String codon) {
        switch (codon) {
            case "TTT", "TTC" -> { return 'F'; }
            case "TTA", "TTG", "CTT", "CTC", "CTA", "CTG" -> { return 'L'; }
            case "TCT", "TCC", "TCA", "TCG", "AGT", "AGC" -> { return 'S'; }
            case "TAT", "TAC" -> { return 'Y'; }
            case "TAA", "TAG", "TGA" -> { return '*'; }
            case "TGT", "TGC" -> { return 'C'; }
            case "TGG" -> { return 'W'; }
            case "CCT", "CCC", "CCA", "CCG" -> { return 'P'; }
            case "CAT", "CAC" -> { return 'H'; }
            case "CAA", "CAG" -> { return 'Q'; }
            case "CGT", "CGC", "CGA", "CGG", "AGA", "AGG" -> { return 'R'; }
            case "ATT", "ATC", "ATA" -> { return 'I'; }
            case "ATG" -> { return 'M'; }
            case "ACT", "ACC", "ACA", "ACG" -> { return 'T'; }
            case "AAT", "AAC" -> { return 'N'; }
            case "AAA", "AAG" -> { return 'K'; }
            case "GTT", "GTC", "GTA", "GTG" -> { return 'V'; }
            case "GCT", "GCC", "GCA", "GCG" -> { return 'A'; }
            case "GAT", "GAC" -> { return 'D'; }
            case "GAA", "GAG" -> { return 'E'; }
            case "GGT", "GGC", "GGA", "GGG" -> { return 'G'; }
            default -> throw new IllegalArgumentException("Invalid codon: " + codon);
        }
    }

    /**
     * Translates a DNA sequence to the base 11 amino acid alphabet.
     * @param sequence DNA sequence
     * @return number representation of the translated sequence in base 11 encoding
     */
    public static long toAAAndBase11(String sequence) {
        long result = 0;
        for (int i = 0; i < sequence.length() - 2; i += 3) {
            String codon = sequence.substring(i, i + 3);
            result = result * 11 + codonToAAAndBase11(codon);
        }
        return result;
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
     * Translates a DNA codon to the base 11 alphabet in forward and reverse orientation.
     * @param codon DNA codon
     * @return short array with the number representation of the codon in forward [0] and reverse [1] orientation.
     */
    public static short[] toAAAndBase11FR(String codon) {
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
            String reverse = reverseComplement(codon);
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
}
