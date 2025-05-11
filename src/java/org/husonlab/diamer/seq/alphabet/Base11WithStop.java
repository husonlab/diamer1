package org.husonlab.diamer.seq.alphabet;

import org.husonlab.diamer.util.logging.Logger;

import java.util.Arrays;

import static org.husonlab.diamer.seq.converter.Utilities.splitAtMinus1;

/**
 * Alphabet for the reduced amino acid alphabet as used in DIAMOND but with stop codons.
 * The stop codons are grouped with the amino acids in group 0.
 */
public class Base11WithStop extends Base11Alphabet {

    private final static Logger logger = new Logger("Base11WithStop");

    @Override
    public String getName() {
        return "Base11WithStop";
    }

    @Override
    public byte[] translateCodon(String codon) {
        switch (codon) {
            case "ATG" -> { return new byte[]{(byte)9, (byte)8}; }
            case "AGG", "CGG" -> { return new byte[]{(byte)0, (byte)4}; }
            case "TAC", "TAT" -> { return new byte[]{(byte)6, (byte)2}; }
            case "AAA", "GAA" -> { return new byte[]{(byte)0, (byte)5}; }
            case "TGG" -> { return new byte[]{(byte)10, (byte)4}; }
            case "TAA", "TAG" -> { return new byte[]{(byte)0, (byte)2}; }
            case "AGA", "CGA", "CGC", "CGT" -> { return new byte[]{(byte)0, (byte)1}; }
            case "CTA", "TTA" -> { return new byte[]{(byte)2, (byte)0}; }
            case "AAC", "AAG", "AAT", "CAA", "CAG", "GAC", "GAG", "GAT" -> { return new byte[]{(byte)0, (byte)2}; }
            case "GGA", "GGC", "GGT" -> { return new byte[]{(byte)3, (byte)1}; }
            case "GGG" -> { return new byte[]{(byte)3, (byte)4}; }
            case "CCA" -> { return new byte[]{(byte)4, (byte)10}; }
            case "TGA" -> { return new byte[]{(byte)0, (byte)1}; }
            case "TTC", "TTT" -> { return new byte[]{(byte)5, (byte)0}; }
            case "TCA" -> { return new byte[]{(byte)1, (byte)0}; }
            case "CAC" -> { return new byte[]{(byte)8, (byte)2}; }
            case "ACA", "GCA" -> { return new byte[]{(byte)1, (byte)7}; }
            case "CAT" -> { return new byte[]{(byte)8, (byte)9}; }
            case "ATC", "ATT", "CTC", "CTG", "CTT", "GTC", "GTT", "TTG" -> { return new byte[]{(byte)2, (byte)0}; }
            case "TGC", "TGT" -> { return new byte[]{(byte)7, (byte)1}; }
            case "ACC", "GCC", "TCC" -> { return new byte[]{(byte)1, (byte)3}; }
            case "CCC" -> { return new byte[]{(byte)4, (byte)3}; }
            case "CCG", "CCT" -> { return new byte[]{(byte)4, (byte)0}; }
            case "ACT", "AGC", "AGT", "GCT" -> { return new byte[]{(byte)1, (byte)1}; }
            case "GTG" -> { return new byte[]{(byte)2, (byte)8}; }
            case "ACG", "GCG", "TCG", "TCT" -> { return new byte[]{(byte)1, (byte)0}; }
            case "ATA", "GTA" -> { return new byte[]{(byte)2, (byte)6}; }
            default -> {
                logger.logWarning("Invalid codon: " + codon);
                return new byte[]{-1, -1};
            }
        }
    }

    /**
     * Translates DNA but only to the first reading frame.
     */
    @Override
    public byte[][] translateDBSequence(char[] seq) {
        if (seq.length < 3) {
            return new byte[0][];
        }
        byte[] translation = new byte[seq.length/3];
        for (int i = 2; i < seq.length; i += 3) {
            // todo: should work without the creation of a string
            String triplet = new String(Arrays.copyOfRange(seq, i-2, i+1));
            byte[] encoding = translateCodon(triplet);
            translation[i/3] = encoding[0];
        }
        return splitAtMinus1(translation);
    }
}
