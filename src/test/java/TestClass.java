import org.husonlab.diamer2.io.taxonomy.TreeIO;
import org.husonlab.diamer2.taxonomy.Tree;
import org.junit.Test;

import java.nio.file.Path;

public class TestClass {

    @Test
    public void test() {
        Path treeFile = Path.of("src/test/resources/expected_output/db_index/tree.txt");
        Tree tree = TreeIO.loadTree(treeFile);
        System.out.println(tree);
    }

}
