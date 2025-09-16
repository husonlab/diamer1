package io;

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
        System.out.println(ncbiTree.hasNode(0));
    }
}
