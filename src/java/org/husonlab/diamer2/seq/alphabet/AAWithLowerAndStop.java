package org.husonlab.diamer2.seq.alphabet;

import java.util.Arrays;
import java.util.HashSet;

public class AAWithLowerAndStop extends Alphabet<Character> {

    /**
     * Array with all 26 letters of the alphabet in both cases that occur in amino acid sequences + stop symbol (*).
     * <p>extended amino acid alphabet:
     *     <li><a href="https://www.ddbj.nig.ac.jp/ddbj/code-e.html">DDBJ</a></li>
     *     <li><a href="https://www.ncbi.nlm.nih.gov/CBBResearch/Yu/logoddslogo/apidocs/weblogo/seq.html">NCBI</a></li>
     * </p>
     */
    private static final Character[] symbols = new Character[]{
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '*'};
    private static final HashSet<Character> symbolSet = new HashSet<>(Arrays.asList(symbols));

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
        return symbols.length;
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
