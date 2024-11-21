package org.husonlab.diamer2.alphabet;

import java.util.LinkedList;

import static org.husonlab.diamer2.alphabet.AAEncoder.toBase11;

public class KmerEncoder {
    private final int k;
    private final int alphabetSize;
    private final LinkedList<Integer> kmer;
    private long encodedKmer;

    /**
     * Class to encode a kmer dynamically.
     * Chars can be added one after the other and the long representation of the kmer is updated accordingly.
     * The new char will be added to the kmer and the last char will be removed.
     * @param k length of the kmer
     * @param alphabetSize size of the alphabet
     */
    public KmerEncoder(int k, int alphabetSize) {
        this.k = k;
        this.alphabetSize = alphabetSize;
        kmer = new LinkedList<>();
    }

    /**
     * Class to encode a kmer dynamically.
     * Chars can be added one after the other and the long representation of the kmer is updated accordingly.
     * The new char will be added to the kmer and the last char will be removed.
     * @param k length of the kmer
     * @param alphabetSize size of the alphabet
     * @param initialKmer initial kmer
     */
    public KmerEncoder(int k, int alphabetSize, String initialKmer) {
        this.k = k;
        this.alphabetSize = alphabetSize;
        kmer = new LinkedList<>();
        initializeKmer(initialKmer);
    }

    /**
     * Initializes the kmer with a (new) string.
     * @param initialKmer initial kmer
     * @return long representation of the kmer
     */
    public long initializeKmer(String initialKmer) {
        kmer.clear();
        initialKmer.chars().forEach(c -> kmer.add((int) toBase11((char) c)));
        encodedKmer = encodeKmer();
        return encodedKmer;
    }

    /**
     * Adds a char to the kmer. The last char is removed.
     * @param c char to add
     * @return long representation of the new kmer
     */
    public long addChar(char c) {
        int oldLetterEncoding = kmer.pop();
        int newLetterEncoding = toBase11(c);
        encodedKmer = (long) ((encodedKmer - oldLetterEncoding*Math.pow(alphabetSize, k - 1))*alphabetSize + newLetterEncoding);
        kmer.add(newLetterEncoding);
        return encodedKmer;
    }

    /**
     * Encodes the current kmer to a long.
     * @return long representation of the kmer
     */
    private long encodeKmer() {
        long result = 0;
        int i = k - 1;
        for (Integer encodedLetter : kmer) {
            result += encodedLetter*Math.pow(alphabetSize, i);
            i--;
        }
        return result;
    }

    /**
     * Returns the long representation of the kmer.
     * @return long representation of the kmer
     */
    public long getEncodedKmer() {
        return encodedKmer;
    }
}
