package org.husonlab.diamer2.seq.alphabet;

public class Base11Alphabet implements Alphabet<Short> {

    private static final int base = 11;

    public long highestEncoding() {
        return 4177248169415650L;
    }

    @Override
    public int getBase() {
        return base;
    }

    @Override
    public boolean contains(Short symbol) {
        return symbol >= 0 && symbol <= 10;
    }

    @Override
    public Short[] getSymbols() {
        return new Short[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    }

    @Override
    public String getName() {
        return "Base 11 Alphabet";
    }

    @Override
    public String toString(Iterable<Short> seq) {
        StringBuilder sb = new StringBuilder();
        for (Short symbol : seq) {
            sb.append(symbol).append(" ");
        }
        return sb.toString();
    }
}
