package org.husonlab.diamer2.main.encoders;

import org.husonlab.diamer2.seq.alphabet.Base11Alphabet;
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
    public long getBucketPartFromKmer(long kmer) {
        return kmer >>> nrOfBitsBucketNames;
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
