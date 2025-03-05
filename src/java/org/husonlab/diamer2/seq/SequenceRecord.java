package org.husonlab.diamer2.seq;

import org.husonlab.diamer2.seq.alphabet.Alphabet;
import org.husonlab.diamer2.seq.alphabet.AA;
import org.husonlab.diamer2.seq.alphabet.DNA;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

/**
 * Represents a sequence of symbols of type {@link S} together with a header/id of another type {@link H} e.g. the
 * header string or id of a sequence.
 * <p>The symbol type of the {@link Alphabet} that is associated with the {@link Sequence} must match with {@link S}</p>
 * @param <H> the type of the header/id
 * @param <S> the type of the symbols in the sequence
 * @param <A> the type of the alphabet of the sequence
 */
public record SequenceRecord<H, S, A extends Alphabet<S>>(H id, Sequence<S, A> sequence) implements Iterable<S> {

    /**
     * Create a new Sequence object.
     * @param sequence the sequence of the sequence
     */
    public SequenceRecord {
    }

    /**
     * Get the id of the sequence.
     * @return the id of the sequence
     */
    @Override
    public H id() {
        return id;
    }

    /**
     * Get the sequence.
     * @return the sequence
     */
    public Sequence<S, A> sequence() {
        return sequence;
    }

    public int length() {
        return sequence.length();
    }

    /**
     * Get a string representation of the sequence.
     * @return a string representation of the sequence
     */
    public String getSequenceString() {
        return sequence.toString();
    }

    @Override
    public String toString() {
        return "%s\n%s".formatted(id.toString(), sequence);
    }

    @NotNull
    @Override
    public Iterator<S> iterator() {
        return sequence.iterator();
    }
}
