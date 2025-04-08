import org.husonlab.diamer2.indexing.kmers.KmerEncoder;
import org.husonlab.diamer2.indexing.kmers.KmerExtractor;
import org.junit.Test;

import java.util.Arrays;

public class MinimizerTest {
    @Test
    public void testMinimizer() {
        byte[] sequence = new byte[]{7, 8, 2, 1, 9, 3, 0, 4, 5, 6, 10, 0, 6, 10, 7, 8, 5, 4, 3, 2, 9, 1};
        double[] letterLikelihoods = new double[]{1/11d, 2/11d, 3/11d, 4/11d, 5/11d, 6/11d, 7/11d, 8/11d, 9/11d, 10/11d, 11/11d};
        KmerEncoder kmerEncoder = new KmerEncoder(11, new boolean[]{true, true}, letterLikelihoods);
        KmerExtractor kmerExtractor = new KmerExtractor(kmerEncoder, (kmer) -> true);
        System.out.println(Arrays.toString(kmerExtractor.extractKmersNoMinimizers(sequence)));

        KmerExtractor kmerExtractor2 = new KmerExtractor(kmerEncoder, (kmer) -> true);
        System.out.println(Arrays.toString(kmerExtractor2.extractKmers(sequence)));
    }
}