package org.husonlab.diamer2.seq.alphabet;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Standard DNA alphabet.
 */
public class DNA extends Alphabet<Character> {

    private final Character[] symbols = new Character[]{'A', 'C', 'G', 'T'};

    private final HashSet<Character> symbolSet = new HashSet<>(Arrays.asList(symbols));

    @Override
    public boolean contains(Character symbol) {
        return symbolSet.contains(symbol);
    }

    @Override
    public Character[] getSymbols() {
        return symbols;
    }

    @Override
    public int getBase() {
        return 5;
    }

    @Override
    public String getName() {
        return "DNA Alphabet";
    }

    @Override
    public String toString(Iterable<Character> seq) {
        return String.valueOf(seq);
    }
}
