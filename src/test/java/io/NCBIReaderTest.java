package io;

import org.husonlab.diamer.io.NCBIReader;
import org.husonlab.diamer.taxonomy.Node;
import org.husonlab.diamer.taxonomy.Tree;
import static utils.Utilities.assertIsIn;
import org.junit.Test;
import static org.junit.Assert.*;

import java.nio.file.Path;

import static org.husonlab.diamer.io.NCBIReader.readNCBITree;

public class NCBIReaderTest {

    Path nodesFile = Path.of("src/test/resources/ncbi_files/taxonomy/nodes.dmp");
    Path namesFile = Path.of("src/test/resources/ncbi_files/taxonomy/names.dmp");

    @Test
    public void testReadNCBITree() {
        Tree ncbiTree = readNCBITree(nodesFile, namesFile, false);
        assertEquals(62, ncbiTree.size());
        assertEquals("Bacteria", ncbiTree.getNode(2).getScientificNameOrFirstLabel());
        assertIsIn("prokaryotes", ncbiTree.getNode(2).getAllNames());
        assertEquals("family", ncbiTree.getNode(90014).getRank());
        assertEquals(22, ncbiTree.getNode(25).getParent().getTaxId());
        assertEquals(267890, ncbiTree.getNode(22).getParent().getTaxId());
        assertEquals(90014, ncbiTree.getNode(267890).getParent().getTaxId());
        assertEquals(90012, ncbiTree.getNode(90014).getParent().getTaxId());
        assertEquals(90008, ncbiTree.getNode(90012).getParent().getTaxId());
        assertEquals(335928, ncbiTree.getNode(90008).getParent().getTaxId());
        assertEquals(131567, ncbiTree.getNode(335928).getParent().getTaxId());
        assertEquals(2, ncbiTree.getNode(131567).getParent().getTaxId());
        assertEquals(1, ncbiTree.getNode(2).getParent().getTaxId());
    }

    @Test
    public void testReadNcbiTreeSQLite() {
        Path sqliteFile = Path.of("src/test/resources/blastdbcmd/taxonomy4blast.sqlite3");
        Tree tree = NCBIReader.readNCBITree(sqliteFile);
        // ensure that the correct root was found
        assertEquals(1, tree.getRoot().getTaxId());
        // check path from echerichia coli to the root
        Node leaf = tree.getNode(562);
        assertNotNull(leaf);
        Node parent = leaf.getParent();
        assertEquals(561, parent.getTaxId());
        parent = parent.getParent();
        assertEquals(543, parent.getTaxId());
        parent = parent.getParent();
        assertEquals(91347, parent.getTaxId());
        parent = parent.getParent();
        assertEquals(1236, parent.getTaxId());
        parent = parent.getParent();
        assertEquals(1224, parent.getTaxId());
        parent = parent.getParent();
        assertEquals(3379134, parent.getTaxId());
        parent = parent.getParent();
        assertEquals(2, parent.getTaxId());
        parent = parent.getParent();
        assertEquals(131567, parent.getTaxId());
        parent = parent.getParent();
        assertEquals(1, parent.getTaxId());
        System.out.println(tree);
    }
}
