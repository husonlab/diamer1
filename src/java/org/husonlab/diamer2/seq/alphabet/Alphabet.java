package org.husonlab.diamer2.seq.alphabet;

import org.husonlab.diamer2.seq.HeaderSequenceRecord;

/**
 * Interface for an alphabet of symbols of type {@link T}.
 * <p>Implementations can be used to set the Alphabet of a {@link HeaderSequenceRecord}.</p>
 * @param <T> the type of symbols in the alphabet
 */
public interface Alphabet<T> {

    /**
     * Checks if the alphabet contains the given symbol.
     * @param symbol the symbol to check
     * @return true if the symbol is in the alphabet, false otherwise
     */
    public boolean contains(T symbol);

    /**
     * @return an array of all symbols in the alphabet
     */
    public T[] getSymbols();

    /**
     * @return the number of symbols in the alphabet
     */
    int getBase();

    /**
     * @return the name of the alphabet
     */
    String getName();

    /**
     * @return a string representation of the given sequence
     */
    String toString(Iterable<T> seq);
}
