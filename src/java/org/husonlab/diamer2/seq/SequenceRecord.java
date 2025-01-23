package org.husonlab.diamer2.seq;

import org.husonlab.diamer2.seq.alphabet.Alphabet;
import org.husonlab.diamer2.seq.alphabet.AlphabetAA;
import org.husonlab.diamer2.seq.alphabet.AlphabetDNA;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

/**
 * Represents a sequence of symbols of type {@link S} together with a header/id of another type {@link H} e.g. the
 * header string or id of a sequence.
 * <p>The symbol type of the {@link Alphabet} that is associated with the {@link Sequence} must match with {@link S}</p>
 */
public record SequenceRecord<H, S>(H id, Sequence<S> sequence) implements Iterable<S> {

    /**
     * Create a new Sequence object.
     * @param sequence the sequence of the sequence
     */
    public SequenceRecord {
    }

    /**
     * Get the id of the sequence.
     *
     * @return the id of the sequence
     */
    @Override
    public H id() {
        return id;
    }

    /**
     * Get the sequence.
     *
     * @return the sequence
     */
    @Override
    public Sequence<S> sequence() {
        return sequence;
    }

    /**
     * Get a string representation of the sequence.
     *
     * @return a string representation of the sequence
     */
    public String getSequenceString() {
        return sequence.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) return false;
        SequenceRecord<?, ?> seq = (SequenceRecord<?, ?>) obj;
        return seq.id() == id && sequence.equals(seq.sequence);
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

    /**
     * Convenience method to create a {@link SequenceRecord} of a {@link CharSequence} with the {@link AlphabetDNA}.
     *
     * @param header   the header of the sequence
     * @param sequence the sequence
     * @return a new DNA SequenceRecord object
     */
    public static SequenceRecord<String, Character> DNA(String header, String sequence) {
        return new SequenceRecord<>(header, new CharSequence(new AlphabetDNA(), sequence));
    }

    /**
     * Convenience method to create a {@link SequenceRecord} of a {@link CharSequence} with the {@link AlphabetDNA}.
     *
     * @param id       the id of the sequence
     * @param sequence the sequence
     * @return a new DNA SequenceRecord object
     */
    public static SequenceRecord<Integer, Character> DNA(int id, String sequence) {
        return new SequenceRecord<>(id, new CharSequence(new AlphabetDNA(), sequence));
    }

    /**
     * Convenience method to create a {@link SequenceRecord} of a {@link CharSequence} with the {@link AlphabetAA}.
     *
     * @param header   the header of the sequence
     * @param sequence the sequence
     * @return a new AA SequenceRecord object
     */
    public static SequenceRecord<String, Character> AA(String header, String sequence) {
        return new SequenceRecord<>(header, new CharSequence(new AlphabetAA(), sequence));
    }

    /**
     * Convenience method to create a {@link SequenceRecord} of a {@link CharSequence} with the {@link AlphabetAA}.
     *
     * @param id       the id of the sequence
     * @param sequence the sequence
     * @return a new AA SequenceRecord object
     */
    public static SequenceRecord<Integer, Character> AA(int id, String sequence) {
        return new SequenceRecord<>(id, new CharSequence(new AlphabetAA(), sequence));
    }
}
