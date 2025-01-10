import org.husonlab.diamer2.indexing.Bucket;
import org.husonlab.diamer2.io.indexing.BucketIO;
import org.husonlab.diamer2.io.indexing.DBIndexIO;
import org.husonlab.diamer2.taxonomy.MeganMapping;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class TestClass {

    @Test
    public void test() throws IOException {
        Tree tree = NCBIReader.readTaxonomy(
                new File("F:\\Studium\\Master\\semester5\\thesis\\data\\NCBI\\taxdmp\\nodes.dmp"),
                new File("F:\\Studium\\Master\\semester5\\thesis\\data\\NCBI\\taxdmp\\names.dmp"));
        tree.reduceToStandardRanks();
        System.out.println("");
    }
}
