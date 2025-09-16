package org.husonlab.diamer.seq.alphabet;

import org.husonlab.diamer.seq.SequenceRecord;

/**
 * Interface for an alphabet of symbols of type {@link T}.
 * <p>Implementations can be used to set the Alphabet of a {@link SequenceRecord}.</p>
 * @param <T> the type of symbols in the alphabet
 */
public abstract class Alphabet<T> {

    /**
     * Checks if the alphabet contains the given symbol.
     * @param symbol the symbol to check
     * @return true if the symbol is in the alphabet, false otherwise
     */
    abstract public boolean contains(T symbol);

    /**
     * @return an array of all symbols in the alphabet
     */
    abstract public T[] getSymbols();

    /**
     * @return the number of symbols in the alphabet
     */
    abstract public int getBase();

    /**
     * @return the name of the alphabet
     */
    abstract public String getName();

    /**
     * @return a string representation of the given sequence
     */
    abstract public String toString(Iterable<T> seq);

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Alphabet && ((Alphabet<?>) obj).getName().equals(getName());
    }

}