package org.husonlab.diamer2.readAssignment;

import org.husonlab.diamer2.taxonomy.Node;

public class ReadTaxonAssiciation {
    private final Node node;
    private int count;

    public ReadTaxonAssiciation(Node node) {
        this.node = node;
        this.count = 1;
    }

    public void incrementCount() {
        count++;
    }

    public Node getNode() {
        return node;
    }

    public int getCount() {
        return count;
    }

    @Override
    public String toString() {
        return node.getTaxId() + ": " + count;
    }
}
