package org.husonlab.diamer2.indexing;

public class Sorting {
    /**
     * Sorts a long array by its first 44 bits.
     * @param input Array to sort.
     * @return Array sorted by the first 44 bits.
     */
    public long[] radixSort(long[] input) {
        final int[] count = new int[2048];
        final long[] output = new long[input.length];
    }

    public long[] countingSort(long[] input, int shift) {
        final int[] count = new int[2048];
        final long[] output = new long[input.length];
        for (long l : input) {
            count[(int) ((l >> shift) & 0b11111111111)]++;
        }
        for (int i = 1; i < count.length; i++) {
            count[i] += count[i - 1];
        }

    }
}
