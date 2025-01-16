package org.husonlab.diamer2.seq.kmers;

import java.util.LinkedList;

public class KmerEncoder {
    private final int base;
    // length of the mask (kmer with spaces)
    private final int k;
    // spaces between the bits of the mask
    private final int s;
    private final long mask;
    private final LinkedList<Integer> kmer = new LinkedList<>();
    private long encoding;

    public KmerEncoder(int base, long mask) {
        this.base = base;
        // remove trailing zeros (the least significant bits with value 0)
        this.mask = mask / Long.lowestOneBit(mask);
        // calculate position of the most significant bit (length of the mask / size of the window)
        this.k = Long.SIZE - Long.numberOfLeadingZeros(this.mask);
        // calculate the number of spaces between the bits of the mask
        this.s = k - Long.bitCount(this.mask);
        encoding = 0;
    }

    public long addFront(int value) {
        if (kmer.size() == k) {
            kmer.pollLast();
            kmer.addFirst(value);
        } else {
            kmer.addFirst(value);
        }
        encoding = encode();
        return encoding;
    }

    public long addBack(int value) {
        if (kmer.size() == k) {
            kmer.pollFirst();
            kmer.addLast(value);
        } else {
            kmer.addLast(value);
        }
        encoding = encode();
        return encoding;
    }

    private long encode() {
        long result = 0;
        int i = k - 1;
        int j = k - s - 1;
        for (int value : kmer) {
            if (inMask(i--)) {
                result += Math.pow(base, j--) * value;
            }
        }
        return result;
    }

    private boolean inMask(int value) {
        return (mask & (1 << value)) != 0;
    }

    public void reset() {
        kmer.clear();
        encoding = 0;
    }

    public int getBase() {
        return base;
    }

    public int getK() {
        return k;
    }

    public int getS() {
        return s;
    }

    public long getMask() {
        return mask;
    }

    public long getEncoding() {
        return encoding;
    }
}
