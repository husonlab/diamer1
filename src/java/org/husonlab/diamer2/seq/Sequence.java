package org.husonlab.diamer2.seq;

import org.husonlab.diamer2.seq.alphabet.Alphabet;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

/**
 * Represents a sequence of symbols of type T.
 * <p>Can be used together with the {@link SequenceRecord} class to combine a sequence with a header/id.</p>
 * @param <S> the type of symbols in the sequence
 * @param <A> the type of alphabet used for the sequence
 */
public abstract class Sequence<S, A extends Alphabet<S>> implements Iterable<S> {
    private final A alphabet;

    public Sequence(A alphabet) {
        this.alphabet = alphabet;
    }

    @NotNull
    @Override
    public abstract Iterator<S> iterator();

    /**
     * Get the symbol at the given index.
     * @param index the index of the symbol to get
     * @return the symbol at the given index
     */
    public abstract S get(int index);

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
    public Alphabet<S> getAlphabet() {
        return alphabet;
    }
}
