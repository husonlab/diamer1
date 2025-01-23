package org.husonlab.diamer2.seq;

import org.husonlab.diamer2.seq.alphabet.Alphabet;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

/**
 * Represents a {@link Sequence} of bytes.
 * <p>Can be used for sequences in reduced {@link Alphabet}s, that don't need to be compressed to 4 bits.</p>
 */
public class ByteSequence extends Sequence<Byte> {

    private final byte[] sequence;

    public ByteSequence(Alphabet<Byte> alphabet, byte[] sequence) {
        super(alphabet);
        this.sequence = sequence;
    }

    @NotNull
    @Override
    public Iterator<Byte> iterator() {
        return new Iterator<>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < sequence.length;
            }

            @Override
            public Byte next() {
                return sequence[index++];
            }
        };
    }

    @Override
    public Byte get(int index) {
        return sequence[index];
    }

    @Override
    public int length() {
        return sequence.length;
    }
}
