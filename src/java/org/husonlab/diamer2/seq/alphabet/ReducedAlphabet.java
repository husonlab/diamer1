package org.husonlab.diamer2.seq.alphabet;

import java.util.BitSet;

public abstract class ReducedAlphabet {

    private int base;
    private int bitsPerElement;

    /**
     * Get the base of the alphabet (e.g. base 11).
     * @return the base of the alphabet
     */
    public int getBase() {
        return base;
    }
    /**
     * Get the number of bits per element (e.g. nucleotide, amino acid).
     * @return the number of bits per element
     */
    public int getBitsPerElement() {
        return bitsPerElement;
    }

    /**
     * Translates and encode a DNA sequence in an array of longs for all 6 reading frames (result[0] ... result[5]).
     * @param sequence the DNA sequence
     * @return an array of longs for all 6 reading frames
     */
    abstract public Long[][] compressDNA(String sequence);

    /**
     * Encode a protein sequence in an array of longs consisting of a compressed bit representation.
     * @param sequence the protein sequence
     * @return an array of longs for the protein sequence
     */
    abstract public Long[] compressProtein(String sequence);
}
