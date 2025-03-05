package org.husonlab.diamer2.seq.converter;

import org.husonlab.diamer2.seq.Compressed4BitSequence;
import org.husonlab.diamer2.seq.Sequence;
import org.husonlab.diamer2.seq.alphabet.DNA;
import org.husonlab.diamer2.seq.alphabet.Base11Uniform;

import static org.husonlab.diamer2.seq.converter.Utilities.splitAtMinus1AndSizeFilter;

/**
 * Converter to convert DNA to a base 11 alphabet that is designed to have about the same likelihood for each of the 11
 * target amino acids. The conversion is done for all 6 reading frames and all translations are split at stop codons.
 */
public class DNAtoBase11Uniform extends Converter<Character, DNA, Byte, Base11Uniform> {

    private static final Base11Uniform TARGET_ALPHABET = new Base11Uniform();
    private final int minLength;

    public DNAtoBase11Uniform(int minLength) {
        this.minLength = minLength;
    }

    @Override
    public Sequence<Byte, Base11Uniform>[] convert(Sequence<Character, DNA> sequence) {

        // The sequence is too short to be translated
        if (sequence.length() < 3) {
            return new Sequence[0];
        }
        StringBuilder triplet = new StringBuilder();
        // Setup byte array for each reading frame
        byte[][] translations = new byte[6][];
        int[] sequenceLengths = new int[3];
        for (int i = 0; i < 3; i++) {
            int len = (sequence.length()-i)/3;
            sequenceLengths[i] = len;
            translations[i*2] = new byte[len];
            translations[i*2+1] = new byte[len];
        }
        triplet.append(sequence.get(0)).append(sequence.get(1));

        for (int i = 2; i < sequence.length(); i++) {
            triplet.append(sequence.get(i));
            byte[] encoding = encodeDNA(triplet.toString());
            int i2 = i*2-4;
            // Forward reading frame
            translations[i2%6][i2/6] = encoding[0];
            // Reverse reading frame, gets filled in reverse order
            translations[(i2%6)+1][sequenceLengths[(i+1)%3]-i2/6-1] = encoding[1];
            triplet.deleteCharAt(0);
        }
        Byte[][] splitTranslations;

        if (sequence.length() == 3) {
            splitTranslations = splitAtMinus1AndSizeFilter(new byte[][]{
                    translations[0],
                    translations[1]}, minLength);
        }
        else if (sequence.length() == 4) {
            splitTranslations = splitAtMinus1AndSizeFilter(new byte[][]{
                    translations[0],
                    translations[1],
                    translations[2],
                    translations[3]}, minLength);
        } else {
            splitTranslations = splitAtMinus1AndSizeFilter(new byte[][]{
                    translations[0],
                    translations[1],
                    translations[2],
                    translations[3],
                    translations[4],
                    translations[5]}, minLength);
        }
        Sequence<Byte, Base11Uniform>[] result = new Sequence[splitTranslations.length];
        for (int i = 0; i < splitTranslations.length; i++) {
            result[i] = new Compressed4BitSequence<>(TARGET_ALPHABET, splitTranslations[i]);
        }
        return result;
    }

    public byte[] encodeDNA(String codon) {
        switch (codon) {
            case "GTC", "GTT" -> { return new byte[]{(byte)3, (byte)8}; }
            case "ATA" -> { return new byte[]{(byte)9, (byte)6}; }
            case "ACG" -> { return new byte[]{(byte)6, (byte)7}; }
            case "TTC" -> { return new byte[]{(byte)9, (byte)5}; }
            case "TAT" -> { return new byte[]{(byte)6, (byte)9}; }
            case "ATC", "ATT" -> { return new byte[]{(byte)9, (byte)8}; }
            case "AGG", "CGG" -> { return new byte[]{(byte)7, (byte)10}; }
            case "TAC" -> { return new byte[]{(byte)6, (byte)3}; }
            case "CTT" -> { return new byte[]{(byte)0, (byte)10}; }
            case "CTC" -> { return new byte[]{(byte)0, (byte)5}; }
            case "ACA", "ACC" -> { return new byte[]{(byte)6, (byte)2}; }
            case "CTG", "TTG" -> { return new byte[]{(byte)0, (byte)7}; }
            case "ACT" -> { return new byte[]{(byte)6, (byte)4}; }
            case "AAG" -> { return new byte[]{(byte)10, (byte)0}; }
            case "CCA" -> { return new byte[]{(byte)10, (byte)3}; }
            case "CTA", "TTA" -> { return new byte[]{(byte)0, (byte)-1}; }
            case "CCC" -> { return new byte[]{(byte)10, (byte)2}; }
            case "CCG", "CCT" -> { return new byte[]{(byte)10, (byte)7}; }
            case "TGA" -> { return new byte[]{(byte)-1, (byte)4}; }
            case "GGG" -> { return new byte[]{(byte)2, (byte)10}; }
            case "TCA" -> { return new byte[]{(byte)4, (byte)-1}; }
            case "AAA" -> { return new byte[]{(byte)10, (byte)9}; }
            case "GTG" -> { return new byte[]{(byte)3, (byte)4}; }
            case "TTT" -> { return new byte[]{(byte)9, (byte)10}; }
            case "GTA" -> { return new byte[]{(byte)3, (byte)6}; }
            case "TAA", "TAG" -> { return new byte[]{(byte)-1, (byte)0}; }
            case "GAG" -> { return new byte[]{(byte)5, (byte)0}; }
            case "AAC", "GAC" -> { return new byte[]{(byte)8, (byte)3}; }
            case "GGA" -> { return new byte[]{(byte)2, (byte)4}; }
            case "GGC", "TGC" -> { return new byte[]{(byte)2, (byte)1}; }
            case "GCG" -> { return new byte[]{(byte)1, (byte)7}; }
            case "AAT", "GAT" -> { return new byte[]{(byte)8, (byte)9}; }
            case "ATG" -> { return new byte[]{(byte)5, (byte)4}; }
            case "TCC" -> { return new byte[]{(byte)4, (byte)2}; }
            case "CGC" -> { return new byte[]{(byte)7, (byte)1}; }
            case "GCT" -> { return new byte[]{(byte)1, (byte)4}; }
            case "CAC" -> { return new byte[]{(byte)4, (byte)3}; }
            case "TGG" -> { return new byte[]{(byte)3, (byte)10}; }
            case "GCA", "GCC" -> { return new byte[]{(byte)1, (byte)2}; }
            case "CAA", "CAG" -> { return new byte[]{(byte)7, (byte)0}; }
            case "AGC" -> { return new byte[]{(byte)4, (byte)1}; }
            case "GAA" -> { return new byte[]{(byte)5, (byte)9}; }
            case "AGT" -> { return new byte[]{(byte)4, (byte)6}; }
            case "TCG", "TCT" -> { return new byte[]{(byte)4, (byte)7}; }
            case "GGT", "TGT" -> { return new byte[]{(byte)2, (byte)6}; }
            case "CAT" -> { return new byte[]{(byte)4, (byte)5}; }
            case "CGT" -> { return new byte[]{(byte)7, (byte)6}; }
            case "AGA", "CGA" -> { return new byte[]{(byte)7, (byte)4}; }
            default -> throw new IllegalArgumentException("Invalid codon: " + codon);
        }
    }
}
