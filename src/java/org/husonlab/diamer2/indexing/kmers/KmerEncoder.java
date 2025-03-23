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
    // array to store the addends of the encoding
    private final long[] kmer;
    // array to store the individual letters that are part of the kmer (before multiplication)
    private final short[] kmerLetters;
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
        this.s = sTemp;
        kmer = new long[k];
        for (int i = 0; i < k; i++) {
            kmer[i] = 0;
        }
        kmerLetters = new short[k];
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
        int first = (int)(value * Math.pow(base, k - s - 1));
        System.arraycopy(kmer, 0, kmer, 1, k - 1);
        System.arraycopy(kmerLetters, 0, kmerLetters, 1, k - 1);
        kmer[0] = 0;
        kmerLetters[0] = value;
        divide();
        kmer[0] = first;
        encoding = encode();
        return encoding;
    }

    /**
     * Adds a value to the back (right side) of the kmer that corresponds to the least significant bit of the mask.
     * @param value short to add to the back (right side) of the kmer
     * @return the new encoding of the kmer
     */
    public long addBack(short value) {
        int last = value;
        System.arraycopy(kmer, 1, kmer, 0, k - 1);
        System.arraycopy(kmerLetters, 1, kmerLetters, 0, k - 1);
        kmer[k - 1] = 0;
        kmerLetters[k - 1] = value;
        multiply();
        kmer[k - 1] = last;
        encoding = encode();
        return encoding;
    }

    /**
     * Multiplies each element in the kmer array that is in the mask by the base of the alphabet.
     * <p>Used to update the encoding of the kmer after adding a value to the back (right side) of the kmer.</p>
     */
    private void multiply() {
        for (int i = 0; i < k; i++) {
            if (mask[i]) {
                kmer[i] *= base;
            }
        }
    }

    /**
     * Divides each element in the kmer array that is in the mask by the base of the alphabet.
     * <p>Used to update the encoding of the kmer after adding a value to the front (left side) of the kmer.</p>
     */
    private void divide() {
        for (int i = 0; i < k; i++) {
            if (mask[i]) {
                kmer[i] /= base;
            }
        }
    }

    /**
     * Adds all masked elements of the kmer array to get the encoding of the kmer.
     * @return the encoding of the kmer
     */
    private long encode() {
        long result = 0;
        for (int i = 0; i < k; i++) {
            if (mask[i]) {
                result += kmer[i];
            }
        }
        return result;
    }

    /**
     * Resets the kmer and the encoding to zero.
     */
    public void reset() {
        for (int i = 0; i < k; i++) {
            kmer[i] = 0;
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
