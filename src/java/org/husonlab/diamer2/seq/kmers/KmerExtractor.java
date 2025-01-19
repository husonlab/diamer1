package org.husonlab.diamer2.seq.kmers;

import org.husonlab.diamer2.seq.Sequence;
import org.husonlab.diamer2.seq.encoder.Encoder;

public abstract class KmerExtractor<T> {
    KmerEncoder kmerEncoder;
    protected final int k;
    protected final int s;

    public KmerExtractor(Encoder encoder) {
        kmerEncoder = new KmerEncoder(encoder.getTargetAlphabet().getBase(), encoder.getMask());
        this.k = kmerEncoder.getK();
        this.s = kmerEncoder.getS();
    }

    abstract public long[] extractKmers(Sequence<T> sequence);

    public long getK() {
        return k;
    }
}
