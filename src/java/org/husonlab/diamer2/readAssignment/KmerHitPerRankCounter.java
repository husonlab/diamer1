package org.husonlab.diamer2.readAssignment;

import org.husonlab.diamer2.seq.alphabet.Base11Alphabet;

import java.util.concurrent.ConcurrentHashMap;

public class KmerHitPerRankCounter {
    private final ConcurrentHashMap<String, int[]> kmerHitsPerRank;
    private final int resolution;
    private final long highestIndexEnc;
    private final float factor;

    public KmerHitPerRankCounter(int resolution) {
        this.highestIndexEnc = (new Base11Alphabet()).highestEncoding();
        this.resolution = resolution;
        this.factor = (float)highestIndexEnc / resolution;
        this.kmerHitsPerRank = new ConcurrentHashMap<>();
    }

    public void addKmerHit(long kmer, String rank) {

    }
}
