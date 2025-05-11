//import org.husonlab.diamer.indexing.Bucket;
//import org.husonlab.diamer.indexing.ReadIndexer;
//import org.husonlab.diamer.io.indexing.BucketIO;
//import org.husonlab.diamer.seq.alphabet.Base11Alphabet;
//import org.junit.Test;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.Path;
//import java.util.Objects;
//
//import static org.junit.Assert.assertEquals;
//
//public class ReadIndexerTest {
//    @Test
//    public void testReadIndexer() throws IOException {
//        ReadIndexer readIndexer = new ReadIndexer(new File("src/test/resources/reads/reads.fq"), Path.of("src/test/resources/test_output/reads_index"), 0b11111111111L, new Base11Alphabet(), 4, 2, 2, 64);
//        readIndexer.index();
//        Bucket[] buckets = new Bucket[1024];
//        int KmerCount = 0;
//        for (int i = 0; i < 1024; i++) {
//            buckets[i] = new BucketIO(new File("src/test/resources/test_output/reads_index/" + i + ".bin"), i).read();
//            KmerCount += Objects.requireNonNull(buckets[i].getContent()).length;
//        }
//        assertEquals(3392, KmerCount);
//
////        TestKmer[] testKmers = {
////            new TestKmer("GATTAGCGATGATGATACCGCGGCGCTGGGCGGCGGCAAAAGCAA", 0),
////            new TestKmer("CAATGGTCTTATAACGAACGGCCAGGGTGCGTACTGTTTCAATGG", 0),
////            new TestKmer("GGTAACTTTGTCATGCGTGGGACCGGCAAGCAATATTCTGGTAAC", 0),
////            new TestKmer("CATAAAACCACCTTTATTGTTATACGTGAAGGCGGCGTAAGATTT", 1),
////            new TestKmer("CATTTAAGCATCCGTTGCAAGTAGGGCACGCGAAGAACTTTTGAG", 5),
////            new TestKmer("CAGACCGGACCGAACAAATGCTACCGATTTCAGCAATACGTAACC", 5)};
////
////        for (TestKmer testKmer : testKmers) {
////            long kmerEnc = DNAEncoder.toAAAndBase11(testKmer.kmer);
////            int bucketId = (int) (kmerEnc & 0b1111111111);
////            long kmerIndex = (kmerEnc << 10) & 0xffffffffffc00000L | testKmer.readId;
////            int idInBucket = Arrays.binarySearch(Objects.requireNonNull(buckets[bucketId].getContent()), kmerIndex);
////            assertEquals(kmerIndex, Objects.requireNonNull(buckets[bucketId].getContent())[idInBucket]);
////        }
//    }
//    private record TestKmer(String kmer, int readId) {}
//}