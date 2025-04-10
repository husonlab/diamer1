import org.husonlab.diamer2.indexing.kmers.*;
import org.junit.Test;

import java.util.Arrays;

public class MinimizerTest {
    @Test
    public void testMinimizer() {
        byte[] sequence = new byte[]{7, 7, 2, 2, 9, 0, 0, 0, 0, 6, 10, 0, 10, 10, 7, 10, 5, 4, 3, 2, 9, 1};
        double[] letterLikelihoods = new double[]{1/11d, 2/11d, 3/11d, 4/11d, 5/11d, 6/11d, 7/11d, 8/11d, 9/11d, 10/11d, 11/11d};
        KmerEncoder kmerEncoder = new KmerEncoder(11, new boolean[]{true, true}, letterLikelihoods);
        KmerExtractor kmerExtractor1 = new KmerExtractor(kmerEncoder);
        KmerExtractor kmerExtractor2 = new KmerExtractorMinimizer(kmerEncoder, 6);
        KmerExtractor kmerExtractor3 = new KmerExtractorFiltered(kmerEncoder, (kmer) -> kmer < 50);
        KmerExtractor kmerExtractor4 = new KmerExtractorProbabilityMinimizer(kmerEncoder, 6);
        KmerExtractor kmerExtractor5 = new KmerExtractorComplexityMaximizer(kmerEncoder, 6);
        System.out.println(Arrays.toString(kmerExtractor1.extractKmers(sequence)));
        System.out.println(Arrays.toString(kmerExtractor2.extractKmers(sequence)));
        System.out.println(Arrays.toString(kmerExtractor3.extractKmers(sequence)));
        System.out.println(Arrays.toString(kmerExtractor4.extractKmers(sequence)));
        System.out.println(Arrays.toString(kmerExtractor5.extractKmers(sequence)));
    }
}

//[85, 90, 23, 20, 102, 33, 4, 49, 61, 76, 110, 6, 76, 117, 85, 93, 59, 47, 35, 31, 100]
//[23, 33, 4, 6, 59, 47, 35]