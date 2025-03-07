package org.husonlab.diamer2.main.encoders;

import org.husonlab.diamer2.io.seq.FastaIdReader;
import org.husonlab.diamer2.io.seq.FastqIdReader;
import org.husonlab.diamer2.io.seq.HeaderToIdReader;
import org.husonlab.diamer2.io.seq.SequenceReader;
import org.husonlab.diamer2.seq.alphabet.AA;
import org.husonlab.diamer2.seq.alphabet.Base11Alphabet;
import org.husonlab.diamer2.seq.alphabet.DNA;
import org.husonlab.diamer2.seq.converter.AAtoBase11;
import org.husonlab.diamer2.seq.converter.Converter;
import org.husonlab.diamer2.seq.converter.DNAtoBase11;

import java.nio.file.Path;

/**
 * {@link Encoder} that uses the base 11 alphabet to encode kmers.
 */
public class K15Base11 extends Encoder<Character, AA, Character, DNA, Base11Alphabet> {

    private static final AA dbAlphabet = new AA();
    private static final DNA readsAlphabet = new DNA();
    private static final Base11Alphabet targetAlphabet = new Base11Alphabet();

    private FastqIdReader<DNA> readsReader;
    private static final AAtoBase11 dbConverter = new AAtoBase11();
    private final DNAtoBase11 readConverter;
    /**
     * number of bits in the bucket that are used to encode the kmer
     */
    private final int nrOfBitsRequiredForKmer;
    /**
     * number of bits that do not fit in the bucket and are stored in the bucket names.
     */
    private final int nrOfBitsBucketNames;
    private final int nrOfBuckets;

    public K15Base11(Path db, Path reads, Path dbIndex, Path readsIndex, boolean[] mask, int bitsIds) {
        super(dbAlphabet, readsAlphabet, targetAlphabet, db, reads, dbIndex, readsIndex, mask, bitsIds);
        nrOfBitsRequiredForKmer = bitsRequired(targetAlphabet.getBase(), k - s);
        nrOfBitsBucketNames = nrOfBitsRequiredForKmer - (64 - bitsIds);
        nrOfBuckets = (int)Math.pow(2, nrOfBitsBucketNames);
        readConverter = new DNAtoBase11(k);
    }

    @Override
    public SequenceReader<Integer, Character, AA> getDBReader() {
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
    public Converter<Character, AA, Byte, Base11Alphabet> getDBConverter() {
        return dbConverter;
    }

    @Override
    public Converter<Character, DNA, Byte, Base11Alphabet> getReadConverter() {
        return readConverter;
    }

    @Override
    public AA getDBAlphabet() {
        return dbAlphabet;
    }

    @Override
    public DNA getReadAlphabet() {
        return readsAlphabet;
    }

    @Override
    public Base11Alphabet getTargetAlphabet() {
        return targetAlphabet;
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
