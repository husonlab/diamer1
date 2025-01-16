package org.husonlab.diamer2.seq;

import org.husonlab.diamer2.seq.alphabet.Alphabet;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public class CharSequence extends Sequence<Character> {
    private final char[] sequence;

    public CharSequence(Alphabet<Character> alphabet, char[] sequence) {
        super(alphabet);
        this.sequence = sequence;
    }

    public CharSequence(Alphabet<Character> alphabet, String sequence) {
        super(alphabet);
        this.sequence = sequence.toCharArray();
    }

    @Override
    public Character get(int index) {
        return sequence[index];
    }

    @Override
    public int length() {
        return sequence.length;
    }

    @Override
    public String toString() {
        return new String(sequence);
    }

    @NotNull
    @Override
    public Iterator<Character> iterator() {
        return new Iterator<>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < sequence.length;
            }

            @Override
            public Character next() {
                return sequence[index++];
            }
        };
    }

}
