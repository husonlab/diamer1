package org.husonlab.diamer2.indexing;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

/**
 * Class that provides methods for the sorting of kmer encodings within buckets, now done in place.
 */
public class Sorting2 {

    private static final int SEQUENTIAL_THRESHOLD = 10_000;

    /**
     * Sorts a long array by its most significant N bits in place.
     * @param input Array to sort.
     */
    public static void radixInPlaceParallel(@NotNull long[] input, int[] ids, int parallelism) {
        try (ForkJoinPool pool = new ForkJoinPool(parallelism)) {
            pool.invoke(new MsdRadixTask(input, ids, 0, input.length, 0));
        }
    }

    public static void radixInPlace(@NotNull long[] input, int[] ids) {
        msdRadix(input, ids, 63, 0, 0);
    }

    private static class MsdRadixTask extends RecursiveAction {

        private final long[] input;
        private final int[] ids;
        private final int begin;
        private final int end;
        private final int shift;

        public MsdRadixTask(long[] input, int[] ids, int begin, int end, int shift) {
            this.input = input;
            this.ids = ids;
            this.begin = begin;
            this.end = end;
            this.shift = shift;
        }

        @Override
        protected void compute() {
            if (end - begin < SEQUENTIAL_THRESHOLD || shift > 63) {
                msdRadix(input, ids, begin, end, shift);
                return;
            }
            int zerosIndex = begin - 1;
            int onesIndex = end;
            while (onesIndex - zerosIndex > 1) {
                if (getBit(input[zerosIndex + 1], shift)) {
                    long temp = input[zerosIndex + 1];
                    int  tempId = ids[zerosIndex + 1];
                    input[zerosIndex + 1] = input[onesIndex - 1];
                    ids[zerosIndex + 1] = ids[onesIndex - 1];
                    input[onesIndex - 1] = temp;
                    ids[onesIndex - 1] = tempId;
                    onesIndex--;
                } else {
                    zerosIndex++;
                }
            }
            MsdRadixTask right = new MsdRadixTask(input, ids, onesIndex, end, shift + 1);
            right.fork();
            new MsdRadixTask(input, ids, begin, zerosIndex + 1, shift + 1).compute();
            right.join();
        }
    }

    /**
     * Return weather the nth most significant bit is a 1.
     * @param n shift from the left.
     * @return true if the bit at position n from the left is a 1.
     */
    private static boolean getBit(long input, int n) {
        return ((input >>> (63 - n)) & 1) == 1;
    }

    /**
     * Performs counting sort in place using a 16-bit window of the most significant bits.
     * @param input Array to sort.
     * @param shift Number of bits to shift the mask to the right.
     */
    private static void msdRadix(@NotNull long[] input, int[] ids, int begin, int end, int shift) {
        if (end - begin < 2 || shift > 63) {
            return;
        }
        int zerosIndex = begin - 1;
        int onesIndex = end;
        while (onesIndex - zerosIndex > 1) {
            if (getBit(input[zerosIndex + 1], shift)) {
                long temp = input[zerosIndex + 1];
                int  tempId = ids[zerosIndex + 1];
                input[zerosIndex + 1] = input[onesIndex - 1];
                ids[zerosIndex + 1] = ids[onesIndex - 1];
                input[onesIndex - 1] = temp;
                ids[onesIndex - 1] = tempId;
                onesIndex--;
            } else {
                zerosIndex++;
            }
        }
        msdRadix(input, ids, begin, zerosIndex + 1, shift + 1);
        msdRadix(input, ids, onesIndex, end, shift + 1);
    }
}
