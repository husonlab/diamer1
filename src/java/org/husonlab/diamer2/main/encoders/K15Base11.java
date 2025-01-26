package org.husonlab.diamer2.main.encoders;

import org.husonlab.diamer2.seq.alphabet.Base11Alphabet;
import org.husonlab.diamer2.seq.alphabet.converter.AAtoBase11;
import org.husonlab.diamer2.seq.alphabet.converter.Converter;
import org.husonlab.diamer2.seq.alphabet.converter.DNAtoBase11;

/**
 * {@link Encoder} that uses the base 11 alphabet to encode kmers.
 */
public class K15Base11 extends Encoder {

    private static final AAtoBase11 aaEncoder = new AAtoBase11();
    private static final DNAtoBase11 dnaEncoder = new DNAtoBase11();
    /**
     * number of bits in the bucket that are used to encode the kmer
     */
    private final int bitsOfKmerInBucket;
    /**
     * number of bits that do not fit in the bucket and are stored in the bucket names.
     */
    private final int bitsOfBucketNames;
    private final int numberOfBuckets;

    public K15Base11(long mask, int bitsIds) {
        super(new Base11Alphabet(), mask, bitsIds);
        bitsOfKmerInBucket = bitsRequired(targetAlphabet.getBase(), k - s);
        bitsOfBucketNames = bitsOfKmerInBucket - (64 - bitsIds);
        numberOfBuckets = (int)Math.pow(2, bitsOfBucketNames);
    }

    @Override
    public Converter<Character, Byte> getAAConverter() {
        return aaEncoder;
    }

    @Override
    public Converter<Character, Byte> getDNAConverter() {
        return dnaEncoder;
    }

    @Override
    public long getBucketPartFromKmer(long kmer) {
        return kmer >>> bitsOfBucketNames;
    }

    @Override
    public long getIndex(int id, long kmer) {
        return getBucketPartFromKmer(kmer) << bitsForIds | id;
    }

    @Override
    public int getBucketNameFromKmer(long kmer) {
        return (int) kmer & (1 << bitsOfBucketNames) - 1;
    }

    @Override
    public int getIdFromIndexEntry(long kmerIndex) {
        return (int) kmerIndex & (1 << bitsForIds) - 1;
    }

    @Override
    public long getKmerFromIndexEntry(int bucketName, long kmerIndex) {
        return (kmerIndex >>> bitsForIds) << bitsOfBucketNames | bucketName;
    }

    @Override
    public long getKmerFromIndexEntry(long kmerIndex) {
        return kmerIndex >>> bitsForIds;
    }

    @Override
    public int getBitsOfBucketNames() {
        return bitsOfBucketNames;
    }

    @Override
    public int getNumberOfBuckets() {
        return numberOfBuckets;
    }
}
