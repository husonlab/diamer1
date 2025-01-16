package org.husonlab.diamer2.seq.alphabet;

public interface ReducedProteinAlphabet extends Alphabet<Short>{
    public short encodeAA(char aa);
    public short[] encodeDNA(String codon);
    public long highestEncoding();
}
