package org.husonlab.diamer.main.encoders;

import org.husonlab.diamer.indexing.kmers.*;
import org.husonlab.diamer.io.indexing.DBIndexIO;
import org.husonlab.diamer.io.indexing.ReadIndexIO;
import org.husonlab.diamer.main.GlobalSettings;
import org.husonlab.diamer.seq.alphabet.ReducedAlphabet;

/**
 * Class to collect all settings that can be changed when indexing a database and a query.
 */
public abstract class Encoder {

    private final GlobalSettings globalSettings;

    protected final ReducedAlphabet targetAlphabet;
    /**
     * length of the mask (kmer with spaces)
     */
    protected final int k;
    /**
     * number of spaces between the bits of the mask
     */
    protected final int s;
    /**
     * bit mask to use for spaced kmer extraction
     */
    protected final boolean[] mask;
    /**
     * number of bits required to represent the ids (taxon ids or sequence ids)
     */
    protected final int bitsForIds;
    /**
     * number of bits in the bucket that are used to encode the kmer
     */
    protected final int nrOfBitsRequiredForKmer;
    /**
     * number of bits that do not fit in the bucket and are stored in the bucket names.
     */
    protected final int nrOfBitsBucketNames = 10;
    protected final int nrOfBuckets;

    public Encoder(GlobalSettings globalSettings) {
        this.targetAlphabet = globalSettings.ALPHABET;
        this.globalSettings = globalSettings;
        this.mask = globalSettings.MASK;
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
        this.bitsForIds = globalSettings.BITS_FOR_IDS;
        nrOfBitsRequiredForKmer = bitsRequired(this.targetAlphabet.getBase(), k - s);
        nrOfBuckets = (int)Math.pow(2, nrOfBitsBucketNames);
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
     * Has to be implemented so that every thread can get its own instance.
     * @return a KmerExtractor that can be used to extract kmers from sequences
     */
    public abstract KmerExtractor getKmerExtractor();
    public int getNrOfKmerBitsInBucketEntry() {
        return 64 - bitsForIds;
    }

    /**
     * @return The DB <strong>Index</strong> IO class to read and write the database index.
     */
    public DBIndexIO getDBIndexIO() {
        return new DBIndexIO(globalSettings.DB_INDEX, getNrOfBuckets());
    }

    /**
     * @return The Read <strong>Index</strong> IO class to read and write the read index.
     */
    public ReadIndexIO getReadIndexIO() {
        return new ReadIndexIO(globalSettings.READS_INDEX, getNrOfBuckets());
    }

    /**
     * @return the alphabet used to encode the kmers
     */
    public ReducedAlphabet getTargetAlphabet() {
        return targetAlphabet;
    }

    public long getIndexEntry(int id, long kmerWithoutBucketName) {
        return (kmerWithoutBucketName << bitsForIds) | id;
    }

    public int getBucketNameFromKmer(long kmer) {
        return (int) (kmer & 0b1111111111) ^ 0b1010101010;
    }

    private static int hashFunction(long value) {
        value = (value ^ (value >>> 33)) * 0xff51afd7ed558ccdL;
        value = (value ^ (value >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return (int) (value ^ (value >>> 33));
    }

    public long getKmerWithoutBucketName(long kmer) {
        return kmer >>> nrOfBitsBucketNames;
    }

    public int getIdFromIndexEntry(long kmerIndex) {
        return (int) kmerIndex & ((1 << bitsForIds) - 1);
    }

    public long getMaxKmerValue() {
        return (long) Math.pow(targetAlphabet.getBase(), getW()) - 1;
    }

    public long getKmerFromIndexEntry(int bucketName, long kmerIndex) {
        return ((kmerIndex >>> bitsForIds) << nrOfBitsBucketNames) | (bucketName ^ 0b1010101010);
    }

    public long getKmerFromIndexEntry(long kmerIndex) {
        return kmerIndex >>> bitsForIds;
    }

    public int getNrOfBitsBucketNames() {
        return nrOfBitsBucketNames;
    }

    public int getNrOfBitsRequiredForKmer() {
        return nrOfBitsRequiredForKmer;
    }

    public int getNrOfBuckets() {
        return nrOfBuckets;
    }
}
