import org.husonlab.diamer2.alphabet.AAEncoder;
import org.husonlab.diamer2.graph.Tree;
import org.husonlab.diamer2.indexing.Bucket;
import org.husonlab.diamer2.indexing.Indexer;
import org.husonlab.diamer2.io.NCBIReader;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.Assert.assertEquals;

public class DBIndexerTest {

    @Test
    public void testDBIndexer() throws IOException {
        Tree tree = NCBIReader.readTaxonomy(new File("src/test/resources/testNCBI/nodes.dmp"), new File("src/test/resources/testNCBI/names.dmp"));
        Indexer indexer = new Indexer(tree, 4, 50, 1, 64);
        indexer.indexDB(new File("src/test/resources/testNCBI/db_preprocessed.fsa"), Path.of("src/test/resources/testNCBI/db_index"));
        Bucket[] buckets = new Bucket[1024];

        int uniqueKmerCount = 0;
        for (int i = 0; i < 1024; i++) {
            buckets[i] = new Bucket(new File("src/test/resources/testNCBI/db_index/" + i + ".bin"));
            uniqueKmerCount += Objects.requireNonNull(buckets[i].getContent()).length;
        }
        assertEquals(1322, uniqueKmerCount);

        TestKmer[] testKmers = {
            new TestKmer("NQFLFAGIELILRKY", 1, 0),
            new TestKmer("VIYGSGTTLQKQKSR", 1, 0),
            new TestKmer("YEITVYQLSADDLRS", 0, 1),
            new TestKmer("LEDNIRRVIADIRPQ", 0, 0),
            new TestKmer("RLETLRAWLDVTAPD", 4, 0)};

        for (TestKmer testKmer : testKmers) {
            System.out.println(testKmer.kmer);
            long kmerEnc = AAEncoder.toBase11(testKmer.kmer);
            int bucketId = (int) (kmerEnc & 0b1111111111);
            long kmerIndex = (kmerEnc << 10) & 0xffffffffffc00000L | testKmer.taxId;
            assertEquals(kmerIndex, Objects.requireNonNull(buckets[bucketId].getContent())[testKmer.pos]);
        }
    }

    private record TestKmer(String kmer, int taxId, int pos) {
        /**
         * pos: position of the kmerIndex in the bucket
         */
    }
}