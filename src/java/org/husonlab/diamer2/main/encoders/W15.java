package org.husonlab.diamer2.main.encoders;

import org.husonlab.diamer2.seq.alphabet.ReducedAlphabet;

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

    public W15(ReducedAlphabet targetAlphabet, Path dbIndex, Path readsIndex, boolean[] mask, int bitsIds) {
        super(targetAlphabet, dbIndex, readsIndex, mask, bitsIds);
        nrOfBitsRequiredForKmer = bitsRequired(this.targetAlphabet.getBase(), k - s);
        //nrOfBitsBucketNames = nrOfBitsRequiredForKmer - (64 - bitsIds);
        nrOfBitsBucketNames = 10;
        nrOfBuckets = (int)Math.pow(2, nrOfBitsBucketNames);
    }

    @Override
    protected double[] getLetterLikelihoods() {
        // Diamond alphabet
//        return new double[] {
//                0.29923406,
//                0.216387433,
//                0.223466117,
//                0.07384495,
//                0.048369393,
//                0.038860925,
//                0.030088765,
//                0.012823821,
//                0.021374994,
//                0.022660274,
//                0.012889268,
//        };
        // Etchebest
//        return new double[]{
//                0.073844950,
//                0.048369393,
//                0.125016218,
//                0.081838958,
//                0.093676297,
//                0.121110173,
//                0.205275507,
//                0.093958553,
//                0.086717656,
//                0.057368475,
//                0.012823821
//        };
        // Solis 15:
//        return new double[] {
//                0.15325383483325403,
//                0.14496368102896806,
//                0.11618364864892684,
//                0.09205330266746488,
//                0.09308286107746126,
//                0.07303932236072436,
//                0.05735650503114866,
//                0.05621676597756235,
//                0.047841581914978205,
//                0.04908692914699858,
//                0.03920023089754339,
//                0.030507102590509212,
//                0.021350351267170255,
//                0.012793290258340879,
//                0.013070592298949007,
//        };
        // Solis 15 S
        return new double[] {
                0.14804623689098245,
                0.15335753273670816,
                0.11506761295258673,
                0.09060244123355764,
                0.08913780616770403,
                0.07092459842937335,
                0.0571424145465511,
                0.05816523467806589,
                0.050833190607683945,
                0.04800544586521191,
                0.037544236351111114,
                0.028267208693218894,
                0.02183733225723984,
                0.012596234924171647,
                0.013157347469282023,
                0.005315126196551265
        };
    }

    @Override
    public long getIndexEntry(int id, long kmerWithoutBucketName) {
        return (kmerWithoutBucketName << bitsForIds) | id;
    }

    @Override
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

    @Override
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

    @Override
    public int getIdFromIndexEntry(long kmerIndex) {
        return (int) kmerIndex & ((1 << bitsForIds) - 1);
    }

    @Override
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
