package org.husonlab.diamer2.indexing;

import org.husonlab.diamer2.util.FlexibleBucket;
import org.husonlab.diamer2.util.FlexibleDBBucket;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

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

    private static final int SEQUENTIAL_THRESHOLD = 10_000;

    /**
     * Sorts a long array by its most significant N bits in place.
     *
     * @param input Array to sort.
     * @param pool
     */
    public static void radixInPlaceParallel(@NotNull long[] input, int[] ids, ForkJoinPool pool) {
        pool.invoke(new MsdRadixTask(input, ids, 0, input.length, 0));
    }

    public static void radixInPlaceParallel(@NotNull long[] input, int[] ids, int parallelism) {
        radixInPlaceParallel(input, ids, new ForkJoinPool(parallelism));
    }

    public static void radixInPlace(@NotNull long[] input, int[] ids) {
        msdRadix(input, ids, 63, 0, 0);
    }

    public static class MsdRadixTask extends RecursiveAction {

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

        public MsdRadixTask(long[] input, int[] ids) {
            this.input = input;
            this.ids = ids;
            this.begin = 0;
            this.end = input.length;
            this.shift = 0;
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

    public static class MsdRadixTaskFlexibleDBBucket extends RecursiveAction {

        private final FlexibleDBBucket bucket;
        private final int begin;
        private final int end;
        private final int shift;

        public MsdRadixTaskFlexibleDBBucket(FlexibleDBBucket bucket, int begin, int end, int shift) {
            this.bucket = bucket;
            this.begin = begin;
            this.end = end;
            this.shift = shift;
        }

        public MsdRadixTaskFlexibleDBBucket(FlexibleDBBucket bucket) {
            this.bucket = bucket;
            this.begin = 0;
            this.end = bucket.size();
            this.shift = 0;
        }

        @Override
        protected void compute() {
            if (end - begin < SEQUENTIAL_THRESHOLD || shift > 63) {
                msdRadixFlexibleDBBucket(bucket, begin, end, shift);
                return;
            }
            int zerosIndex = begin - 1;
            int onesIndex = end;
            while (onesIndex - zerosIndex > 1) {
                if (getBit(bucket.getValue(zerosIndex + 1), shift)) {
                    long temp = bucket.getValue(zerosIndex + 1);
                    int  tempId = bucket.getId(zerosIndex + 1);
                    bucket.set(zerosIndex + 1, bucket.getValue(onesIndex - 1), bucket.getId(onesIndex - 1));
                    bucket.set(onesIndex - 1, temp, tempId);
                    onesIndex--;
                } else {
                    zerosIndex++;
                }
            }
            MsdRadixTaskFlexibleDBBucket right = new MsdRadixTaskFlexibleDBBucket(bucket, onesIndex, end, shift + 1);
            right.fork();
            new MsdRadixTaskFlexibleDBBucket(bucket, begin, zerosIndex + 1, shift + 1).compute();
            right.join();
        }
    }

    public static class MsdRadixTaskFlexibleBucket extends RecursiveAction {

        private final FlexibleBucket bucket;
        private final int begin;
        private final int end;
        private final int shift;

        public MsdRadixTaskFlexibleBucket(FlexibleBucket bucket, int begin, int end, int shift) {
            this.bucket = bucket;
            this.begin = begin;
            this.end = end;
            this.shift = shift;
        }

        public MsdRadixTaskFlexibleBucket(FlexibleBucket bucket) {
            this.bucket = bucket;
            this.begin = 0;
            this.end = bucket.size();
            this.shift = 0;
        }

        @Override
        protected void compute() {
            if (end - begin < SEQUENTIAL_THRESHOLD || shift > 63) {
                msdRadixFlexibleBucket(bucket, begin, end, shift);
                return;
            }
            int zerosIndex = begin - 1;
            int onesIndex = end;
            while (onesIndex - zerosIndex > 1) {
                if (getBit(bucket.getValue(zerosIndex + 1), shift)) {
                    long temp = bucket.getValue(zerosIndex + 1);
                    bucket.set(zerosIndex + 1, bucket.getValue(onesIndex - 1));
                    bucket.set(onesIndex - 1, temp);
                    onesIndex--;
                } else {
                    zerosIndex++;
                }
            }
            MsdRadixTaskFlexibleBucket right = new MsdRadixTaskFlexibleBucket(bucket, onesIndex, end, shift + 1);
            right.fork();
            new MsdRadixTaskFlexibleBucket(bucket, begin, zerosIndex + 1, shift + 1).compute();
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

    private static void msdRadixFlexibleDBBucket(@NotNull FlexibleDBBucket bucket, int begin, int end, int shift) {
        if (end - begin < 2 || shift > 63) {
            return;
        }
        int zerosIndex = begin - 1;
        int onesIndex = end;
        while (onesIndex - zerosIndex > 1) {
            if (getBit(bucket.getValue(zerosIndex + 1), shift)) {
                long temp = bucket.getValue(zerosIndex + 1);
                int  tempId = bucket.getId(zerosIndex + 1);
                bucket.set(zerosIndex + 1, bucket.getValue(onesIndex - 1), bucket.getId(onesIndex - 1));
                bucket.set(onesIndex - 1, temp, tempId);
                onesIndex--;
            } else {
                zerosIndex++;
            }
        }
        msdRadixFlexibleDBBucket(bucket, begin, zerosIndex + 1, shift + 1);
        msdRadixFlexibleDBBucket(bucket, onesIndex, end, shift + 1);
    }

    private static void msdRadixFlexibleBucket(@NotNull FlexibleBucket bucket, int begin, int end, int shift) {
        if (end - begin < 2 || shift > 63) {
            return;
        }
        int zerosIndex = begin - 1;
        int onesIndex = end;
        while (onesIndex - zerosIndex > 1) {
            if (getBit(bucket.getValue(zerosIndex + 1), shift)) {
                long temp = bucket.getValue(zerosIndex + 1);
                bucket.set(zerosIndex + 1, bucket.getValue(onesIndex - 1));
                bucket.set(onesIndex - 1, temp);
                onesIndex--;
            } else {
                zerosIndex++;
            }
        }
        msdRadixFlexibleBucket(bucket, begin, zerosIndex + 1, shift + 1);
        msdRadixFlexibleBucket(bucket, onesIndex, end, shift + 1);
    }
}