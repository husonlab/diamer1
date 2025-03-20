package org.husonlab.diamer2.indexing;

import org.jetbrains.annotations.NotNull;

/**
 * Class that provides methods for the sorting of kmer encodings within buckets, now done in place.
 */
public class Sorting2 {

    /**
     * Sorts a long array by its most significant N bits in place.
     * @param input Array to sort.
     * @param nBits Number of bits to sort by.
     */
    public static void radixInPlace(@NotNull long[] input, int nBits) {
        msdRadix(input, nBits - 1, 0, input.length, 0);
    }

    /**
     * Performs counting sort in place using a 16-bit window of the most significant bits.
     * @param input Array to sort.
     * @param shift Number of bits to shift the mask to the right.
     */
    private static void msdRadix(@NotNull long[] input, int maxShift, int begin, int end, int shift) {
        if (end - begin < 2 || shift > maxShift) {
            return;
        }
        int zerosIndex = begin - 1;
        int onesIndex = end;
        while (onesIndex - zerosIndex > 1) {
            if (getBit(input[zerosIndex + 1], shift)) {
                long temp = input[zerosIndex + 1];
                input[zerosIndex + 1] = input[onesIndex - 1];
                input[onesIndex - 1] = temp;
                onesIndex--;
            } else {
                zerosIndex++;
            }
        }
        msdRadix(input, maxShift, begin, zerosIndex + 1, shift + 1);
        msdRadix(input, maxShift, onesIndex, end, shift + 1);
    }

    /**
     * Returns weather the nth most significant bit is a 1.
     * @param n shift from the left.
     * @return true if the bit at position n from the left is a 1.
     */
    private static boolean getBit(long input, int n) {
        return ((input >>> (63 - n)) & 1) == 1;
    }
}
