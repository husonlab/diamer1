package org.husonlab.diamer2.seq;

/**
 * Represents a sequence of type {@link S} together with a header/id of another type {@link H} e.g. the
 * header string or id of a sequence.
 * @param <H> the type of the header/id
 * @param <S> the type of the sequence
 */
public record SequenceRecord<H, S>(H id, S sequence) {

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
    public S sequence() {
        return sequence;
    }
}
