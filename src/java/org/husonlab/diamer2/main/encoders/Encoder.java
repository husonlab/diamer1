package org.husonlab.diamer2.main.encoders;

import org.husonlab.diamer2.indexing.kmers.*;
import org.husonlab.diamer2.io.indexing.DBIndexIO;
import org.husonlab.diamer2.io.indexing.ReadIndexIO;
import org.husonlab.diamer2.seq.alphabet.ReducedAlphabet;

import java.nio.file.Path;

/**
 * Class to collect all settings that can be changed when indexing a database and a query.
 */
public abstract class Encoder {

    protected final ReducedAlphabet targetAlphabet;
    protected final Path dbIndex;
    protected final Path readsIndex;
    /**
     * length of the mask (kmer with spaces)
     */
    protected final int k;
    /**
     * number of spaces between the bits of the mask
     */
    protected final int s;
    protected final boolean[] mask;
    /**
     * number of bits required to represent the ids (taxon ids or sequence ids)
     */
    protected final int bitsForIds;

    /**
     * @param targetAlphabet the alphabet used to encode the kmers of database and query (probably a reduced protein
     *                       alphabet)
     * @param mask           bit mask to use for spaced kmer extraction
     * @param bitsForIds     number of bits required to represent the ids of the sequences (taxon ids or read ids)
     */
    public Encoder(ReducedAlphabet targetAlphabet, Path dbIndex, Path readsIndex, boolean[] mask, int bitsForIds) {
        this.targetAlphabet = targetAlphabet;
        this.dbIndex = dbIndex;
        this.readsIndex = readsIndex;
        this.mask = mask;
        // calculate position of the most significant bit (length of the mask / size of the window)
        this.k = mask.length;
        // calculate the number of spaces between the bits of the mask
        int sTemp = 0;
        for (boolean b : mask) {
            if (!b) {
                sTemp++;
            }
        }
        this.s = sTemp;
        this.bitsForIds = bitsForIds;
    }

    /**
     * @return the size of the mask (with spaces)
     */
    public int getK() {
        return k;
    }

    /**
     * @return the number of spaces in the mask
     */
    public int getS() {
        return s;
    }

    /**
     * @return the weight of the mask (number of non-zero bits)
     */
    public int getW() {
        return k - s;
    }

    /**
     * Calculates the number of bits required to represent a kmer of length k_s with an alphabet with a given base.
     * <p>Is used to calculate the bits that are required to encode a kmer.</p>
     * @param base the base of the alphabet
     * @param k_s the number of non-zero bits in the mask
     * @return the number of bits required to encode the kmer.
     */
    protected int bitsRequired(int base, long k_s) {
        return (int) Math.ceil(Math.log(Math.pow(base, k_s)) / Math.log(2));
    }

    /**
     * @return a KmerEncoder that can be used to encode kmers
     */
    protected abstract double[] getLetterLikelihoods();

    /**
     * @return a KmerExtractor that can be used to extract kmers from sequences
     */
    public KmerExtractor getKmerExtractor(){
//        KmerEncoder kmerEncoder = new KmerEncoder(targetAlphabet.getBase(), mask, getLetterLikelihoods());
//        return new KmerExtractorFiltered(kmerEncoder, (kmer) -> kmerEncoder.getComplexity() > 3);

//        KmerEncoder kmerEncoder = new KmerEncoder(targetAlphabet.getBase(), mask, getLetterLikelihoods());
//        return new KmerExtractorComplexityMaximizer(kmerEncoder, 15);

//        KmerEncoder kmerEncoder = new KmerEncoder(targetAlphabet.getBase(), mask, getLetterLikelihoods());
//        return new KmerExtractorProbabilityMinimizer(kmerEncoder, 15);

        return new KmerExtractor(new KmerEncoder(targetAlphabet.getBase(), mask, getLetterLikelihoods()));
    }

    public int getNrOfKmerBitsInBucketEntry() {
        return 64 - bitsForIds;
    }

    /**
     * @return The DB <strong>Index</strong> IO class to read and write the database index.
     */
    public DBIndexIO getDBIndexIO() {
        return new DBIndexIO(dbIndex, getNrOfBuckets());
    }

    /**
     * @return The Read <strong>Index</strong> IO class to read and write the read index.
     */
    public ReadIndexIO getReadIndexIO() {
        return new ReadIndexIO(readsIndex, getNrOfBuckets());
    }

    /**
     * @return the alphabet used to encode the kmers
     */
    public ReducedAlphabet getTargetAlphabet() {
        return targetAlphabet;
    }

    /**
     * @return the mask used to extract kmers
     */
    public boolean[] getMask() {
        return mask;
    }

    /**
     * Combines an id and a kmer to an index entry. The bucket name is not included in the result.
     * @param id taxon id or sequence id the kmer belongs to
     * @param kmerWithoutBucketName the number representing the kmer
     * @return index entry
     */
    public abstract long getIndexEntry(int id, long kmerWithoutBucketName);

    /**
     * Extracts the bucket name from a kmer.
     * @param kmer the number representing the kmer
     * @return the bucket name
     */
    public abstract int getBucketNameFromKmer(long kmer);

    /**
     * Removes the bits from the bucket that are captured in the bucket name.
     *
     * @return remaining bits of the kmer packed to the left 54 bits.
     */
    public abstract long getKmerWithoutBucketName(long kmer);

    /**
     * Extracts the id (taxon id or read id) from an index entry.
     * @param kmerIndex the number representing the index entry of the kmer (without the bucket name).
     * @return the taxon or read id
     */
    public abstract int getIdFromIndexEntry(long kmerIndex);

    public long getMaxKmerValue() {
        return (long) Math.pow(targetAlphabet.getBase(), getW()) - 1;
    }

    /**
     * Combines a bucket name and a kmerIndex index entry to the number representing the kmer.
     * @param bucketName the bucket name
     * @param kmerIndex the index entry of the kmer (without the bucket name)
     * @return the number representing the kmer
     */
    @Deprecated
    public abstract long getKmerFromIndexEntry(int bucketName, long kmerIndex);

    /**
     * Extracts the part of the kmer that is stored in the bucket (and not its name) from an index entry.
     * @param kmerIndex the index entry of the kmer
     * @return the part of the kmer that is stored in the bucket
     */
    public abstract long getKmerFromIndexEntry(long kmerIndex);

    /**
     * @return the number of bits that remain and make up the names of the buckets.
     */
    public abstract int getNrOfBitsBucketNames();

    public abstract int getNrOfBitsRequiredForKmer();

    public abstract int getNrOfBuckets();
}
