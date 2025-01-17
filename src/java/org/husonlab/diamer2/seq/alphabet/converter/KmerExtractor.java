package org.husonlab.diamer2.seq.alphabet.converter;

import java.util.LinkedList;

public class KmerExtractor<T> {
    private final int k;
    private final LinkedList<T> kmer = new LinkedList<>();

    public KmerExtractor(long mask) {
        // remove trailing zeros (the least significant bits with value 0)
        long mask1 = mask / Long.lowestOneBit(mask);
        // calculate position of the most significant bit (length of the mask / size of the window)
        this.k = Long.SIZE - Long.numberOfLeadingZeros(mask1);
    }

    public T[] addFront(T value, T[] result) {
        if (kmer.size() == k) {
            kmer.pollLast();
            kmer.addFirst(value);
        } else {
            kmer.addFirst(value);
        }
        return kmer.toArray(result);
    }

    public T[] addBack(T value, T[] result) {
        if (kmer.size() == k) {
            kmer.pollFirst();
            kmer.addLast(value);
        } else {
            kmer.addLast(value);
        }
        return kmer.toArray(result);
    }
}
