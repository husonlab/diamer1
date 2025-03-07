package org.husonlab.diamer2.main.encoders;

import org.husonlab.diamer2.io.seq.FastaIdReader;
import org.husonlab.diamer2.io.seq.FastqIdReader;
import org.husonlab.diamer2.io.seq.HeaderToIdReader;
import org.husonlab.diamer2.io.seq.SequenceReader;
import org.husonlab.diamer2.seq.alphabet.Base11WithStop;
import org.husonlab.diamer2.seq.alphabet.DNA;
import org.husonlab.diamer2.seq.converter.Converter;
import org.husonlab.diamer2.seq.converter.DNAtoBase11RF1WithStop;
import org.husonlab.diamer2.seq.converter.DNAtoBase11WithStop;

import java.nio.file.Path;

/**
 * {@link Encoder} for the use of a nucleotide database in the DIAMOND base 11 alphabet.
 */
public class K15Base11Nuc extends Encoder<Character, DNA, Character, DNA, Base11WithStop> {

    private static final DNA dbAlphabet = new DNA();
    private static final DNA readsAlphabet = new DNA();
    private static final Base11WithStop targetAlphabet = new Base11WithStop();
    private FastqIdReader<DNA> readsReader;
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

    public K15Base11Nuc(Path db, Path reads, Path dbIndex, Path readsIndex, boolean[] mask, int bitsIds) {
        super(dbAlphabet, readsAlphabet, new Base11WithStop(), db, reads, dbIndex, readsIndex, mask, bitsIds);
        bitsOfKmerInBucket = bitsRequired(targetAlphabet.getBase(), k - s);
        bitsOfBucketNames = bitsOfKmerInBucket - (64 - bitsIds);
        numberOfBuckets = (int)Math.pow(2, bitsOfBucketNames);
    }

    @Override
    public SequenceReader<Integer, Character, DNA> getDBReader() {
        return new FastaIdReader<>(db, dbAlphabet);
    }

    @Override
    public SequenceReader<Integer, Character, DNA> getReadReader() {
        if (readsReader == null) readsReader = new FastqIdReader<>(reads, readsAlphabet);
        return readsReader;
    }

    @Override
    public HeaderToIdReader getHeaderToIdReader() {
        if (readsReader == null) readsReader = new FastqIdReader<>(reads, readsAlphabet);
        return readsReader;
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
    public DNA getDBAlphabet() {
        return dbAlphabet;
    }

    @Override
    public DNA getReadAlphabet() {
        return readsAlphabet;
    }

    @Override
    public Base11WithStop getTargetAlphabet() {
        return targetAlphabet;
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
    public int getNrOfBitsBucketNames() {
        return bitsOfBucketNames;
    }

    @Override
    public int getNrOfBitsRequiredForKmer() {
        return 0;
    }

    @Override
    public int getNrOfBuckets() {
        return numberOfBuckets;
    }
}
