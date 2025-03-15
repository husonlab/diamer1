package org.husonlab.diamer2.seq.alphabet;

import java.util.Arrays;

import static org.husonlab.diamer2.seq.converter.Utilities.codonToAAFR;

public class Base11Custom extends Base11Alphabet {

    private final String alphabetString;
    private final byte[] aaToCustom;

    public Base11Custom(String customAlphabet) {
        this.alphabetString = customAlphabet;
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
    }

    @Override
    public String getName() {
        return "Custom Alphabet: " + alphabetString;
    }

    @Override
    public byte[] translateCodon(String codon) {
        char[] translation = codonToAAFR(codon);
        return new byte[]{aaToCustom[translation[0]], aaToCustom[translation[1]]};
    }

    @Override
    public byte translateAA(char aa) {
        return aaToCustom[aa];
    }
}
