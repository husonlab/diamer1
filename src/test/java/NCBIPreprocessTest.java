import org.husonlab.diamer2.graph.Tree;
import org.husonlab.diamer2.io.NCBIReader;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class NCBIPreprocessTest {
    @Test
    public void testNCBIPreprocess() throws IOException {
        Tree tree = NCBIReader.readTaxonomyWithAccessions(
                new File("src/test/resources/testNCBI/nodes.dmp"),
                new File("src/test/resources/testNCBI/names.dmp"),
                new NCBIReader.AccessionMapping[] {
                        new NCBIReader.AccessionMapping("src/test/resources/testNCBI/prot.accession2taxid.gz", 1, 2),
                }, true);
        NCBIReader.preprocessNR(new File("src/test/resources/testNCBI/db.fsa"), new File("src/test/resources/testNCBI/db_preprocessed.fsa"), tree);
    }
}
