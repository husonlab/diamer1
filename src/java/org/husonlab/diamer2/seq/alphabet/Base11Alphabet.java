package org.husonlab.diamer2.seq.alphabet;

import org.husonlab.diamer2.util.logging.Logger;

/**
 * Alphabet for the reduced amino acid alphabet as used in DIAMOND. There is no symbol for stop codons.
 */
public class Base11Alphabet extends ReducedAlphabet {

    private static final Logger logger = new Logger("Base11Alphabet");

    /**
     * @param bits length of a kmer
     * @return highest number that can occur when a kmer with the given length is converted to a number.
     */
    @Deprecated
    public long highestEncoding(int bits) {
        return (long)Math.pow(getBase(), bits) - 1L;
    }

    @Override
    public int getBase() {
        return 11;
    }

    @Override
    public boolean contains(Byte symbol) {
        return symbol >= 0 && symbol <= 10;
    }

    @Override
    public Byte[] getSymbols() {
        return new Byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    }

    @Override
    public String getName() {
        return "Base 11 Alphabet";
    }

    @Override
    public String toString(Iterable<Byte> seq) {
        StringBuilder sb = new StringBuilder();
        for (Byte symbol : seq) {
            sb.append(symbol).append(" ");
        }
        return sb.toString();
    }

    @Override
    public byte[] translateCodon(String codon) {
        // todo: include other letters (as N) if they allow an unambiguous translation in the specific codon
        switch (codon) {
            case "ATG" -> { return new byte[]{(byte)9, (byte)8}; }
            case "AGG", "CGG" -> { return new byte[]{(byte)0, (byte)4}; }
            case "TAC", "TAT" -> { return new byte[]{(byte)6, (byte)2}; }
            case "AAA", "GAA" -> { return new byte[]{(byte)0, (byte)5}; }
            case "TGG" -> { return new byte[]{(byte)10, (byte)4}; }
            case "TAA", "TAG" -> { return new byte[]{(byte)-1, (byte)2}; }
            case "AGA", "CGA", "CGC", "CGT" -> { return new byte[]{(byte)0, (byte)1}; }
            case "CTA", "TTA" -> { return new byte[]{(byte)2, (byte)-1}; }
            case "AAC", "AAG", "AAT", "CAA", "CAG", "GAC", "GAG", "GAT" -> { return new byte[]{(byte)0, (byte)2}; }
            case "GGA", "GGC", "GGT" -> { return new byte[]{(byte)3, (byte)1}; }
            case "GGG" -> { return new byte[]{(byte)3, (byte)4}; }
            case "CCA" -> { return new byte[]{(byte)4, (byte)10}; }
            case "TGA" -> { return new byte[]{(byte)-1, (byte)1}; }
            case "TTC", "TTT" -> { return new byte[]{(byte)5, (byte)0}; }
            case "TCA" -> { return new byte[]{(byte)1, (byte)-1}; }
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

    @Override
    public byte translateAA(char aa) {
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
            default -> {
                logger.logWarning("Invalid amino acid: " + aa);
                return -1;
            }
        }
    }
}
