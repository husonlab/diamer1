package org.husonlab.diamer2.seq.converter;

import org.husonlab.diamer2.seq.CharSequence;
import org.husonlab.diamer2.seq.Sequence;
import org.husonlab.diamer2.seq.Compressed4BitSequence;
import org.husonlab.diamer2.seq.alphabet.*;
import org.husonlab.diamer2.seq.alphabet.Utilities;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * Converter to convert DNA to the DIAMOND base 11 alphabet in only the first reading frame and ignores stop codons.
 * Sequences are not split at stop codons, but the stop codons are grouped in the first base 11 symbol (0).
 */
public class DNAtoBase11RF1WithStop extends Converter<Character, DNA, Byte, Base11WithStop> {

    private static final Base11WithStop TARGET_ALPHABET = new Base11WithStop();
    private static final DNA dnaAlphabet = new DNA();

    @Override
    public Sequence<Byte, Base11WithStop>[] convert(Sequence<Character, DNA> sequence) {
        LinkedList<Sequence<Byte, Base11WithStop>> translations = new LinkedList<>();
        for (Sequence<Character, DNA> validSequence: removeInvalidCharactersAndSplit(sequence)) {
            // The sequence is too short to be translated
            if (validSequence.length() < 3) {
                continue;
            }
            // Setup byte array for each reading frame
            byte[] translation = new byte[Math.floorDiv(validSequence.length(), 3)];

            for (int i = 2; i < validSequence.length(); i += 3) {
                byte[] encoding = encodeDNA(String.valueOf(validSequence.get(i - 2)) + validSequence.get(i - 1) + validSequence.get(i));
                // Use only forward reading frame
                translation[(i - 2) / 3] = encoding[0];
            }
            translations.add(new Compressed4BitSequence<>(TARGET_ALPHABET, translation));
        }

        if (translations.isEmpty()) {
            return new Sequence[0];
        } else {
            return translations.toArray(new Sequence[0]);
        }
    }

    /**
     * Removes all characters that are not part of the DNA alphabet and splits the sequence into the remaining valid
     * parts.
     */
    private Sequence<Character, DNA>[] removeInvalidCharactersAndSplit(Sequence<Character, DNA> sequence) {
        LinkedList<Sequence<Character, DNA>> sequences = new LinkedList<>();
        StringBuilder sb = new StringBuilder();
        for (Character c : sequence) {
            if (dnaAlphabet.contains(c)) {
                sb.append(c);
            } else {
                if (sb.length() > 0) {
                    sequences.add(new CharSequence<>(dnaAlphabet, sb.toString()));
                    sb = new StringBuilder();
                }
            }
        }
        if (sb.length() > 0) {
            sequences.add(new CharSequence<>(dnaAlphabet, sb.toString()));
        }
        return sequences.toArray(new Sequence[0]);
    }

    /**
     * Translates a DNA codon to the base 11 alphabet.
     * @param codon DNA codon
     * @return number representation of the codon in base 11 encoding
     */
    public static byte codonToAAAndBase11(String codon) {
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

    public byte[] encodeDNA(String codon) {
        switch (codon) {
            case "ACC", "GCC", "TCC" -> { return new byte[]{(byte)1, (byte)3}; }
            case "ATA", "GTA" -> { return new byte[]{(byte)2, (byte)6}; }
            case "AGA", "CGA", "CGC", "CGT", "TGA" -> { return new byte[]{(byte)0, (byte)1}; }
            case "ACT", "AGC", "AGT", "GCT" -> { return new byte[]{(byte)1, (byte)1}; }
            case "ACG", "GCG", "TCA", "TCG", "TCT" -> { return new byte[]{(byte)1, (byte)0}; }
            case "AAC", "AAG", "AAT", "CAA", "CAG", "GAC", "GAG", "GAT", "TAA", "TAG" -> { return new byte[]{(byte)0, (byte)2}; }
            case "GGG" -> { return new byte[]{(byte)3, (byte)4}; }
            case "AAA", "GAA" -> { return new byte[]{(byte)0, (byte)5}; }
            case "GTG" -> { return new byte[]{(byte)2, (byte)8}; }
            case "AGG", "CGG" -> { return new byte[]{(byte)0, (byte)4}; }
            case "ATG" -> { return new byte[]{(byte)9, (byte)8}; }
            case "TGG" -> { return new byte[]{(byte)10, (byte)4}; }
            case "CCC" -> { return new byte[]{(byte)4, (byte)3}; }
            case "TTC", "TTT" -> { return new byte[]{(byte)5, (byte)0}; }
            case "CCG", "CCT" -> { return new byte[]{(byte)4, (byte)0}; }
            case "CCA" -> { return new byte[]{(byte)4, (byte)10}; }
            case "CAT" -> { return new byte[]{(byte)8, (byte)9}; }
            case "TAC", "TAT" -> { return new byte[]{(byte)6, (byte)2}; }
            case "TGC", "TGT" -> { return new byte[]{(byte)7, (byte)1}; }
            case "GGA", "GGC", "GGT" -> { return new byte[]{(byte)3, (byte)1}; }
            case "CAC" -> { return new byte[]{(byte)8, (byte)2}; }
            case "ACA", "GCA" -> { return new byte[]{(byte)1, (byte)7}; }
            case "ATC", "ATT", "CTA", "CTC", "CTG", "CTT", "GTC", "GTT", "TTA", "TTG" -> { return new byte[]{(byte)2, (byte)0}; }
            default -> throw new IllegalArgumentException("Invalid codon: " + codon);
        }
    }
}