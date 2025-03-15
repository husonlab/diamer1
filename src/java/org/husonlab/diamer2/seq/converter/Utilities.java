package org.husonlab.diamer2.seq.converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

public class Utilities {

    public static String[] codons = {"AAA", "AAC", "AAG", "AAT", "ACA", "ACC", "ACG", "ACT", "AGA", "AGC", "AGG", "AGT",
            "ATA", "ATC", "ATG", "ATT", "CAA", "CAC", "CAG", "CAT", "CCA", "CCC", "CCG", "CCT", "CGA", "CGC",
            "CGG", "CGT", "CTA", "CTC", "CTG", "CTT", "GAA", "GAC", "GAG", "GAT", "GCA", "GCC", "GCG", "GCT",
            "GGA", "GGC", "GGG", "GGT", "GTA", "GTC", "GTG", "GTT", "TAA", "TAC", "TAG", "TAT", "TCA", "TCC",
            "TCG", "TCT", "TGA", "TGC", "TGG", "TGT", "TTA", "TTC", "TTG", "TTT"};

    public static String reverseComplement(String sequence) {
        StringBuilder sb = new StringBuilder();
        for (int i = sequence.length() - 1; i >= 0; i--) {
            char c = sequence.charAt(i);
            switch (c) {
                case 'A' -> { sb.append('T'); }
                case 'T' -> { sb.append('A'); }
                case 'G' -> { sb.append('C'); }
                case 'C' -> { sb.append('G'); }
                default -> throw new IllegalArgumentException("Invalid character: " + c);
            }
        }
        return sb.toString();
    }

    /**
     * Base 11 alphabet with equal distribution of amino acid likelihood.
     */
    public static byte encodeAABase11Uniform(char aa) {
        switch (aa) {
            case 'L' -> { return 0; }
            case 'A' -> { return 1; }
            case 'G', 'C' -> { return 2; }
            case 'V', 'W', 'U', 'B', 'J', 'Z', 'O' -> { return 3; }
            case 'S', 'H' -> { return 4; }
            case 'E', 'M', 'X' -> { return 5; }
            case 'T', 'Y' -> { return 6; }
            case 'R', 'Q' -> { return 7; }
            case 'D', 'N' -> { return 8; }
            case 'I', 'F' -> { return 9; }
            case 'P', 'K' -> { return 10; }
            case '*' -> { return -1; }
            default -> throw new IllegalArgumentException("Invalid amino acid: " + aa);
        }
    }

    /**
     * Translates a DNA codon to an amino acid.
     * @param codon DNA codon
     * @return an amino acid
     */
    public static char codonToAA(String codon) {
        switch (codon) {
            case "GAT", "GAC" -> { return 'D'; }  // Aspartic acid
            case "GAA", "GAG" -> { return 'E'; }  // Glutamic acid
            case "AAA", "AAG" -> { return 'K'; }  // Lysine
            case "AAT", "AAC" -> { return 'N'; }  // Asparagine
            case "CAA", "CAG" -> { return 'Q'; }  // Glutamine
            case "CGT", "CGC", "CGA", "CGG", "AGA", "AGG" -> { return 'R'; }  // Arginine
            case "GCT", "GCC", "GCA", "GCG" -> { return 'A'; }  // Alanine
            case "TCT", "TCC", "TCA", "TCG", "AGT", "AGC" -> { return 'S'; }  // Serine
            case "ACT", "ACC", "ACA", "ACG" -> { return 'T'; }  // Threonine
            case "ATT", "ATC", "ATA" -> { return 'I'; }  // Isoleucine
            case "TTA", "TTG", "CTT", "CTC", "CTA", "CTG" -> { return 'L'; }  // Leucine
            case "GTT", "GTC", "GTA", "GTG" -> { return 'V'; }  // Valine
            case "GGT", "GGC", "GGA", "GGG" -> { return 'G'; }  // Glycine
            case "CCT", "CCC", "CCA", "CCG" -> { return 'P'; }  // Proline
            case "TTT", "TTC" -> { return 'F'; }  // Phenylalanine
            case "TAT", "TAC" -> { return 'Y'; }  // Tyrosine
            case "TGT", "TGC" -> { return 'C'; }  // Cysteine
            case "CAT", "CAC" -> { return 'H'; }  // Histidine
            case "ATG" -> { return 'M'; }  // Methionine
            case "TGG" -> { return 'W'; }  // Tryptophan
            case "TAA", "TAG", "TGA" -> { return '*'; }  // Stop codons
            default -> throw new IllegalArgumentException("Invalid codon: " + codon);
        }
    }

    /**
     * Translated a DNA codon to an amino acid in forward and reverse reading direction.
     * @param codon DNA codon
     * @return amino acids in forward and reverse reading direction
     */
    public static char[] codonToAAFR(String codon) {
        switch (codon) {
            case "CCG", "CCT" -> { return new char[]{'P', 'R'}; }
            case "CAT" -> { return new char[]{'H', 'M'}; }
            case "CTG", "TTG" -> { return new char[]{'L', 'Q'}; }
            case "GAT" -> { return new char[]{'D', 'I'}; }
            case "CCA" -> { return new char[]{'P', 'W'}; }
            case "CAC" -> { return new char[]{'H', 'V'}; }
            case "TAT" -> { return new char[]{'Y', 'I'}; }
            case "GAC" -> { return new char[]{'D', 'V'}; }
            case "ATC" -> { return new char[]{'I', 'D'}; }
            case "ATG" -> { return new char[]{'M', 'H'}; }
            case "CAA", "CAG" -> { return new char[]{'Q', 'L'}; }
            case "TAC" -> { return new char[]{'Y', 'V'}; }
            case "GAA" -> { return new char[]{'E', 'F'}; }
            case "GCA" -> { return new char[]{'A', 'C'}; }
            case "ATT" -> { return new char[]{'I', 'N'}; }
            case "GCC" -> { return new char[]{'A', 'G'}; }
            case "GAG" -> { return new char[]{'E', 'L'}; }
            case "GTC" -> { return new char[]{'V', 'D'}; }
            case "CGC" -> { return new char[]{'R', 'A'}; }
            case "ATA" -> { return new char[]{'I', 'Y'}; }
            case "GCG" -> { return new char[]{'A', 'R'}; }
            case "GCT" -> { return new char[]{'A', 'S'}; }
            case "GTG" -> { return new char[]{'V', 'H'}; }
            case "TCA" -> { return new char[]{'S', '*'}; }
            case "GTT" -> { return new char[]{'V', 'N'}; }
            case "AAT" -> { return new char[]{'N', 'I'}; }
            case "AGG", "CGG" -> { return new char[]{'R', 'P'}; }
            case "TTC" -> { return new char[]{'F', 'E'}; }
            case "AGA", "CGA" -> { return new char[]{'R', 'S'}; }
            case "CGT" -> { return new char[]{'R', 'T'}; }
            case "GTA" -> { return new char[]{'V', 'Y'}; }
            case "TTT" -> { return new char[]{'F', 'K'}; }
            case "AAC" -> { return new char[]{'N', 'V'}; }
            case "AGC" -> { return new char[]{'S', 'A'}; }
            case "TCC" -> { return new char[]{'S', 'G'}; }
            case "TGG" -> { return new char[]{'W', 'P'}; }
            case "GGC" -> { return new char[]{'G', 'A'}; }
            case "AAA" -> { return new char[]{'K', 'F'}; }
            case "CTA", "TTA" -> { return new char[]{'L', '*'}; }
            case "TGC" -> { return new char[]{'C', 'A'}; }
            case "TCG", "TCT" -> { return new char[]{'S', 'R'}; }
            case "AAG" -> { return new char[]{'K', 'L'}; }
            case "AGT" -> { return new char[]{'S', 'T'}; }
            case "TAA", "TAG" -> { return new char[]{'*', 'L'}; }
            case "GGG" -> { return new char[]{'G', 'P'}; }
            case "TGA" -> { return new char[]{'*', 'S'}; }
            case "GGA" -> { return new char[]{'G', 'S'}; }
            case "GGT" -> { return new char[]{'G', 'T'}; }
            case "ACA" -> { return new char[]{'T', 'C'}; }
            case "TGT" -> { return new char[]{'C', 'T'}; }
            case "ACC" -> { return new char[]{'T', 'G'}; }
            case "CCC" -> { return new char[]{'P', 'G'}; }
            case "CTC" -> { return new char[]{'L', 'E'}; }
            case "ACG" -> { return new char[]{'T', 'R'}; }
            case "ACT" -> { return new char[]{'T', 'S'}; }
            case "CTT" -> { return new char[]{'L', 'K'}; }
            default -> { return new char[]{'*', '*'}; }
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
            String reverse = org.husonlab.diamer2.seq.alphabet.Utilities.reverseComplement(codon);
            int codonEncoding = encodeAABase11Uniform(codonToAA(codon));
            int reverseEncoding = encodeAABase11Uniform(codonToAA(reverse));
            String encoding = "(byte)" + codonEncoding + ", " + "(byte)" + reverseEncoding;
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
            sb.append(" -> { return new byte[]{").append(encoding).append("}; }");
            System.out.println(sb);
        }
    }

    /**
     * Splits all input arrays at stop codons ({@code -1}) and returns the resulting sequences that are above or equal
     * to the given length threshold.
     * @param sequences Input sequences
     * @param minLength Minimum length of a sequence
     * @return Sequences split at stop codons
     */
    public static Byte[][] splitAtMinus1AndSizeFilter(byte[][] sequences, int minLength) {
        ArrayList<Byte[]> sequencesArrayList = new ArrayList<>();
        for (byte[] sequence : sequences) {
            ArrayList<Byte> sequenceArrayList = new ArrayList<>();
            for (byte aa: sequence) {
                if (aa == -1) {
                    if (!sequenceArrayList.isEmpty() && sequenceArrayList.size() >= minLength) {
                        sequencesArrayList.add(sequenceArrayList.toArray(Byte[]::new));
                    }
                    sequenceArrayList = new ArrayList<>();
                } else {
                    sequenceArrayList.add(aa);
                }
            }
            if (!sequenceArrayList.isEmpty() && sequenceArrayList.size() >= minLength) {
                sequencesArrayList.add(sequenceArrayList.toArray(Byte[]::new));
            }
        }
        return sequencesArrayList.toArray(Byte[][]::new);
    }

    public static byte[][] splitAtMinus1(byte[] sequence) {
        ArrayList<byte[]> sequences = new ArrayList<>();
        int i = 0;
        int j = 0;
        for (; j < sequence.length; j++) {
            if (sequence[j] == -1) {
                if (i != j) {
                    sequences.add(Arrays.copyOfRange(sequence, i, j));
                }
                i = 1 + j;
            }
        }
        if (i != j) {
            sequences.add(Arrays.copyOfRange(sequence, i, j));
        }
        return sequences.toArray(byte[][]::new);
    }
}
