package org.husonlab.diamer2.main.encoders;

import org.husonlab.diamer2.seq.alphabet.Base11Alphabet;
import org.husonlab.diamer2.seq.alphabet.Base11WithStop;
import org.husonlab.diamer2.seq.alphabet.DNA;
import org.husonlab.diamer2.seq.converter.Converter;
import org.husonlab.diamer2.seq.converter.DNAtoBase11;
import org.husonlab.diamer2.seq.converter.DNAtoBase11RF1WithStop;
import org.husonlab.diamer2.seq.converter.DNAtoBase11WithStop;

/**
 * {@link Encoder} for the use of a nucleotide database in the DIAMOND base 11 alphabet.
 */
public class K15Base11Nuc extends Encoder<Character, DNA, Character, DNA, Base11WithStop> {

    private final DNAtoBase11RF1WithStop dbConverter = new DNAtoBase11RF1WithStop();
    private static final DNAtoBase11WithStop readConverter = new DNAtoBase11WithStop();
    /**
     * number of bits in the bucket that are used to encode the kmer
     */
    private final int bitsOfKmerInBucket;
    /**
     * number of bits that do not fit in the bucket and are stored in the bucket names.
     */
    private final int bitsOfBucketNames;
    private final int numberOfBuckets;

    public K15Base11Nuc(boolean[] mask, int bitsIds) {
        super(new DNA(), new DNA(), new Base11WithStop(), mask, bitsIds);
        bitsOfKmerInBucket = bitsRequired(targetAlphabet.getBase(), k - s);
        bitsOfBucketNames = bitsOfKmerInBucket - (64 - bitsIds);
        numberOfBuckets = (int)Math.pow(2, bitsOfBucketNames);
    }

    @Override
    public Converter<Character, DNA, Byte, Base11WithStop> getDBConverter() {
        return dbConverter;
    }

    @Override
    public Converter<Character, DNA, Byte, Base11WithStop> getReadConverter() {
        return readConverter;
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
