package org.husonlab.diamer2.seq;

import org.husonlab.diamer2.seq.alphabet.Alphabet;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

/**
 * Represents a {@link Sequence} of numbers that only require a 4 bit encoding. Used for reduced alphabets.
 * The sequences are compressed in a long array (to save memory) and extracted when needed. Numbers from -1 to 14 can
 * be stored in this sequence.
 * @param <A> The alphabet of the sequence
 */
public class Compressed4BitSequence<A extends Alphabet<Byte>> extends Sequence<Byte, A> {

    private final long[] sequence;
    private final int length;

    public Compressed4BitSequence(A alphabet, byte[] sequence) {
        super(alphabet);
        int longArrayLength = (sequence.length + 15) / 16;
        this.sequence = new long[longArrayLength];
        this.length = sequence.length;

        // compresses the first 4 bits of each byte into a long array
        for (int i = 0; i < sequence.length; i++) {
            int longIndex = i / 16;
            int byteIndex = i % 16;
            this.sequence[longIndex] |= ((long) ((sequence[i] + 1) & 0x0F)) << (byteIndex * 4);
        }
    }

    public Compressed4BitSequence(A alphabet, Byte[] sequence) {
        super(alphabet);
        int longArrayLength = (sequence.length + 15) / 16;
        this.sequence = new long[longArrayLength];
        this.length = sequence.length;

        // compresses the first 4 bits of each byte into a long array
        for (int i = 0; i < sequence.length; i++) {
            int longIndex = i / 16;
            int byteIndex = i % 16;
            this.sequence[longIndex] |= ((long) ((sequence[i] + 1) & 0x0F)) << (byteIndex * 4);
        }
    }

    @NotNull
    @Override
    public Iterator<Byte> iterator() {
        return new Iterator<>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < length();
            }

            @Override
            public Byte next() {
                int longIndex = index / 16;
                int byteIndex = index % 16;
                index++;
                return (byte) (((sequence[longIndex] >> (byteIndex * 4)) & 0x0F) - 1);
            }
        };
    }

    @Override
    public Byte get(int index) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for sequence of length " + length);
        }
        int longIndex = index / 16;
        int byteIndex = index % 16;
        return (byte) (((sequence[longIndex] >> (byteIndex * 4)) & 0x0F) - 1);
    }

    @Override
    public int length() {
        return length;
    }
}
