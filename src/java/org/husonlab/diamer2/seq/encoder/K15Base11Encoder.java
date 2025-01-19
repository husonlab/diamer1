package org.husonlab.diamer2.seq.encoder;

import org.husonlab.diamer2.seq.alphabet.Base11Alphabet;
import org.husonlab.diamer2.seq.alphabet.converter.AAtoBase11;
import org.husonlab.diamer2.seq.alphabet.converter.Converter;
import org.husonlab.diamer2.seq.alphabet.converter.DNAtoBase11;

public class K15Base11Encoder extends Encoder {

    private static final AAtoBase11 aaEncoder = new AAtoBase11();
    private static final DNAtoBase11 dnaEncoder = new DNAtoBase11();
    // number of bits in the bucket that are used to encode the kmer
    private final int bitsKmerBucket;
    // number of bits that do not fit in the bucket and are stored in the bucket names.
    private final int bitsBucketNames;

    public K15Base11Encoder(long mask, int bitsIds) {
        super(new Base11Alphabet(), mask, bitsIds);
        bitsKmerBucket = bitsRequired(targetAlphabet.getBase(), k - s);
        bitsBucketNames = bitsKmerBucket - (64 - bitsIds);
    }

    @Override
    public Converter<Character, Short> getAAConverter() {
        return aaEncoder;
    }

    @Override
    public Converter<Character, Short> getDNAConverter() {
        return dnaEncoder;
    }

    @Override
    public long getKmerBucket(long kmer) {
        return kmer >>> bitsBucketNames;
    }

    @Override
    public long getIndex(int id, long kmer) {
        return getKmerBucket(kmer) << bitsIds | id;
    }

    @Override
    public int getBucketName(long kmer) {
        return (int) kmer & (1 << bitsBucketNames) - 1;
    }

    @Override
    public int getId(long kmerIndex) {
        return (int) kmerIndex & (1 << bitsIds) - 1;
    }

    @Override
    public long getKmer(int bucketName, long kmerIndex) {
        return (kmerIndex >>> bitsBucketNames) << bitsBucketNames | bucketName;
    }

    @Override
    public int getBitsBucketNames() {
        return bitsBucketNames;
    }
}
