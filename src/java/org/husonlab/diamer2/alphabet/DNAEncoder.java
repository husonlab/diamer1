package org.husonlab.diamer2.alphabet;

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

    private static short toAAAndBase11AndNumber(String codon) {
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
}
