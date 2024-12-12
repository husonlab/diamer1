package old;

import org.husonlab.diamer2.alphabet.DNAEncoder;
import org.husonlab.diamer2.indexing.Bucket;
import org.husonlab.diamer2.indexing.Indexer;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

import static org.junit.Assert.assertEquals;

public class ReadIndexerTest {
    @Test
    public void testReadIndexer() throws IOException {
        Indexer indexer = new Indexer(null, 4, 2, 2, 64);
        indexer.indexReads(new File("src/test/resources/testNCBI/reads.fq"), Path.of("src/test/resources/testNCBI/reads_index"));
        Bucket[] buckets = new Bucket[1024];
        int kmerCount = 0;
        for (int i = 0; i < 1024; i++) {
            buckets[i] = new Bucket(new File("src/test/resources/testNCBI/reads_index/" + i + ".bin"));
            kmerCount += Objects.requireNonNull(buckets[i].getContent()).length;
        }
        assertEquals(3392, kmerCount);

        TestKmer[] testKmers = {
            new TestKmer("GATTAGCGATGATGATACCGCGGCGCTGGGCGGCGGCAAAAGCAA", 0),
            new TestKmer("CAATGGTCTTATAACGAACGGCCAGGGTGCGTACTGTTTCAATGG", 0),
            new TestKmer("GGTAACTTTGTCATGCGTGGGACCGGCAAGCAATATTCTGGTAAC", 0),
            new TestKmer("CATAAAACCACCTTTATTGTTATACGTGAAGGCGGCGTAAGATTT", 1),
            new TestKmer("CATTTAAGCATCCGTTGCAAGTAGGGCACGCGAAGAACTTTTGAG", 5),
            new TestKmer("CAGACCGGACCGAACAAATGCTACCGATTTCAGCAATACGTAACC", 5)};

        for (TestKmer testKmer : testKmers) {
            long kmerEnc = DNAEncoder.toAAAndBase11(testKmer.kmer);
            int bucketId = (int) (kmerEnc & 0b1111111111);
            long kmerIndex = (kmerEnc << 10) & 0xffffffffffc00000L | testKmer.readId;
            int idInBucket = Arrays.binarySearch(Objects.requireNonNull(buckets[bucketId].getContent()), kmerIndex);
            assertEquals(kmerIndex, Objects.requireNonNull(buckets[bucketId].getContent())[idInBucket]);
        }

    }
    private record TestKmer(String kmer, int readId) {}

    // 1183484283575074816
}