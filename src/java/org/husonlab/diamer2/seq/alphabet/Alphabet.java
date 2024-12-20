package org.husonlab.diamer2.seq.alphabet;

import java.util.HashSet;

public abstract class Alphabet {
    protected int base;
    protected int bitsPerElement;
    protected final HashSet<Character> validChars = new HashSet<>();

    public int getBase() {
        return base;
    }
    public int getBitsPerElement() {
        return bitsPerElement;
    }
    public boolean isValid(char c) {
        return validChars.contains(c);
    }
}
