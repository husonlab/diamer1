package org.husonlab.diamer2.seq.converter;

import org.husonlab.diamer2.seq.Sequence;
import org.husonlab.diamer2.seq.alphabet.Alphabet;

/**
 * Interface for Converters that convert sequences from one alphabet to another.
 * <p>IMPORTANT: Only the sequence types are checked. A converter for DNA could be used to convert protein sequences and
 * there would be no error, since both are character sequences!</p>
 * @param <S1> The type of the input sequence.
 * @param <A1> The type of the input alphabet.
 * @param <S2> The type of the output sequence.
 * @param <A2> The type of the output alphabet.
 */
public abstract class Converter<S1, A1 extends Alphabet<S1>, S2, A2 extends Alphabet<S2>> {

    /**
     * Converts a sequence of type T to a sequence of type U.
     * @param sequence The sequence to convert.
     * @return The converted sequence.
     */
    abstract public Sequence<S2, A2>[] convert(Sequence<S1, A1> sequence);

}
