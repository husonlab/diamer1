package org.husonlab.diamer2.indexing;

import org.jetbrains.annotations.NotNull;

/**
 * Class that provides methods for the sorting of kmer encodings within buckets.
 */
public class Sorting {

    /**
     * Sorts a long array by its most significant N bits.
     * <p>
     *     Actually, N is rounded up to the next multiple of 16, so the array might be sorted by more than only the most
     *     significant N bits.
     * </p>
     * @param input Array to sort.
     * @param nBits Number of bits to sort by.
     * @return Array sorted by the most significant N bits.
     */
    @NotNull
    public static long[] radixSortNBits(@NotNull long[] input, int nBits) {
        int shift = Math.ceilDiv(nBits, 16) - 1;
        for (int i = shift; i >= 0; i--) {
            input = countingSort(input, i*16);
        }
        return input;
    }

    /**
     * Sorts a long array by a 16 bit window of the most significant bits. The window can be shifted to the right.
     * @param input Array to sort.
     * @return Sorted array.
     */
    @NotNull
    public static long[] countingSort(@NotNull long[] input, int shift) {
        final int[] count = new int[65536];
        final long[] output = new long[input.length];

        // count occurrences
        for (long l : input) {
            count[applyMask(l, shift)]++;
        }

        // compute prefix sums
        for (int i = 1; i < count.length; i++) {
            count[i] += count[i - 1];
        }

        // sorting
        for (int i = input.length - 1; i >= 0; i--) {
            output[--count[applyMask(input[i], shift)]] = input[i];
        }
        return output;
    }

    /**
     * Applies a mask of length 16 bits to the most significant bits of a long. The mask can be shifted to the right.
     * @param input Long to apply the mask to.
     * @param shift Number*16 to shift the mask to the right.
     * @return Integer that corresponds to the masked bits.
     */
    private static int applyMask(long input, int shift) {
        return (int) ((input >>> (48 - shift)) & 0b1111111111111111);
    }
}