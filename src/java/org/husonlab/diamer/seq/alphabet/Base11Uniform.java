package org.husonlab.diamer.seq.alphabet;

import org.husonlab.diamer.util.logging.Logger;

public class Base11Uniform extends Base11Alphabet {
    private final static Logger logger = new Logger("Base11Uniform");

    @Override
    public String getName() {
        return "Base11Uniform";
    }

    @Override
    public byte[] translateCodon(String codon) {
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
            default -> {
                logger.logWarning("Invalid codon: " + codon);
                return new byte[]{-1, -1};
            }
        }
    }

    @Override
    public byte translateAA(char aa) {
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
            default -> {
                logger.logWarning("Invalid amino acid: " + aa);
                return -1;
            }
        }
    }
}
