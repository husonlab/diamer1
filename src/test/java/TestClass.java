import org.husonlab.diamer2.io.Utilities;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class TestClass {

    @Test
    public void test() throws IOException {
        int test = Utilities.approximateNumberOfSequences(new File("F:\\Studium\\Master\\semester5\\thesis\\data\\NCBI\\100\\nr100.fsa"), "\n>");
        System.out.println(test);
    }
}
