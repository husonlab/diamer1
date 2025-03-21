import static org.husonlab.diamer2.indexing.Sorting.radixSortNBits;
import org.junit.Test;

import java.util.concurrent.ThreadLocalRandom;

import static org.husonlab.diamer2.indexing.Sorting2.radixInPlaceParallel;
import static org.junit.Assert.*;

public class SortingTest {
    @Test
    public void testRadixSortNBits() {
        long[] input = new long[]{
                //1111111111111111111111111111111111111111110000000000000000000000
                0b0000000000000001000000000000000000000000000001010000000000000000L,
                0b0000000000000000100000000000000000000000000001010000000000000000L,
                0b0000000000000000000000000000000100010000000100010000000000000000L,
                0b1100000000000000000000000000000100010000000100010000000000000000L,
                0b1100000000000000000000000000000100010000000100110000000000000000L,
                0b1100000000000000000000000000000100010000000100010000000000000000L,
                0b1000000000000000000000000000000100010000000100010000000000000000L,
                0b0000000000000000000000000000000000000001110000010000000000000000L,
                0b0000000000000000000000000000000000000000000000110000000000000000L,
                0b0000000000000000000000000000000000000000000000101111111111101111L,
                0b0000000000000000000000000000000000000000000000101000000000000000L,
                0b0000000000000000000000000000000000000000000000011111100011111111L,
                0b0000000000000000000000000000000000000000000000010001111111111000L,
                0b0000000000000000000000000000000000000000000000010000001111111110L,
                0b0000000000000000000000000000000000000000000000000000000000000000L,
                0b0000000000000000000000000000000000000000000000010000000000000000L,
                0b0000000000000000000000000000000000000000000000000000000000000000L
        };
        long[] expected = new long[]{
                0b0L,
                0b0L,
                0b11111100011111111L,
                0b10001111111111000L,
                0b10000001111111110L,
                0b10000000000000000L,
                0b101111111111101111L,
                0b101000000000000000L,
                0b110000000000000000L,
                0b1110000010000000000000000L,
                0b100010000000100010000000000000000L,
                0b100000000000000000000000000001010000000000000000L,
                0b1000000000000000000000000000001010000000000000000L,
                0b1000000000000000000000000000000100010000000100010000000000000000L,
                0b1100000000000000000000000000000100010000000100010000000000000000L,
                0b1100000000000000000000000000000100010000000100010000000000000000L,
                0b1100000000000000000000000000000100010000000100110000000000000000L,
        };
        for (long l : radixSortNBits(input, 44)) {
            System.out.println(Long.toBinaryString(l));
        }
        assertArrayEquals(expected, radixSortNBits(input, 44));
    }

    @Test
    public void compare() {
        int size = 300_000_000;
        long[] input = new long[size];
        int[] ids = new int[size];
        for (int i = 0; i < input.length; i++) {
            input[i] = ThreadLocalRandom.current().nextLong() >>> 1;
            ids[i] = (int) (input[i] >>> 32);
        }
//        input[0] = 0;
//        input[1] = 2;
//        input[2] = 1;
//        input[3] = 3;
//        input[4] = 0;
//        input[5] = 4;
//        input[6] = 0;

        long start = System.currentTimeMillis();
        long[] result = radixSortNBits(input, 64);
        System.out.println("Time: " + (System.currentTimeMillis() - start));
        for (int i = 1; i < result.length; i++) {
//            System.out.println(result[i] + " " + result[i - 1]);
            assertTrue(result[i] >= result[i - 1]);
        }

        start = System.currentTimeMillis();
        radixInPlaceParallel(input, ids, 12);
        System.out.println("Time inplace parallel: " + (System.currentTimeMillis() - start));
        for (int i = 1; i < input.length; i++) {
            assertTrue(input[i] >= input[i - 1]);
            assertEquals(input[i], result[i]);
        }
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] != (int) (input[i] >>> 32)) {
                System.out.println(ids[i] + " " + (int) (input[i] >>> 32));
            }
            assertTrue(ids[i] ==  (int) (input[i] >>> 32));
        }
    }
}
