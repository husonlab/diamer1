import org.husonlab.diamer2.indexing.Bucket;
import org.husonlab.diamer2.io.indexing.BucketIO;
import org.husonlab.diamer2.io.indexing.DBIndexIO;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;

public class TestClass {

    @Test
    public void test() throws IOException {
        DBIndexIO dbIndexIO = new DBIndexIO(Path.of("F:\\Studium\\Master\\semester5\\thesis\\data\\NCBI\\100\\index"));

        for (int i = 0; i < 12; i++) {
            BucketIO bucketIO = dbIndexIO.getBucketIO(i);
            if (bucketIO.exists()) {
                Bucket bucket = bucketIO.read();
                System.out.println(bucket.getName() + " " + bucket.getSize());
            }
        }
    }

}
