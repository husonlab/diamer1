import org.husonlab.diamer2.indexing.DBIndexer;
import org.husonlab.diamer2.io.BucketIO;
import org.husonlab.diamer2.seq.alphabet.Base11Alphabet;
import org.husonlab.diamer2.taxonomy.Tree;
import org.husonlab.diamer2.indexing.Bucket;
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
        Tree tree = NCBIReader.readTaxonomy(new File("src/test/resources/database/nodes.dmp"), new File("src/test/resources/database/names.dmp"));
        DBIndexer dbIndexer = new DBIndexer(new File("src/test/resources/database/db_preprocessed.fsa"), Path.of("src/test/resources/test_output/db_index"), tree, 0b11111111111L, new Base11Alphabet(), 4, 50, 1, 64, true);
        dbIndexer.index();
        Bucket[] buckets = new Bucket[1024];

        int uniqueKmerCount = 0;
        for (int i = 0; i < 1024; i++) {
            buckets[i] = new BucketIO(new File("src/test/resources/test_output/db_index/" + i + ".bin"), i).read();
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
            long kmerEnc = Base11Alphabet.codonToAAAndBase11(testKmer.kmer);
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