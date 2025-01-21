package org.husonlab.diamer2.seq.alphabet.converter;

import org.husonlab.diamer2.seq.Sequence;
import org.husonlab.diamer2.seq.ShortSequence;
import org.husonlab.diamer2.seq.alphabet.Alphabet;
import org.husonlab.diamer2.seq.alphabet.AlphabetDNA;
import org.husonlab.diamer2.seq.alphabet.Base11Alphabet;
import org.husonlab.diamer2.seq.alphabet.Utilities;

import java.util.HashMap;
import java.util.LinkedList;

public class DNAtoBase11 implements Converter<Character, Short> {

    private static final Alphabet<Character> SOURCE_ALPHABET = new AlphabetDNA();
    private static final Alphabet<Short> TARGET_ALPHABET = new Base11Alphabet();

    @Override
    public Sequence<Short>[] convert(Sequence<Character> sequence) {

        if (sequence.length() < 3) {
            return new Sequence[0];
        }
        StringBuilder triplet = new StringBuilder();
        short[][] translations = new short[6][];
        for (int i = 0; i < 3; i++) {
            translations[i*2] = new short[(sequence.length()-i)/3];
            translations[i*2+1] = new short[(sequence.length()-i)/3];
        }
        triplet.append(sequence.get(0)).append(sequence.get(1));

        for (int i = 2; i < sequence.length(); i++) {
            triplet.append(sequence.get(i));
            short[] encoding = encodeDNA(triplet.toString());
            int i2 = i*2-4;
            translations[i2%6][i2/6] = encoding[0];
            translations[(i2%6)+1][i2/6] = encoding[1];
            triplet.deleteCharAt(0);
        }
        if (sequence.length() == 3) {
            return new Sequence[]{
                    new ShortSequence(new Base11Alphabet(), translations[0]),
                    new ShortSequence(new Base11Alphabet(), translations[1])};
        }
        if (sequence.length() == 4) {
            return new Sequence[]{
                    new ShortSequence(new Base11Alphabet(), translations[0]),
                    new ShortSequence(new Base11Alphabet(), translations[1]),
                    new ShortSequence(new Base11Alphabet(), translations[2]),
                    new ShortSequence(new Base11Alphabet(), translations[3])};
        }
        return new Sequence[]{
                new ShortSequence(new Base11Alphabet(), translations[0]),
                new ShortSequence(new Base11Alphabet(), translations[1]),
                new ShortSequence(new Base11Alphabet(), translations[2]),
                new ShortSequence(new Base11Alphabet(), translations[3]),
                new ShortSequence(new Base11Alphabet(), translations[4]),
                new ShortSequence(new Base11Alphabet(), translations[5])};
    }

    @Override
    public Alphabet<Character> getSourceAlphabet() {
        return SOURCE_ALPHABET;
    }

    @Override
    public Alphabet<Short> getTargetAlphabet() {
        return TARGET_ALPHABET;
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
