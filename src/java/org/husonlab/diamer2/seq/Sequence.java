package org.husonlab.diamer2.seq;

import org.husonlab.diamer2.seq.alphabet.Alphabet;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

/**
 * Represents a sequence of symbols of type T.
 * <p>Is meant to be used to instantiate the {@link SequenceRecord} class</p>
 * @param <T> the type of symbols in the sequence
 */
public abstract class Sequence<T> implements Iterable<T> {
    private final Alphabet<T> alphabet;

    public Sequence(Alphabet<T> alphabet) {
        this.alphabet = alphabet;
    }

    @NotNull
    @Override
    public abstract Iterator<T> iterator();

    /**
     * Get the symbol at the given index.
     * @param index the index of the symbol to get
     * @return the symbol at the given index
     */
    public abstract T get(int index);

    /**
     * Get the length of the sequence.
     * @return the length of the sequence
     */
    public abstract int length();

    /**
     * Get the string representation of the sequence.
     * @return the string representation of the sequence
     */
    @Override
    public String toString() {
        return alphabet.toString(this);
    }

    /**
     * Get the alphabet of the sequence.
     * @return the alphabet of the sequence
     */
    public Alphabet<T> getAlphabet() {
        return alphabet;
    }
}
