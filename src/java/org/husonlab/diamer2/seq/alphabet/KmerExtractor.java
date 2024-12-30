package org.husonlab.diamer2.seq.alphabet;

public abstract class KmerExtractor {
    protected final long mask;
    protected final ReducedProteinAlphabet proteinAlphabet;
    protected final int k;
    protected final int s;

    public KmerExtractor(long mask, ReducedProteinAlphabet proteinAlphabet) {
        this.mask = mask;
        this.proteinAlphabet = proteinAlphabet;
        KmerEncoder encoder = new KmerEncoder(proteinAlphabet.getBase(), mask);
        this.k = encoder.getK();
        this.s = encoder.getS();
    }

    abstract public long[] extractKmers(String sequence);
}
