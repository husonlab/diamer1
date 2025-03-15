package org.husonlab.diamer2.main.encoders;

import org.husonlab.diamer2.seq.alphabet.Base11Alphabet;

import java.nio.file.Path;

/**
 * {@link Encoder} that uses the base 11 alphabet to encode kmers.
 */
public class W15 extends Encoder {
    /**
     * number of bits in the bucket that are used to encode the kmer
     */
    private final int nrOfBitsRequiredForKmer;
    /**
     * number of bits that do not fit in the bucket and are stored in the bucket names.
     */
    private final int nrOfBitsBucketNames;
    private final int nrOfBuckets;

    public W15(Path dbIndex, Path readsIndex, boolean[] mask, int bitsIds) {
        super(new Base11Alphabet(), dbIndex, readsIndex, mask, bitsIds);
        nrOfBitsRequiredForKmer = bitsRequired(targetAlphabet.getBase(), k - s);
        nrOfBitsBucketNames = nrOfBitsRequiredForKmer - (64 - bitsIds);
        nrOfBuckets = (int)Math.pow(2, nrOfBitsBucketNames);
    }

    @Override
    public long getBucketPartFromKmer(long kmer) {
        return kmer >>> nrOfBitsBucketNames;
    }

    @Override
    public long getIndex(int id, long kmer) {
        return getBucketPartFromKmer(kmer) << bitsForIds | id;
    }

    @Override
    public int getBucketNameFromKmer(long kmer) {
        return (int) kmer & (1 << nrOfBitsBucketNames) - 1;
    }

    @Override
    public int getIdFromIndexEntry(long kmerIndex) {
        return (int) kmerIndex & (1 << bitsForIds) - 1;
    }

    @Override
    public long getKmerFromIndexEntry(int bucketName, long kmerIndex) {
        return (kmerIndex >>> bitsForIds) << nrOfBitsBucketNames | bucketName;
    }
    @Override
    public long getKmerFromIndexEntry(long kmerIndex) {
        return kmerIndex >>> bitsForIds;
    }

    @Override
    public int getNrOfBitsBucketNames() {
        return nrOfBitsBucketNames;
    }

    @Override
    public int getNrOfBitsRequiredForKmer() {
        return nrOfBitsRequiredForKmer;
    }

    @Override
    public int getNrOfBuckets() {
        return nrOfBuckets;
    }
}
