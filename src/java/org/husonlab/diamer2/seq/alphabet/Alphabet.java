package org.husonlab.diamer2.seq.alphabet;

public interface Alphabet<T> {
    public boolean contains(T symbol);
    public T[] getSymbols();
    int getBase();
    String getName();
    String toString(Iterable<T> seq);
}
