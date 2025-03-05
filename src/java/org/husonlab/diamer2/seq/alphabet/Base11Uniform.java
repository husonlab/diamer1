package org.husonlab.diamer2.seq.alphabet;

public class Base11Uniform extends Alphabet<Byte> {
    /**
     * @param bits length of a kmer
     * @return highest number that can occur when a kmer with the given length is converted to a number.
     */
    @Deprecated
    public long highestEncoding(int bits) {
        return (long)Math.pow(getBase(), bits) - 1L;
    }

    @Override
    public int getBase() {
        return 11;
    }

    @Override
    public boolean contains(Byte symbol) {
        return symbol >= 0 && symbol <= 10;
    }

    @Override
    public Byte[] getSymbols() {
        return new Byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    }

    @Override
    public String getName() {
        return "Base 11 Uniform";
    }

    @Override
    public String toString(Iterable<Byte> seq) {
        StringBuilder sb = new StringBuilder();
        for (Byte symbol : seq) {
            sb.append(symbol).append(" ");
        }
        return sb.toString();
    }
}
