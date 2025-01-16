package org.husonlab.diamer2.seq;

import org.husonlab.diamer2.seq.alphabet.Alphabet;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public class ShortSequence extends Sequence<Short> {

    private final short[] sequence;

    public ShortSequence(Alphabet<Short> alphabet, short[] sequence) {
        super(alphabet);
        this.sequence = sequence;
    }

    @NotNull
    @Override
    public Iterator<Short> iterator() {
        return new Iterator<>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < sequence.length;
            }

            @Override
            public Short next() {
                return sequence[index++];
            }
        };
    }

    @Override
    public Short get(int index) {
        return sequence[index];
    }

    @Override
    public int length() {
        return sequence.length;
    }
}
