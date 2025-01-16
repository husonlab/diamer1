package org.husonlab.diamer2.seq.alphabet;

import java.util.HashSet;
import java.util.List;

public class AlphabetAA implements Alphabet<Character> {
    private final List<Character> symbols = List.of(
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z');
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
        return symbols.size();
    }

    @Override
    public String getName() {
        return "Amino Acid Alphabet";
    }

    @Override
    public String toString(Iterable<Character> seq) {
        StringBuilder sb = new StringBuilder();
        for (Character s : seq) {
            sb.append(s);
        }
        return sb.toString();
    }

}
