package org.husonlab.diamer2.indexing;

public class IndexEncoding {
    /**
     * Extracts the encoded kmer from an index entry (first 42 bits (left to right)).
     * The kmer suffix (bucket name) is not included.
     * @param entry the index entry
     * @return the first 42 bits (left to right) of the kmer encoding
     */
    public static long getKmerPrefix(long entry) {
        return (entry >> 22) & 0x3FFFFFFFFFFL;
    }

    /**
     * Encodes a kmer and a bucket name into a kmer encoding.
     * @param entry the index entry
     * @param bucketName the bucket name to encode
     * @return the kmer encoding
     */
    public static long getKmer(long entry, int bucketName) {
        return (getKmerPrefix(entry) << 10) | bucketName;
    }

    /**
     * Extracts the taxId from a database index entry (last 22 bits (left to right)).
     * @param entry the index entry
     * @return the last 22 bits (left to right) of the index entry (taxId)
     */
    public static int getTaxId(long entry) {
        return (int) (entry & 0x3FFFFF);
    }

    /**
     * Extracts the readId from a read index entry (last 22 bits (left to right)).
     * @param entry the index entry
     * @return the last 22 (left to right) bits of the index entry (readId)
     */
    public static int getReadId(long entry) {
        return (int) (entry & 0x3FFFFF);
    }

    /**
     * Encodes a kmer and a taxId/readId into an index entry.
     * @param kmer the kmer to encode
     * @param id the taxId or readId to encode
     * @return the index entry
     */
    public static long getIndexEntry(long kmer, int id) {
        return ((kmer << 12) & 0xFFFFFFFFFFC00000L ) | id;
    }

    /**
     * Extracts the bucket name from a kmer encoding (last 10 bits (left to right)).
     * @param kmer the kmer encoding
     * @return the last 10 bits (left to right) of the kmer encoding (bucket name)
     */
    public static int getBucketName(long kmer) {
        return (int) (kmer & 0x3FF);
    }
}
