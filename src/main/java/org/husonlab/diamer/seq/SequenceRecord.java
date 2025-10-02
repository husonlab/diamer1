package org.husonlab.diamer.seq;

/**
 * Represents a Sequence of type {@link S} together with a header/Id of another type {@link H} e.g. the
 * header string or Id of a Sequence.
 * @param <H> the type of the header/Id
 * @param <S> the type of the Sequence
 */
public class SequenceRecord<H, S> {
    private final H id;
    private final S sequence;

    public SequenceRecord(H id, S sequence) {
        this.id = id;
        this.sequence = sequence;
    }

    /**
     * Get the Id of the Sequence.
     * @return the id of the Sequence
     */
    public H getId() {
        return id;
    }

    /**
     * Get the Sequence.
     * @return the Sequence
     */
    public S getSequence() {
        return sequence;
    }
}
