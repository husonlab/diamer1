package org.husonlab.diamer2.seq.alphabet.converter;

import org.husonlab.diamer2.seq.Sequence;
import org.husonlab.diamer2.seq.alphabet.Alphabet;

public interface Converter<T, U> {
    /**
     * Converts a sequence of type T to a sequence of type U.
     * @param sequence The sequence to convert.
     * @return The converted sequence.
     */
    public Sequence<U>[] convert(Sequence<T> sequence);

    /**
     * @return The input alphabet.
     */
    public Alphabet<T> getSourceAlphabet();

    /**
     * @return The output alphabet.
     */
    public Alphabet<U> getTargetAlphabet();
}
