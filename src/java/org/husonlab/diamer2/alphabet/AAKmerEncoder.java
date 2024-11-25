package org.husonlab.diamer2.alphabet;

import java.util.LinkedList;

import static org.husonlab.diamer2.alphabet.AAEncoder.toBase11;

public class AAKmerEncoder {

    private final int k;
    private final int alphabetSize;
    private final LinkedList<Short> kmer;
    private long encodedKmer;

    /**
     * Class to encode a kmer dynamically.
     * Chars can be added one after the other and the long representation of the kmer is updated accordingly.
     * The new char will be added to the kmer and the last char will be removed.
     * @param k length of the kmer
     */
    public AAKmerEncoder(int k, int alphabetSize) {
        this.k = k;
        this.alphabetSize = alphabetSize;
        kmer = new LinkedList<>();
        for (int i = 0; i < k; i++) {
            kmer.addFirst((short) 0);
        }
        encodedKmer = 0;
    }

    public long addFront(short aaEncoding) {
        encodedKmer = (encodedKmer - kmer.pollLast()) / alphabetSize + aaEncoding * (long)Math.pow(alphabetSize, k - 1);
        kmer.addFirst(aaEncoding);
        return encodedKmer;
    }

    /**
     * Adds a char to the back of the kmer. The first char is removed.
     * @param aaEncoding encoding of the new char
     * @return long representation of the new kmer
     */
    public long addBack(short aaEncoding) {
        encodedKmer = (encodedKmer - kmer.pollFirst() * (long)Math.pow(alphabetSize, k - 1)) * alphabetSize + aaEncoding;
        kmer.addLast(aaEncoding);
        return encodedKmer;
    }

    /**
     * Returns the long representation of the kmer.
     * @return long representation of the kmer
     */
    public long getEncodedKmer() {
        return encodedKmer;
    }
}
