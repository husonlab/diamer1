package org.husonlab.diamer2.seq.converter;

import org.husonlab.diamer2.seq.Compressed4BitSequence;
import org.husonlab.diamer2.seq.Sequence;
import org.husonlab.diamer2.seq.alphabet.Alphabet;
import org.husonlab.diamer2.seq.alphabet.Base11Alphabet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class Utilities {

    public static byte encodeAABase11(char aa) {
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
     * Splits all input sequences at stop codons {@code -1} and returns the resulting sequences that are above the given
     * length threshold.
     * @param sequences Input sequences
     * @param minLength Minimum length of a sequence
     * @param alphabet Alphabet of the input sequences
     * @return Sequences split at stop codons
     */
    public static Compressed4BitSequence[] splitAtStopCodons(byte[][] sequences, int minLength, Alphabet<Byte> alphabet) {
        ArrayList<Compressed4BitSequence> sequencesArrayList = new ArrayList<>();
        for (byte[] sequence : sequences) {
            ArrayList<Byte> sequenceArrayList = new ArrayList<>();
            for (byte aa: sequence) {
                if (aa == -1) {
                    if (!sequenceArrayList.isEmpty() && sequenceArrayList.size() >= minLength) {
                        sequencesArrayList.add(new Compressed4BitSequence(alphabet, sequenceArrayList.toArray(new Byte[0])));
                    }
                    sequenceArrayList = new ArrayList<>();
                } else {
                    sequenceArrayList.add(aa);
                }
            }
            if (!sequenceArrayList.isEmpty() && sequenceArrayList.size() >= minLength) {
                sequencesArrayList.add(new Compressed4BitSequence(alphabet, sequenceArrayList.toArray(new Byte[0])));
            }
        }
        return sequencesArrayList.toArray(new Compressed4BitSequence[0]);
    }
}
