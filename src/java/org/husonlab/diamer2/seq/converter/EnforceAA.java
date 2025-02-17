package org.husonlab.diamer2.seq.converter;

import org.husonlab.diamer2.seq.CharSequence;
import org.husonlab.diamer2.seq.Sequence;
import org.husonlab.diamer2.seq.alphabet.Alphabet;
import org.husonlab.diamer2.seq.alphabet.AlphabetAA;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Enforces an uppercase amino acid alphabet without stop symbols (*). Sequences are split at stop symbols.
 * All characters that are not part of the {@link AlphabetAA} are removed.
 */
public class EnforceAA implements Converter<Character, Character> {

    private static final Alphabet<Character> targetAlphabet = new AlphabetAA();

    @Override
    public Sequence<Character>[] convert(Sequence<Character> sequence) {
        ArrayList<Sequence<Character>> result = new ArrayList<>();
        char[] tempArray = new char[sequence.length()];
        int i = 0;
        for (Character s : sequence) {
            s = Character.toUpperCase(s);
            if (s == '*' && i > 0) {
                result.add(new CharSequence(targetAlphabet, Arrays.copyOf(tempArray, i)));
                tempArray = new char[sequence.length() - i - 1];
                i = 0;
            } else if (targetAlphabet.contains(s)) {
                tempArray[i++] = s;
            }
        }
        if (i > 0) {
            result.add(new CharSequence(targetAlphabet, Arrays.copyOf(tempArray, i)));
        }
        return result.toArray(new Sequence[0]);
    }

    @Override
    public Alphabet<Character> getSourceAlphabet() {
        return new AlphabetAA();
    }

    @Override
    public Alphabet<Character> getTargetAlphabet() {
        return targetAlphabet;
    }
}
