package org.husonlab.diamer2.indexing;

import org.jetbrains.annotations.NotNull;

public class Sorting {
    /**
     * Sorts a long array by its first 44 bits.
     * @param input Array to sort.
     * @return Array sorted by the first 44 bits.
     */
    @NotNull
    public static long[] radixSort44bits(@NotNull long[] input) {
        for (int i = 0; i < 4; i++) {
            input = countingSort(input, i);
        }
        return input;
    }

    /**
     * Sorts a long array by its first 44 bits.
     * @param input Array to sort.
     * @return Array sorted by the first 44 bits.
     */
    @NotNull
    public static long[] radixSort44bits(@NotNull long[] input, boolean pseudoInPlace) {
        for (int i = 0; i < 4; i++) {
            input = countingSort(input, i);
        }
        return input;
    }

    /**
     * Sorts a long array by its first 11 bits.
     * @param input Array to sort.
     * @return Array sorted by the first 11 bits.
     */
    @NotNull
    public static long[] countingSort(@NotNull long[] input, int shift) {
        final int[] count = new int[2048];
        final long[] output = new long[input.length];
        for (long l : input) {
            count[applyMask(l, shift)]++;
        }
        for (int i = 1; i < count.length; i++) {
            count[i] += count[i - 1];
        }
        for (int i = input.length - 1; i >= 0; i--) {
            output[--count[applyMask(input[i], shift)]] = input[i];
        }
        return output;
    }

    /**
     * Applies a mask of length 11 bits to a long. The first 20 bits are ignored. The mask can be shifted to the left.
     * @param input Long to apply the mask to.
     * @param shift Number*11 to shift the mask to the left.
     * @return Integer that corresponds to the masked bits.
     */
    private static int applyMask(long input, int shift) {
        return (int) ((input >> (shift * 11 + 20)) & 0b11111111111);
    }
}
