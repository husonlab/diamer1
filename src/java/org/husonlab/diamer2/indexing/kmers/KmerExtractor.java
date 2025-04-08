package org.husonlab.diamer2.indexing.kmers;

import org.husonlab.diamer2.util.Pair;

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Class to extract and encode of kmers from a sequence.
 */
public class KmerExtractor {
    private final KmerEncoder kmerEncoder;
    private final int k;
    private final Filter filter;

    // for minimizers
    private final static int windowSize = 27;
    private final PriorityQueue<Pair<Long, Double>> kmersQueue;
    private final Pair<Long, Double>[] kmersArray;

    /**
     * Creates a new KmerExtractor with the given encoder.
     *
     * @param kmerEncoder the encoder to use
     * @param filter
     */
    public KmerExtractor(KmerEncoder kmerEncoder, Filter filter) {
        this.kmerEncoder = kmerEncoder;
        this.k = this.kmerEncoder.getK();
        this.filter = filter;

        // for minimizers
        this.kmersQueue = new PriorityQueue<>(
                windowSize - k + 1,
                Comparator.comparingDouble(Pair::last)
        );
        this.kmersArray = new Pair[windowSize - k + 1];
    }

    /**
     * @return the length of the mask (including spaces)
     */
    public long getK() {
        return k;
    }

    /**
     * @return the number of spaces between the bits of the mask
     */
    public long getS() {
        return kmerEncoder.getS();
    }

    /**
     * Extracts the kmers from the given sequence.
     * <p>Together with the provided {@link KmerEncoder} the mask will be shifted over the sequence and all kmers will
     * be extracted and converted to a number. The most significant bit mask position will correspond to the most
     * significant position in the kmer to number conversion.</p>
     * @param sequence the sequence to extract the kmers from
     * @return the extracted kmers
     */
    public long[] extractKmersNoMinimizers(byte[] sequence) {
        int seqLength = sequence.length;
        if (seqLength < k) {
            return new long[0];
        }
        kmerEncoder.reset();
        // add the first k-1 characters to the encoder
        for (int i = 0; i < k - 1; i++) {
            kmerEncoder.addBack(sequence[i]);
        }
        // add the remaining characters to the encoder and store the resulting encoding
        long kmerEncoding;
        long[] kmers = new long[seqLength - k + 1];
        int kmersIndex = 0;
        for (int i = k - 1; i < seqLength; i++) {
            kmerEncoding = kmerEncoder.addBack(sequence[i]);
            if (filter.keep(kmerEncoding)) {
                kmers[kmersIndex++] = kmerEncoding;
            }
        }
        return Arrays.copyOf(kmers, kmersIndex);
    }

    /**
     * Extracts the minimizers from the given sequence.
     */
    public long[] extractKmers(byte[] sequence) {
        int seqLength = sequence.length;
        if (seqLength < windowSize) {
            return new long[0];
        }
        kmerEncoder.reset();
        kmersQueue.clear();
        // add the first k-1 characters to the encoder
        for (int i = 0; i < k - 1; i++) {
            kmerEncoder.addBack(sequence[i]);
        }

        long kmerEncoding;
        double kmerProbability;

        // fill first minimizer window
        for (int i = k - 1; i < windowSize - 1; i++) {
            kmerEncoding = kmerEncoder.addBack(sequence[i]);
            kmerProbability = kmerEncoder.getProbability();
            Pair<Long, Double> pair = new Pair<>(kmerEncoding, kmerProbability);
            kmersArray[i - k + 1] = pair;
            kmersQueue.add(pair);
        }

        // add the remaining characters to the encoder and store the resulting encoding
        long[] kmers = new long[seqLength - windowSize + 1];
        int kmersIndex = 0;
        for (int i = windowSize - 1; i < seqLength; i++) {
            kmerEncoding = kmerEncoder.addBack(sequence[i]);
            kmerProbability = kmerEncoder.getProbability();
            Pair<Long, Double> newKmer = new Pair<>(kmerEncoding, kmerProbability);
            Pair<Long, Double> oldKmer = kmersArray[0];
            System.arraycopy(kmersArray, 1, kmersArray, 0, kmersArray.length - 1);
            kmersArray[kmersArray.length - 1] = newKmer;
            kmersQueue.remove(oldKmer);
            kmersQueue.add(newKmer);
            Pair<Long, Double> minimizer = kmersQueue.peek();
            if (kmersIndex == 0 || minimizer.first() != kmers[kmersIndex - 1]) {
                kmers[kmersIndex++] = minimizer.first();
            }
        }
        return Arrays.copyOf(kmers, kmersIndex);
    }

    public interface Filter {
        boolean keep(long kmer);
    }
}