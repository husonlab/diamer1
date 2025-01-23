package org.husonlab.diamer2.seq.alphabet.converter;

import org.husonlab.diamer2.seq.Sequence;
import org.husonlab.diamer2.seq.Compressed4BitSequence;
import org.husonlab.diamer2.seq.alphabet.Alphabet;
import org.husonlab.diamer2.seq.alphabet.AlphabetAA;
import org.husonlab.diamer2.seq.alphabet.Base11Alphabet;

import java.util.Objects;

/**
 * Converts the standard amino acid alphabet to a base 11 alphabet.
 */
public class AAtoBase11 implements Converter<Character, Byte> {

    private static final Alphabet<Character> SOURCE_ALPHABET = new AlphabetAA();
    private static final Alphabet<Byte> TARGET_ALPHABET = new Base11Alphabet();

    @Override
    public Sequence<Byte>[] convert(Sequence<Character> sequence) {
        byte[] result = new byte[sequence.length()];
        for (int i = 0; i < sequence.length(); i++) {
            result[i] = encodeAA(sequence.get(i));
        }
        return new Sequence[]{new Compressed4BitSequence(TARGET_ALPHABET, result)};
    }

    @Override
    public Alphabet<Character> getSourceAlphabet() {
        return SOURCE_ALPHABET;
    }

    @Override
    public Alphabet<Byte> getTargetAlphabet() {
        return TARGET_ALPHABET;
    }

    /**
     * Converts an amino acid to a number in the base 11 alphabet.
     * @param aa amino acid (upper case)
     * @return number representation of the amino acid in the base 11 alphabet
     */
    public byte encodeAA(char aa) {
        switch (aa) {
            // X unknown, B aspartate/asparagine, Z glutamate/glutamine, O pyrrolysine
            case 'B', 'D', 'E', 'K', 'N', 'O', 'Q', 'R', 'X', 'Z' -> { return 0; }
            case 'A', 'S', 'T' -> { return 1; }
            // J leucine/isoleucine
            case 'I', 'J', 'L', 'V' -> { return 2; }
            case 'G' -> { return 3; }
            case 'P' -> { return 4; }
            case 'F' -> { return 5; }
            case 'Y' -> { return 6; }
            // U selenocysteine
            case 'C', 'U' -> { return 7; }
            case 'H' -> { return 8; }
            case 'M' -> { return 9; }
            case 'W' -> { return 10; }
            default -> throw new IllegalArgumentException("Invalid amino acid: " + aa);
        }
    }
}
