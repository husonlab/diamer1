package org.husonlab.diamer2.main.encoders;

import org.husonlab.diamer2.indexing.kmers.*;
import org.husonlab.diamer2.io.indexing.DBIndexIO;
import org.husonlab.diamer2.io.indexing.ReadIndexIO;
import org.husonlab.diamer2.main.GlobalSettings;
import org.husonlab.diamer2.seq.alphabet.ReducedAlphabet;

/**
 * Class to collect all settings that can be changed when indexing a database and a query.
 */
public class Encoder {

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
     * kmer extractor to use for spaced kmer extraction
     */
    protected final KmerExtractor kmerExtractor;
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
    protected final int nrOfBitsBucketNames;
    protected final int nrOfBuckets;

    public Encoder(GlobalSettings globalSettings, KmerExtractor kmerExtractor) {
        this.targetAlphabet = globalSettings.ALPHABET;
        this.globalSettings = globalSettings;
        this.mask = globalSettings.MASK;
        this.kmerExtractor = kmerExtractor;
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
        nrOfBitsBucketNames = 10;
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
     * @return a KmerExtractor that can be used to extract kmers from sequences
     */
    public KmerExtractor getKmerExtractor(){
        return kmerExtractor;
//        KmerEncoder kmerEncoder = new KmerEncoder(targetAlphabet.getBase(), mask, getLetterLikelihoods());
//        return new KmerExtractorFiltered(kmerEncoder, (kmer) -> kmerEncoder.getComplexity() > 3);

//        KmerEncoder kmerEncoder = new KmerEncoder(targetAlphabet.getBase(), mask, getLetterLikelihoods());
//        return new KmerExtractorComplexityMaximizer(kmerEncoder, 15);

//        KmerEncoder kmerEncoder = new KmerEncoder(targetAlphabet.getBase(), mask, getLetterLikelihoods());
//        return new KmerExtractorProbabilityMinimizer(kmerEncoder, 15);

//        return new KmerExtractor(new KmerEncoder(targetAlphabet.getBase(), mask, getLetterLikelihoods()));
    }

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
        // return (int) kmer & (1 << nrOfBitsBucketNames) - 1;

        return (int) (kmer & 0b1111111111) ^ 0b1010101010;

        // extract specific bits
        // 1111111111111111111111111110010111010111011111101101011011111111
        // 0000000000000000000000000001101000101000100000010010100100000000
        // 36, 35, 33, 29, 27, 23, 16, 13, 11, 8
        // 6   1   2   7   0   8   9   5   4   3
//        return (int)   ((((kmer >> 27) & 1)) |      // 27 -> 0
//                        (((kmer >> 35) & 1) << 1) | // 35 -> 1
//                        (((kmer >> 33) & 1) << 2) | // 33 -> 2
//                        (((kmer >> 8 ) & 1) << 3) | // ...
//                        (((kmer >> 11) & 1) << 4) |
//                        (((kmer >> 13) & 1) << 5) |
//                        (((kmer >> 36) & 1) << 6) |
//                        (((kmer >> 29) & 1) << 7) |
//                        (((kmer >> 23) & 1) << 8) |
//                        (((kmer >> 16) & 1) << 9));

        // hash bucket name for equal distribution
//        int bucket = (int) ((((kmer >> 27) & 1)) |      // 27 -> 0
//                (((kmer >> 35) & 1) << 1) | // 35 -> 1
//                (((kmer >> 33) & 1) << 2) | // 33 -> 2
//                (((kmer >> 8 ) & 1) << 3) | // ...
//                (((kmer >> 11) & 1) << 4) |
//                (((kmer >> 13) & 1) << 5) |
//                (((kmer >> 36) & 1) << 6) |
//                (((kmer >> 29) & 1) << 7) |
//                (((kmer >> 23) & 1) << 8) |
//                (((kmer >> 16) & 1) << 9));
//        return hashFunction(bucket) & 0b1111111111;
    }

    private static int hashFunction(long value) {
        value = (value ^ (value >>> 33)) * 0xff51afd7ed558ccdL;
        value = (value ^ (value >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return (int) (value ^ (value >>> 33));
    }

    public long getKmerWithoutBucketName(long kmer) {
                       // 0000000000000000000000000001101000101000100000010010100100000000
//        return ( kmer & 0b0000000000000000000000000000000000000000000000000000000011111111L) |
//               ((kmer & 0b0000000000000000000000000000000000000000000000000000011000000000L) >>> 1) |
//               ((kmer & 0b0000000000000000000000000000000000000000000000000001000000000000L) >>> 2) |
//               ((kmer & 0b0000000000000000000000000000000000000000000000001100000000000000L) >>> 3) |
//               ((kmer & 0b0000000000000000000000000000000000000000011111100000000000000000L) >>> 4) |
//               ((kmer & 0b0000000000000000000000000000000000000111000000000000000000000000L) >>> 5) |
//               ((kmer & 0b0000000000000000000000000000000000010000000000000000000000000000L) >>> 6) |
//               ((kmer & 0b0000000000000000000000000000000111000000000000000000000000000000L) >>> 7) |
//               ((kmer & 0b0000000000000000000000000000010000000000000000000000000000000000L) >>> 8) |
//               ((kmer & 0b1111111111111111111111111110000000000000000000000000000000000000L) >>> 10);
        return kmer >>> nrOfBitsBucketNames;
    }

    public int getIdFromIndexEntry(long kmerIndex) {
        return (int) kmerIndex & ((1 << bitsForIds) - 1);
    }

    public long getMaxKmerValue() {
        return (long) Math.pow(targetAlphabet.getBase(), getW()) - 1;
    }

    public long getKmerFromIndexEntry(int bucketName, long kmerIndex) {
        // return ((kmerIndex >>> bitsForIds) << nrOfBitsBucketNames) | bucketName;

        return ((kmerIndex >>> bitsForIds) << nrOfBitsBucketNames) | (bucketName ^ 0b1010101010);

        // insert specific bits
                             // 0000000000000001101000101000100000010010100100000000
//        return  ((kmerIndex & 0b1111111111111110000000000000000000000000000000000000000000000000L) >>> (bitsForIds - 10)) | ((bucketName & 0b0001000000L) << 30)
//                                                                                                                          | ((bucketName & 0b0000000010L) << 34) |
//                ((kmerIndex & 0b0000000000000001000000000000000000000000000000000000000000000000L) >>> (bitsForIds - 8 )) | ((bucketName & 0b0000000100L) << 31) |
//                ((kmerIndex & 0b0000000000000000111000000000000000000000000000000000000000000000L) >>> (bitsForIds - 7 )) | ((bucketName & 0b0010000000L) << 22) |
//                ((kmerIndex & 0b0000000000000000000100000000000000000000000000000000000000000000L) >>> (bitsForIds - 6 )) | ((bucketName & 0b0000000001L) << 27) |
//                ((kmerIndex & 0b0000000000000000000011100000000000000000000000000000000000000000L) >>> (bitsForIds - 5 )) | ((bucketName & 0b0100000000L) << 15) |
//                ((kmerIndex & 0b0000000000000000000000011111100000000000000000000000000000000000L) >>> (bitsForIds - 4 )) | ((bucketName & 0b1000000000L) << 7) |
//                ((kmerIndex & 0b0000000000000000000000000000011000000000000000000000000000000000L) >>> (bitsForIds - 3 )) | ((bucketName & 0b0000100000L) << 8 ) |
//                ((kmerIndex & 0b0000000000000000000000000000000100000000000000000000000000000000L) >>> (bitsForIds - 2 )) | ((bucketName & 0b0000010000L) << 7 ) |
//                ((kmerIndex & 0b0000000000000000000000000000000011000000000000000000000000000000L) >>> (bitsForIds - 1 )) | ((bucketName & 0b0000001000L) << 5 ) |
//                ((kmerIndex & 0b0000000000000000000000000000000000111111110000000000000000000000L) >>> bitsForIds);
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
