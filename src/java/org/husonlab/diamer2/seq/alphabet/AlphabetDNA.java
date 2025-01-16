package org.husonlab.diamer2.seq.alphabet;

import java.util.HashSet;
import java.util.List;

public class AlphabetDNA implements Alphabet<Character> {

    private final List<Character> symbols = List.of('A', 'C', 'G', 'T', 'N');

    private final HashSet<Character> symbolSet = new HashSet<>(symbols);

    @Override
    public boolean contains(Character symbol) {
        return symbolSet.contains(symbol);
    }

    @Override
    public Character[] getSymbols() {
        return symbols.toArray(new Character[0]);
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
