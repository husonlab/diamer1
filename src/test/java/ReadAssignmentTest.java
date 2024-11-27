import org.husonlab.diamer2.graph.Tree;
import org.husonlab.diamer2.io.NCBIReader;
import org.husonlab.diamer2.readAssignment.ReadAssigner;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ReadAssignmentTest {

    @Test
    public void testReadAssignment() throws IOException {
        Tree tree = NCBIReader.readTaxonomy(new File("src/test/resources/testNCBI/nodes.dmp"), new File("src/test/resources/testNCBI/names.dmp"));
        ReadAssigner readAssigner = new ReadAssigner(tree, 1);
        readAssigner.readHeaderIndex(new File("src/test/resources/testNCBI/reads_index/header_index.txt"));
        readAssigner.assignReads(Path.of("src/test/resources/testNCBI/db_index"), Path.of("src/test/resources/testNCBI/reads_index"));
        ReadAssigner.ReadAssignment[] readAssignments = readAssigner.getReadAssignments();
        assertEquals(6, readAssignments.length);
        assertEquals(1, readAssignments[0].taxIds().size());
        assertEquals(0, readAssignments[1].taxIds().size());
        assertEquals(0, readAssignments[2].taxIds().size());
        assertEquals(1, readAssignments[3].taxIds().size());
        assertEquals(2, readAssignments[4].taxIds().size());
        assertEquals(1, readAssignments[5].taxIds().size());
        assertNotNull(readAssignments[0].taxIds().get(2));
        assertNotNull(readAssignments[3].taxIds().get(0));
        assertNotNull(readAssignments[4].taxIds().get(1));
        assertNotNull(readAssignments[4].taxIds().get(5));
        assertNotNull(readAssignments[5].taxIds().get(0));
        assertEquals(79, (int)readAssignments[0].taxIds().get(2));
        assertEquals(1, (int)readAssignments[3].taxIds().get(0));
        assertEquals(21, (int)readAssignments[4].taxIds().get(1));
        assertEquals(1, (int)readAssignments[4].taxIds().get(5));
        assertEquals(1, (int)readAssignments[5].taxIds().get(0));
    }
}
