package org.husonlab.diamer2.alphabet;

import org.husonlab.diamer2.util.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

public class DNAEncoder {

    /**
     * Translates a DNA codon to an amino acid.
     * @param codon DNA codon
     * @return corresponding amino acid
     */
    private static char toAA(String codon) {
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

    public static short toAAAndBase11AndNumber(String codon) {
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
    public static Pair<Short, Short> toAAAndBase11AndNumberFR(String codon) {
        switch (codon) {
            case "ACC", "GCC", "TCC" -> { return new Pair<>((short)1, (short)4); }
            case "CTT" -> { return new Pair<>((short)2, (short)5); }
            case "CGA", "TGA" -> { return new Pair<>((short)0, (short)1); }
            case "ACA", "ACG", "ACT", "GCA", "GCG", "GCT", "TCA", "TCG", "TCT" -> {
                return new Pair<>((short)1, (short)1);
            }
            case "GGT" -> { return new Pair<>((short)3, (short)10); }
            case "AAA", "AAC", "AAG", "AAT", "AGA", "CAA", "CAG", "CGC", "GAA", "GAC", "GAG", "GAT", "TAA", "TAG" -> {
                return new Pair<>((short)0, (short)0);
            }
            case "AGG", "CGG" -> { return new Pair<>((short)0, (short)3); }
            case "AGC", "AGT" -> { return new Pair<>((short)1, (short)0); }
            case "GTA" -> { return new Pair<>((short)2, (short)9); }
            case "ATG" -> { return new Pair<>((short)9, (short)2); }
            case "GGG" -> { return new Pair<>((short)3, (short)3); }
            case "TGG" -> { return new Pair<>((short)10, (short)3); }
            case "CCC" -> { return new Pair<>((short)4, (short)4); }
            case "TTC" -> { return new Pair<>((short)5, (short)2); }
            case "TGT" -> { return new Pair<>((short)7, (short)7); }
            case "TAT" -> { return new Pair<>((short)6, (short)6); }
            case "CCA", "CCG", "CCT" -> { return new Pair<>((short)4, (short)1); }
            case "TTT" -> { return new Pair<>((short)5, (short)5); }
            case "CAC" -> { return new Pair<>((short)8, (short)8); }
            case "CAT" -> { return new Pair<>((short)8, (short)6); }
            case "CGT" -> { return new Pair<>((short)0, (short)7); }
            case "GGA", "GGC" -> { return new Pair<>((short)3, (short)0); }
            case "TGC" -> { return new Pair<>((short)7, (short)0); }
            case "TAC" -> { return new Pair<>((short)6, (short)8); }
            case "ATA", "ATC", "ATT", "CTA", "CTC", "CTG", "GTC", "GTG", "GTT", "TTA", "TTG" -> {
                return new Pair<>((short)2, (short)2);
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
            String reverse = new StringBuilder(codon).reverse().toString();
            int codonEncoding = toAAAndBase11AndNumber(codon);
            int reverseEncoding = toAAAndBase11AndNumber(reverse);
            String encoding = "(short)" + codonEncoding + ", " + "(short)" + reverseEncoding;
            encodingToCodons.computeIfAbsent(encoding, _ -> new LinkedList<>());
            encodingToCodons.computeIfPresent(encoding, (_, v) -> { v.add(codon); return v; });
        }
        for (String encoding : encodingToCodons.keySet()) {
            StringBuilder sb = new StringBuilder();
            sb.append("case ");
            for (String codon : encodingToCodons.get(encoding)) {
                sb.append("\"").append(codon).append("\", ");
            }
            sb.delete(sb.length() - 2, sb.length());
            sb.append(" -> { return new Pair<>(").append(encoding).append("); }");
            System.out.println(sb);
        }
    }
}
