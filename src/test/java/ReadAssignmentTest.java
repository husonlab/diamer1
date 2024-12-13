import org.husonlab.diamer2.readAssignment.ReadAssignment;
import org.husonlab.diamer2.taxonomy.Tree;
import org.husonlab.diamer2.io.NCBIReader;
import org.husonlab.diamer2.readAssignment.ReadAssigner;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ReadAssignmentTest {

    @Test
    public void testReadAssignment() throws Exception {
        Tree tree = NCBIReader.readTaxonomy(new File("src/test/resources/database/nodes.dmp"), new File("src/test/resources/database/names.dmp"));
        ReadAssigner readAssigner = new ReadAssigner(tree,1, Path.of("src/test/resources/database/index"), Path.of("src/test/resources/reads/index"));
        ReadAssignment assignment = readAssigner.assignReads();
        assertEquals(6, assignment.size());
//        assertEquals(1, reads[0].readAssignments().size());
//        assertEquals(0, reads[1].readAssignments().size());
//        assertEquals(0, reads[2].readAssignments().size());
//        assertEquals(1, reads[3].readAssignments().size());
//        assertEquals(2, reads[4].readAssignments().size());
//        assertEquals(1, reads[5].readAssignments().size());
//        assertNotNull(reads[0].readAssignments().get(2));
//        assertNotNull(reads[3].readAssignments().get(0));
//        assertNotNull(reads[4].readAssignments().get(1));
//        assertNotNull(reads[4].readAssignments().get(5));
//        assertNotNull(reads[5].readAssignments().get(0));
//        assertEquals(79, (int) reads[0].readAssignments().get(2));
//        assertEquals(1, (int) reads[3].readAssignments().get(0));
//        assertEquals(21, (int) reads[4].readAssignments().get(1));
//        assertEquals(1, (int) reads[4].readAssignments().get(5));
//        assertEquals(1, (int) reads[5].readAssignments().get(0));
    }
}
