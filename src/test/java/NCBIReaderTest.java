import org.husonlab.diamer2.io.NCBIReader;
import org.husonlab.diamer2.taxonomy.Tree;
import org.junit.Test;

import static org.junit.Assert.*;
import java.io.File;

public class NCBIReaderTest {
    @Test
    public void testTaxonomyReader() {
        Tree tree = NCBIReader.readTaxonomy(
                new File("src/test/resources/database/nodes.dmp"),
                new File("src/test/resources/database/names.dmp"));
        assertEquals(6, tree.idMap.size());
        assertEquals(3, tree.idMap.get(3).getLabels().size());
        assertEquals("organism1", tree.idMap.get(3).getScientificName());
        assertEquals(tree.idMap.get(2), tree.idMap.get(3).getParent());
    }
}
