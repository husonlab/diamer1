package org.husonlab.diamer2.seq;

import org.husonlab.diamer2.seq.alphabet.Alphabet;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public abstract class Sequence<T> implements Iterable<T> {
    private final Alphabet<T> alphabet;

    public Sequence(Alphabet<T> alphabet) {
        this.alphabet = alphabet;
    }

    @NotNull
    @Override
    public abstract Iterator<T> iterator();

    public abstract T get(int index);

    public abstract int length();

    public String toString() {
        return alphabet.toString(this);
    }

    public Alphabet<T> getAlphabet() {
        return alphabet;
    }
}
