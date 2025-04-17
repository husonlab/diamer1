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
        return new double[] {
                0.15325383483325403,
                0.14496368102896806,
                0.11618364864892684,
                0.09205330266746488,
                0.09308286107746126,
                0.07303932236072436,
                0.05735650503114866,
                0.05621676597756235,
                0.047841581914978205,
                0.04908692914699858,
                0.03920023089754339,
                0.030507102590509212,
                0.021350351267170255,
                0.012793290258340879,
                0.013070592298949007,
        };
    }

    @Override
    public long getIndexEntry(int id, long kmerWithoutBucketName) {
        return (kmerWithoutBucketName << bitsForIds) | id;
    }

    @Override
    public int getBucketNameFromKmer(long kmer) {
//        return (int) kmer & (1 << nrOfBitsBucketNames) - 1;
        // 1111111111111111011111111110010111010111011111111101011011111111
        // 0000000000000000100000000001101000101000100000000010100100000000
        // 47, 36, 35, 33, 29, 27, 23, 13, 11, 8
        return (int)   ((((kmer >> 27) & 1)) |      // 27 -> 0
                        (((kmer >> 35) & 1) << 1) | // 35 -> 1
                        (((kmer >> 33) & 1) << 2) | // 33 -> 2
                        (((kmer >> 8 ) & 1) << 3) | // ...
                        (((kmer >> 11) & 1) << 4) |
                        (((kmer >> 13) & 1) << 5) |
                        (((kmer >> 36) & 1) << 6) |
                        (((kmer >> 29) & 1) << 7) |
                        (((kmer >> 23) & 1) << 8) |
                        (((kmer >> 47) & 1) << 9));
    }

    @Override
    public long getKmerWithoutBucketName(long kmer) {
        return (kmer & 0xFFL) |
               ((kmer & 0x600L) >>> 1) |
               ((kmer & 0x1000L) >>> 2) |
               ((kmer & 0x7FC000L) >>> 3) |
               ((kmer & 0x7000000L) >>> 4) |
               ((kmer & 0x10000000L) >>> 5) |
               ((kmer & 0x1C0000000L) >>> 6) |
               ((kmer & 0x400000000L) >>> 7) |
               ((kmer & 0x7FE000000000L) >>> 9) |
               ((kmer & 0xFFFF000000000000L) >>> 10);
    }

    @Override
    public int getIdFromIndexEntry(long kmerIndex) {
        return (int) kmerIndex & ((1 << bitsForIds) - 1);
    }

    @Override
    public long getKmerFromIndexEntry(int bucketName, long kmerIndex) {
//        return (kmerIndex >>> bitsForIds) << nrOfBitsBucketNames | bucketName;
        return  (kmerIndex & 0xFFFF000000000000L) | ((bucketName & 0x200L) << 38) | // 9 -> 47
                (kmerIndex & 0xFFC000000000L >> 1) | ((bucketName & 0x40L) << 30) | ((bucketName & 0x2L) << 34)| // 6,1 -> 36,35
                (kmerIndex & 0x2000000000L >> 3) | ((bucketName & 0x4L) << 31) | // 2 -> 33
                (kmerIndex & 0x1C00000000L >> 4) | ((bucketName & 0x80L) << 22) | // 7 -> 29
                (kmerIndex & 0x200000000L >> 5) | ((bucketName & 0x1L) << 27) | // 0 -> 27
                (kmerIndex & 0x1C0000000L >> 6) | ((bucketName & 0x100L) << 15) | // 8 -> 23
                (kmerIndex & 0x3FE00000L >> 7) | ((bucketName & 0x20L) << 8) | // 5 -> 13
                (kmerIndex & 0x100000L >> 8) | ((bucketName & 0x10L) << 7) | // 4 -> 11
                (kmerIndex & 0xC0000L >> 9) | ((bucketName & 0x8L) << 5); // 3 -> 8
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
