package org.husonlab.diamer2.seq.alphabet;

public interface ReducedProteinAlphabet {
    public int getBase();
    public short encodeAA(char aa);
    public short[] encodeDNA(String codon);
}
