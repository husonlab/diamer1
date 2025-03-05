package org.husonlab.diamer2.seq;

import org.husonlab.diamer2.seq.alphabet.Alphabet;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

/**
 * Class to represent a sequence of characters.
 * <p>Can be used to represent {@link Sequence}s in the DNA or the amino acid {@link Alphabet}.</p>
 */
public class CharSequence<A extends Alphabet<Character>> extends Sequence<Character, A> {
    private final char[] sequence;

    /**
     * Creates a new {@link CharSequence} with the given {@link Alphabet} and a char array.
     * @param alphabet The {@link Alphabet} of the sequence
     * @param sequence The sequence as a char array
     */
    public CharSequence(A alphabet, char[] sequence) {
        super(alphabet);
        this.sequence = sequence;
    }

    /**
     * Creates a new {@link CharSequence} with the given {@link Alphabet} and a {@link String}.
     * @param alphabet The {@link Alphabet} of the sequence
     * @param sequence The sequence as a {@link String}
     */
    public CharSequence(A alphabet, String sequence) {
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
