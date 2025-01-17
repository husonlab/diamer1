package org.husonlab.diamer2.seq.alphabet.converter;

import org.husonlab.diamer2.seq.Sequence;

public interface Converter<T, U> {
    public Sequence<U>[] convert(Sequence<T> sequence);
}
