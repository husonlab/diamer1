package org.husonlab.diamer2.indexing.kmers;

/**
 * Class to encode and update a kmer of the shape defined by a bitmask as a number of a given base.
 * <p>Bytes passed to the addFront or addBack methods get added the respective side of an array initialized with zeros.
 * The elements in that array get updated with each new element to the significance of their current position with
 * respect to the base of the encoding alphabet and the provided mask. The "encoding" of this class always represents
 * the sum of the elements in the array that lie in the mask.</p>
 * <p>The class is most efficient when all kmers of a sequence must be extracted and the individual characters are
 * added one by one to one side of the kmer.</p>
 */
public class KmerEncoder {
    // base of the target system to encode the kmer in
    private final int base;
    // length of the mask (kmer with spaces)
    private final int k;
    // spaces between the bits of the mask
    private final int s;
    // bitmask, most significant bit is the first (leftmost) bit of the mask
    private final boolean[] mask;
    // array with the indices of true values in the mask
    private final int[] maskIndices;
    // array to store the individual letters that are part of the kmer (before multiplication)
    private final short[] kmerLetters;
    // array to precalculate and store the letters with base^n
    private final long[][] letterMultiples;
    // array to store the likelihood of each letter of the alphabet in the kmer
    private final double[] letterLikelihoods;
    // current encoding of the kmer
    private long encoding;

    /**
     * Constructor for the KmerEncoder class.
     * @param base base of the alphabet to encode the kmer in
     * @param mask bitmask
     */
    public KmerEncoder(int base, boolean[] mask, double[] letterLikelihoods) {
        this.base = base;
        // remove trailing zeros (the least significant bits with value 0)
        this.mask = mask;
        // calculate position of the most significant bit (length of the mask / size of the window)
        this.k = mask.length;
        // calculate the number of spaces between the bits of the mask
        int sTemp = 0;
        for (boolean b : mask) {
            if (!b) {
                sTemp++;
            }
        }
        this.maskIndices = new int[k - sTemp];
        for (int i = 0, j = 0; i < k; i++) {
            if (mask[i]) {
                maskIndices[j] = i;
                j++;
            }
        }
        this.s = sTemp;
        kmerLetters = new short[k];
        letterMultiples = new long[base][k - s];
        for (int i = 0; i < base; i++) {
            for (int j = 0; j < k - s; j++) {
                letterMultiples[i][j] = (long) (i * Math.pow(base, j));
            }
        }
        this.letterLikelihoods = letterLikelihoods;
        encoding = 0;
    }

    /**
     * Adds a value to the front (left side) of the kmer that corresponds to the most significant bit of the mask.
     * <p>Can be used to extract dna kmers on the reverse strand.</p>
     * <p>Probably less intuitive to use compared with the addBack() method.</p>
     * @param value short to add to the front (left side) of the kmer
     * @return the new encoding of the kmer
     */
    public long addFront(short value) {
        System.arraycopy(kmerLetters, 0, kmerLetters, 1, k - 1);
        kmerLetters[0] = value;
        encoding = encode();
        return encoding;
    }

    /**
     * Adds a value to the back (right side) of the kmer that corresponds to the least significant bit of the mask.
     * @param value short to add to the back (right side) of the kmer
     * @return the new encoding of the kmer
     */
    public long addBack(short value) {
        System.arraycopy(kmerLetters, 1, kmerLetters, 0, k - 1);
        kmerLetters[k - 1] = value;
        encoding = encode();
        return encoding;
    }

    /**
     * Adds all masked elements of the kmer array to get the encoding of the kmer.
     * @return the encoding of the kmer
     */
    private long encode() {
        if (k - s == 15) {
            return letterMultiples[kmerLetters[maskIndices[0]]][14]
                   + letterMultiples[kmerLetters[maskIndices[1]]][13]
                   + letterMultiples[kmerLetters[maskIndices[2]]][12]
                   + letterMultiples[kmerLetters[maskIndices[3]]][11]
                   + letterMultiples[kmerLetters[maskIndices[4]]][10]
                   + letterMultiples[kmerLetters[maskIndices[5]]][9]
                   + letterMultiples[kmerLetters[maskIndices[6]]][8]
                   + letterMultiples[kmerLetters[maskIndices[7]]][7]
                   + letterMultiples[kmerLetters[maskIndices[8]]][6]
                   + letterMultiples[kmerLetters[maskIndices[9]]][5]
                   + letterMultiples[kmerLetters[maskIndices[10]]][4]
                   + letterMultiples[kmerLetters[maskIndices[11]]][3]
                   + letterMultiples[kmerLetters[maskIndices[12]]][2]
                   + letterMultiples[kmerLetters[maskIndices[13]]][1]
                   + letterMultiples[kmerLetters[maskIndices[14]]][0];
        } else {
            long result = 0;
            for (int i = 0, j = maskIndices.length - 1; i < maskIndices.length; i++, j--) {
                result += letterMultiples[kmerLetters[maskIndices[i]]][j];
            }
            return result;
        }
    }

    /**
     * Resets the kmer and the encoding to zero.
     */
    public void reset() {
        for (int i = 0; i < k; i++) {
            kmerLetters[i] = 0;
        }
        encoding = 0;
    }

    /**
     * @return the base of the encoding alphabet.
     */
    public int getBase() {
        return base;
    }

    /**
     * @return the length of the mask (kmer with spaces).
     */
    public int getK() {
        return k;
    }

    /**
     * @return the number of spaces between the bits of the mask.
     */
    public int getS() {
        return s;
    }

    /**
     * @return the bitmask.
     */
    public boolean[] getMask() {
        return mask;
    }

    /**
     * @return the current encoding of the kmer.
     */
    public long getEncoding() {
        return encoding;
    }

    public double getLikelihood() {
        double likelihood = 1;
        for (int i = 0; i < k; i++) {
            if (mask[i]) {
                likelihood *= letterLikelihoods[kmerLetters[i]];
            }
        }
        return likelihood;
    }
}
