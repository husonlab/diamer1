package org.husonlab.diamer2.seq.alphabet.converter;

import org.husonlab.diamer2.seq.Sequence;
import org.husonlab.diamer2.seq.alphabet.Alphabet;

/**
 * Interface for Converters that convert sequences from one alphabet to another.
 * <p>IMPORTANT: Only the sequence types are checked. A converter for DNA could be used to convert protein sequences and
 * there would be no error, since both are character sequences!</p>
 * @param <T> The type of the input alphabet.
 * @param <U> The type of the output alphabet.
 */
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
