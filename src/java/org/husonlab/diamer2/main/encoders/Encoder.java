package org.husonlab.diamer2.main.encoders;

import org.husonlab.diamer2.indexing.kmers.KmerEncoder;
import org.husonlab.diamer2.indexing.kmers.KmerExtractor;
import org.husonlab.diamer2.seq.alphabet.Alphabet;
import org.husonlab.diamer2.seq.converter.Converter;

/**
 * Class to collect all settings that can be changed when indexing a database and a query.
 */
public abstract class Encoder {

    protected final Alphabet<Byte> targetAlphabet;
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
     * @param mask bit mask to use for spaced kmer extraction
     * @param bitsForIds number of bits required to represent the ids of the sequences (taxon ids or read ids)
     */
    public Encoder(Alphabet<Byte> targetAlphabet, boolean[] mask, int bitsForIds) {
        this.targetAlphabet = targetAlphabet;
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
     * @return converter to convert amino acid sequences to base 11 sequences
     */
    public abstract Converter<Character, Byte> getDBConverter();
    /**
     * @return converter to convert DNA sequences to base 11 sequences
     */
    public abstract Converter<Character, Byte> getReadConverter();

    /**
     * @return the alphabet used to encode the kmers
     */
    public Alphabet<Byte> getTargetAlphabet() {
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
     * @param kmer the number representing the kmer
     * @return index entry
     */
    public abstract long getIndex(int id, long kmer);

    /**
     * Extracts the part of a kmer that is stored in the bucket from a whole encoded kmer.
     * @param kmer the number representing the kmer
     * @return the part of the kmer that belongs in the bucket
     */
    public abstract long getBucketPartFromKmer(long kmer);

    /**
     * Extracts the bucket name from a kmer.
     * @param kmer the number representing the kmer
     * @return the bucket name
     */
    public abstract int getBucketNameFromKmer(long kmer);

    /**
     * Extracts the id (taxon id or read id) from an index entry.
     * @param kmerIndex the number representing the index entry of the kmer (without the bucket name).
     * @return the taxon or read id
     */
    public abstract int getIdFromIndexEntry(long kmerIndex);

    /**
     * Combines a bucket name and a kmerIndex index entry to the number representing the kmer.
     * @param bucketName the bucket name
     * @param kmerIndex the index entry of the kmer (without the bucket name)
     * @return the number representing the kmer
     */
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
    public abstract int getBitsOfBucketNames();

    public abstract int getNumberOfBuckets();

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
        return new KmerExtractor(new KmerEncoder(targetAlphabet.getBase(), mask));
    }
}
