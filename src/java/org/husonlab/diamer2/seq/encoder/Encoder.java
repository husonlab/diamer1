package org.husonlab.diamer2.seq.encoder;

import org.husonlab.diamer2.seq.alphabet.Alphabet;
import org.husonlab.diamer2.seq.alphabet.converter.Converter;

public abstract class Encoder {

    protected final Alphabet<Short> targetAlphabet;
    // length of the mask (kmer with spaces)
    protected final int k;
    // number of spaces between the bits of the mask
    protected final int s;
    protected final long mask;
    // number of bits required to represent the ids (taxon ids or sequence ids)
    protected final int bitsIds;

    /**
     * @param mask bit mask to use for spaced kmer extraction
     * @param bitsIds number of bits required to represent the ids (taxon ids or sequence ids)
     */
    public Encoder(Alphabet<Short> targetAlphabet, long mask, int bitsIds) {
        this.targetAlphabet = targetAlphabet;
        // remove trailing zeros (the least significant bits with value 0)
        this.mask = mask / Long.lowestOneBit(mask);
        // calculate position of the most significant bit (length of the mask / size of the window)
        this.k = Long.SIZE - Long.numberOfLeadingZeros(this.mask);
        // calculate the number of spaces between the bits of the mask
        this.s = k - Long.bitCount(this.mask);
        this.bitsIds = bitsIds;
    }

    /**
     * @return converter to convert amino acid sequences to base 11 sequences
     */
    public abstract Converter<Character, Short> getAAEncoder();
    /**
     * @return converter to convert DNA sequences to base 11 sequences
     */
    public abstract Converter<Character, Short> getDNAEncoder();

    /**
     * Combines an id and a kmer to an index entry. The bucket name is not included in the result.
     * @param id taxon id or sequence id the kmer belongs to
     * @param kmer the number representing the kmer
     * @return index entry
     */
    public abstract long getIndex(int id, long kmer);

    /**
     * Extracts the part of a kmer that is stored in the bucket and not its name.
     * @param kmer the number representing the kmer
     * @return the part of the kmer that belongs in the bucket
     */
    public abstract long getKmerBucket(long kmer);

    /**
     * Extracts the bucket name from a kmer.
     * @param kmer the number representing the kmer
     * @return the bucket name
     */
    public abstract int getBucketName(long kmer);

    /**
     * Extracts the id (taxon id or read id) from an index entry.
     * @param kmerIndex the number representing the index entry of the kmer (without the bucket name).
     * @return the taxon or read id
     */
    public abstract int getId(long kmerIndex);

    /**
     * Combines a bucket name and a kmerIndex index entry to the number representing the kmer.
     * @param bucketName the bucket name
     * @param kmerIndex the index entry of the kmer (without the bucket name)
     * @return the number representing the kmer
     */
    public abstract long getKmer(int bucketName, long kmerIndex);

    /**
     * Calculates the number of bits required to represent a kmer of length k with an alphabet with a given base.
     * <p>Is used to calculate the bits that are required to encode a kmer.</p>
     * @param base the base of the alphabet
     * @param k the length of the kmer
     * @return the number of bits required to encode the kmer.
     */
    protected int bitsRequired(int base, long k) {
        return (int) Math.ceil(Math.log(Math.pow(base, k)) / Math.log(2));
    }
}
