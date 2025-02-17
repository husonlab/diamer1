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
public class AAtoBase11Uniform implements Converter<Character, Byte> {

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
            case 'L' -> { return 0; }
            case 'A' -> { return 1; }
            case 'G', 'C' -> { return 2; }
            case 'V', 'W', 'U', 'B', 'J', 'Z', 'O' -> { return 3; }
            case 'S', 'H' -> { return 4; }
            case 'E', 'M', 'X' -> { return 5; }
            case 'T', 'Y' -> { return 6; }
            case 'R', 'Q' -> { return 7; }
            case 'D', 'N' -> { return 8; }
            case 'I', 'F' -> { return 9; }
            case 'P', 'K' -> { return 10; }
            default -> throw new IllegalArgumentException("Invalid amino acid: " + aa);
        }
    }
}
