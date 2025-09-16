package org.husonlab.diamer.io.seq;

import org.husonlab.diamer.seq.SequenceRecord;

/**
 * This Interface is intended to be used in scenarios, where a computation has to be performed on a
 * {@link SequenceRecord} in a lazy manner and can result in more than one sequence record.
 * <p>
 *     So far, it is used when reading in sequences that have to be translated in multiple reading frames.
 *     To perform the translation only when the result is actually needed, the process can be embedded in the
 *     {@link #getSequenceRecords()} method and will be executed on the thread that calls this method.
 * </p>
 * @param <H> Header type
 * @param <S> Sequence type
 */
public interface FutureSequenceRecords<H, S> {
    /**
     * Performs the computation defined in the implementing class and returns the result.
     * <p>
     *     Depending on the implementation, this method call might be more or less computationally expensive.
     * </p>
     */
    Iterable<SequenceRecord<H, S>> getSequenceRecords();
}
