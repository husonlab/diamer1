import org.husonlab.diamer2.graph.*;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class GraphTest {
    @Test
    public void testTree() {
        Tree tree = new Tree();
        Node node1 = new Node(tree, "node1");
        // second root node
        assertThrows(IllegalArgumentException.class, () -> new Node(tree, "fail"));
        // second node with same label
        assertThrows(IllegalArgumentException.class, () -> new Node("node1", node1));
        Node node2 = new Node("node2", node1);
        Node node3 = new Node("node3", node2);
        Node node4 = new Node("node4", node2);
        assertEquals(node1, tree.getRoot());
        // tree has 4 nodes in total
        assertEquals(4, tree.size());
        // node 2 has two children
        assertEquals(2, node2.getChildren().size());
        // node 2 is the parent of node 3
        assertEquals(node2, node3.getParent());
        // node 2 is the parent of node 4
        assertEquals(node3, node2.getChildren().get(0));
        // node 4 has id 3
        assertEquals(3, node4.getId());
        // node 4 can be accessed by its id in the tree
        assertEquals(node4, tree.getNodes().get(3));
        // node 2 can be accessed by its label in the tree
        assertEquals(node2, tree.byLabel("node2"));
        // node 2 can be accessed by its id in the tree
        assertEquals(node2, tree.byId(1));
        assertEquals("node2 (1)", node2.toString());
        assertEquals("Tree with 4 nodes.", tree.toString());

        tree.addLeaf("node1", "node2", "node4", "node5", "node6");
        assertEquals(6, tree.size());
        assertThrows(IllegalArgumentException.class, () -> tree.addLeaf("node1", "node2", "node4", "node6", "node5"));
    }
}
