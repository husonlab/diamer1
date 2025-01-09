import org.husonlab.diamer2.io.accessionMapping.MeganMapping;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class TestClass {

    @Test
    public void test() throws IOException {
        MeganMapping mapping = new MeganMapping(new File("F:\\Studium\\Master\\semester5\\thesis\\data\\megan_map\\megan-map-Feb2022.db"));
        System.out.println(mapping.getTaxId("WP_094582345"));
    }
}
