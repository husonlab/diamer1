package org.husonlab.diamer2.util;

public class Pair<S,T> {
    private final S first;
    private final T last;

    public Pair(S first, T last) {
        this.first = first;
        this.last = last;
    }

    public T getLast() {
        return last;
    }

    public S getFirst() {
        return first;
    }

    @Override
    public String toString() {
        return this.first.toString() + ", " + this.last.toString();
    }
}