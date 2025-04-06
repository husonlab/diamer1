package org.husonlab.diamer2.seq.alphabet;

import java.util.Arrays;

import static org.husonlab.diamer2.seq.converter.Utilities.codonToAAFR;

public class CustomAlphabet extends ReducedAlphabet {

    private final String alphabetString;
    private final byte[] aaToCustom;
    private final byte base;
    private final Byte[] symbols;

    public CustomAlphabet(String customAlphabet) {
        this.alphabetString = customAlphabet;
        this.aaToCustom = new byte[256];
        Arrays.fill(this.aaToCustom, (byte) -1);
        // A-Z: 65 - 90
        // a-z: 97 - 122
        String[] groups = customAlphabet.substring(1, customAlphabet.length() - 1).split("]\\[");
        this.base = (byte) groups.length;
        this.symbols = new Byte[base];
        for (int i = 0; i < groups.length; i++) {
            symbols[i] = (byte) i;
            String group = groups[i];
            for (char aa: group.toCharArray()) {
                this.aaToCustom[aa] = (byte) i;
                // Uncomment the following lines if you want to support both upper and lower case without explicitly
                // specifying them in the custom alphabet string
//                char upper = Character.isUpperCase(aa) ? aa : Character.toUpperCase(aa);
//                char lower = Character.isLowerCase(aa) ? aa : Character.toLowerCase(aa);
//                this.aaToCustom[upper] = (byte) i;
//                this.aaToCustom[lower] = (byte) i;
            }
        }
    }

    @Override
    public boolean contains(Byte symbol) {
        return symbol < base;
    }

    @Override
    public Byte[] getSymbols() {
        return symbols;
    }

    @Override
    public int getBase() {
        return base;
    }

    @Override
    public String getName() {
        return "Custom Alphabet: " + alphabetString;
    }

    @Override
    public String toString(Iterable<Byte> seq) {
        StringBuilder sb = new StringBuilder();
        for (Byte symbol : seq) {
            sb.append(symbol).append(" ");
        }
        return sb.toString();
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
