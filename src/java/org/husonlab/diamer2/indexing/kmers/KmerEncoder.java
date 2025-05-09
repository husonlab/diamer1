package org.husonlab.diamer2.indexing.kmers;

import java.util.Arrays;

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
    private final byte[] kmerLetters;
    // array to store whether a letter is part of the kmer to count the number of different letters
    private final boolean[] letterInKmer;
    // array to precalculate and store the letters with base^n
    private final long[][] letterMultiples;
    // array to store the likelihood of each letter of the alphabet in the kmer
    private final double[] letterLikelihoods;
    // flag to indicate wether the mask has spaces or not
    private final boolean hasSpaces;
    // the number a letter has to be multiplied with to shift it to the MSD in the base of the alphabet
    private final long multiplicator;
    // current encoding of the kmer
    private long encoding;

    /**
     * Constructor for the KmerEncoder class.
     * @param base base of the alphabet to encode the kmer in
     * @param mask bitmask
     */
    public KmerEncoder(int base, boolean[] mask, double[] letterLikelihoods) {
        this(base, mask);
        for (int i = 0; i < letterLikelihoods.length; i++) {
            this.letterLikelihoods[i] = letterLikelihoods[i];
        }
    }

    public KmerEncoder(int base, boolean[] mask) {
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
        kmerLetters = new byte[k];
        letterInKmer = new boolean[base];
        letterMultiples = new long[base][k - s];
        for (int i = 0; i < base; i++) {
            for (int j = 0; j < k - s; j++) {
                letterMultiples[i][j] = (long) (i * Math.pow(base, j));
            }
        }
        letterLikelihoods = new double[base];
        Arrays.fill(letterLikelihoods, 1.0 / base);
        hasSpaces = s > 0;
        multiplicator = (long) Math.pow(base, k - s - 1);
        encoding = 0;
    }

    /**
     * Adds a value to the front (left side) of the kmer that corresponds to the most significant bit of the mask.
     * <p>Can be used to extract dna kmers on the reverse strand.</p>
     * <p>Probably less intuitive to use compared with the addBack() method.</p>
     * @param value short to add to the front (left side) of the kmer
     * @return the new encoding of the kmer
     */
    public long addFront(byte value) {
        System.arraycopy(kmerLetters, 0, kmerLetters, 1, k - 1);
        kmerLetters[0] = value;
        encoding = encodeSpaced();
        return encoding;
    }

    /**
     * Adds a value to the back (right side) of the kmer that corresponds to the least significant bit of the mask.
     * @param value short to add to the back (right side) of the kmer
     * @return the new encoding of the kmer
     */
    public long addBack(byte value) {
        byte removedLetter = kmerLetters[0];
        System.arraycopy(kmerLetters, 1, kmerLetters, 0, k - 1);
        kmerLetters[k - 1] = value;
        if (hasSpaces) {
            encoding = encodeSpaced();
        } else {
            encoding = encode(removedLetter);
        }
        return encoding;
    }

    /**
     * Adds all masked elements of the kmer array to get the encoding of the kmer.
     * @return the encoding of the kmer
     */
    private long encodeSpaced() {
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
        } else if (k - s == 14) {
            return letterMultiples[kmerLetters[maskIndices[0]]][13]
                   + letterMultiples[kmerLetters[maskIndices[1]]][12]
                   + letterMultiples[kmerLetters[maskIndices[2]]][11]
                   + letterMultiples[kmerLetters[maskIndices[3]]][10]
                   + letterMultiples[kmerLetters[maskIndices[4]]][9]
                   + letterMultiples[kmerLetters[maskIndices[5]]][8]
                   + letterMultiples[kmerLetters[maskIndices[6]]][7]
                   + letterMultiples[kmerLetters[maskIndices[7]]][6]
                   + letterMultiples[kmerLetters[maskIndices[8]]][5]
                   + letterMultiples[kmerLetters[maskIndices[9]]][4]
                   + letterMultiples[kmerLetters[maskIndices[10]]][3]
                   + letterMultiples[kmerLetters[maskIndices[11]]][2]
                   + letterMultiples[kmerLetters[maskIndices[12]]][1]
                   + letterMultiples[kmerLetters[maskIndices[13]]][0];
        } else if (k - s == 13) {
            return letterMultiples[kmerLetters[maskIndices[0]]][12]
                   + letterMultiples[kmerLetters[maskIndices[1]]][11]
                   + letterMultiples[kmerLetters[maskIndices[2]]][10]
                   + letterMultiples[kmerLetters[maskIndices[3]]][9]
                   + letterMultiples[kmerLetters[maskIndices[4]]][8]
                   + letterMultiples[kmerLetters[maskIndices[5]]][7]
                   + letterMultiples[kmerLetters[maskIndices[6]]][6]
                   + letterMultiples[kmerLetters[maskIndices[7]]][5]
                   + letterMultiples[kmerLetters[maskIndices[8]]][4]
                   + letterMultiples[kmerLetters[maskIndices[9]]][3]
                   + letterMultiples[kmerLetters[maskIndices[10]]][2]
                   + letterMultiples[kmerLetters[maskIndices[11]]][1]
                   + letterMultiples[kmerLetters[maskIndices[12]]][0];
        } else if (k - s == 12) {
            return letterMultiples[kmerLetters[maskIndices[0]]][11]
                   + letterMultiples[kmerLetters[maskIndices[1]]][10]
                   + letterMultiples[kmerLetters[maskIndices[2]]][9]
                   + letterMultiples[kmerLetters[maskIndices[3]]][8]
                   + letterMultiples[kmerLetters[maskIndices[4]]][7]
                   + letterMultiples[kmerLetters[maskIndices[5]]][6]
                   + letterMultiples[kmerLetters[maskIndices[6]]][5]
                   + letterMultiples[kmerLetters[maskIndices[7]]][4]
                   + letterMultiples[kmerLetters[maskIndices[8]]][3]
                   + letterMultiples[kmerLetters[maskIndices[9]]][2]
                   + letterMultiples[kmerLetters[maskIndices[10]]][1]
                   + letterMultiples[kmerLetters[maskIndices[11]]][0];
        } else if (k - s == 11) {
            return letterMultiples[kmerLetters[maskIndices[0]]][10]
                   + letterMultiples[kmerLetters[maskIndices[1]]][9]
                   + letterMultiples[kmerLetters[maskIndices[2]]][8]
                   + letterMultiples[kmerLetters[maskIndices[3]]][7]
                   + letterMultiples[kmerLetters[maskIndices[4]]][6]
                   + letterMultiples[kmerLetters[maskIndices[5]]][5]
                   + letterMultiples[kmerLetters[maskIndices[6]]][4]
                   + letterMultiples[kmerLetters[maskIndices[7]]][3]
                   + letterMultiples[kmerLetters[maskIndices[8]]][2]
                   + letterMultiples[kmerLetters[maskIndices[9]]][1]
                   + letterMultiples[kmerLetters[maskIndices[10]]][0];
        } else {
            long result = 0;
            for (int i = 0, j = maskIndices.length - 1; i < maskIndices.length; i++, j--) {
                result += letterMultiples[kmerLetters[maskIndices[i]]][j];
            }
            return result;
        }
    }

    private long encode(byte removedLetter) {
        return (encoding - removedLetter * multiplicator) * base + kmerLetters[k - 1];
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

    public double getProbability() {
        double likelihood = 1;
        for (int i = 0; i < k; i++) {
            if (mask[i]) {
                likelihood *= letterLikelihoods[kmerLetters[i]];
            }
        }
        return likelihood;
    }

    public int getComplexity() {
        int complexity = 0;
        Arrays.fill(letterInKmer, false);
        for (short kmerLetter : kmerLetters) {
            if (!letterInKmer[kmerLetter]) {
                letterInKmer[kmerLetter] = true;
                complexity++;
            }
        }
        return complexity;
    }
}
