package org.husonlab.diamer2.seq.converter;

import org.husonlab.diamer2.seq.ByteSequence;
import org.husonlab.diamer2.seq.Sequence;
import org.husonlab.diamer2.seq.alphabet.AA;
import org.husonlab.diamer2.seq.alphabet.Base11Custom;

import java.util.Arrays;

import static org.husonlab.diamer2.seq.converter.Utilities.codonToAAFR;
import static org.husonlab.diamer2.seq.converter.Utilities.splitAtMinus1;

public class AAtoBase11Custom extends Converter<Character, AA, Byte, Base11Custom>{

    private final Base11Custom targetAlphabet = new Base11Custom();
    private final byte[] aaToCustom;
    private final int base;

    public AAtoBase11Custom(String customAlphabet) {
        this.aaToCustom = new byte[256];
        Arrays.fill(this.aaToCustom, (byte) -1);
        // A-Z: 65 - 90
        // a-z: 97 - 122
        String[] groups = customAlphabet.substring(1, customAlphabet.length() - 1).split("]\\[");
        for (int i = 0; i < groups.length; i++) {
            String group = groups[i];
            for (char aa: group.toCharArray()) {
                char upper = Character.isUpperCase(aa) ? aa : Character.toUpperCase(aa);
                char lower = Character.isLowerCase(aa) ? aa : Character.toLowerCase(aa);
                this.aaToCustom[upper] = (byte) i;
                this.aaToCustom[lower] = (byte) i;
            }
        }
        this.base = groups.length;
    }

    @Override
    public Sequence<Byte, Base11Custom>[] convert(Sequence<Character, AA> sequence) {
        byte[] result = new byte[sequence.length()];
        for (int i = 0; i < sequence.length(); i++) {
            result[i] = aaToAlphabet(sequence.get(i));
        }
        byte[][] splitResult = splitAtMinus1(result);
        Sequence<Byte, Base11Custom>[] sequences = new Sequence[splitResult.length];
        for (int i = 0; i < splitResult.length; i++) {
            sequences[i] = new ByteSequence<>(targetAlphabet, splitResult[i]);
        }
        return sequences;
    }

    private byte aaToAlphabet(char aa) {
        return aaToCustom[aa];
    }

    public int getBase() {
        return base;
    }

    private byte[] codonToAlphabetFR(String codon) {
        char[] translation = codonToAAFR(codon);
        return new byte[]{aaToCustom[translation[0]], aaToCustom[translation[1]]};
    }
}
