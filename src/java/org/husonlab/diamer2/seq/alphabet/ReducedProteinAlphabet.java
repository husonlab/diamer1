package org.husonlab.diamer2.seq.alphabet;

/**
 * Interface to represent reduced protein alphabets.
 * <p>This class is used to group the functions needed to convert </p>
 */
public interface ReducedProteinAlphabet extends Alphabet<Short> {
    public short encodeAA(char aa);
    public short[] encodeDNA(String codon);
    public long highestEncoding();
}
