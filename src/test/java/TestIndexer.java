import org.husonlab.diamer2.alphabet.AAEncoder;
import org.husonlab.diamer2.graph.Tree;
import org.husonlab.diamer2.indexing.Bucket;
import org.husonlab.diamer2.indexing.Indexer;
import org.husonlab.diamer2.io.NCBIReader;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

import static org.junit.Assert.assertEquals;

public class TestIndexer {

    @Test
    public void testIndexer() throws IOException {
        Tree tree = NCBIReader.readTaxonomy(new File("src/test/resources/testNCBI/nodes.dmp"), new File("src/test/resources/testNCBI/names.dmp"));
        Indexer indexer = new Indexer(tree, 4, 50, 1, 64);
        indexer.indexDB(new File("src/test/resources/testNCBI/db.fsa"), Path.of("src/test/resources/testNCBI/index"));
        Bucket[] buckets = new Bucket[1024];
        for (int i = 0; i < 1024; i++) {
            buckets[i] = new Bucket(new File("src/test/resources/testNCBI/index/" + i + ".bin"));
        }

        HashMap<String, Integer> testKmers = new HashMap<>();
        testKmers.put("NQFLFAGIELILRKY", 1);
        testKmers.put("VIYGSGTTLQKQKSR", 1);
        testKmers.put("YEITVYQLSADDLRS", 0);
        testKmers.put("LEDNIRRVIADIRPQ", 0);

        testKmers.forEach((kmer, taxId) -> {
            System.out.println(kmer);
            long kmerEnc = AAEncoder.toBase11(kmer);
            int bucketId = (int) (kmerEnc & 0b1111111111);
            long kmerIndex = (kmerEnc << 10) & 0xffffffffffc00000L | taxId;
            assertEquals(Objects.requireNonNull(buckets[bucketId].getContent())[0], kmerIndex);
        });
    }
}
