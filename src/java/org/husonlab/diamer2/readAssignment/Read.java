package org.husonlab.diamer2.readAssignment;

import org.husonlab.diamer2.taxonomy.Node;

import java.util.ArrayList;
import java.util.LinkedList;

public class Read {

    private final String header;
    private final ArrayList<int[]> readTaxonAssiciations;
    private Node assignedNode;

    public Read(String header) {
        this.header = header;
        this.readTaxonAssiciations = new ArrayList<>();
    }

    /**
     * Adds a read assignment to the read. If the node is already assigned to the read, the count is incremented.
     *
     * @param node the node to assign
     */
    public void addReadAssignment(Node node) {
        synchronized (this) {
            for (int[] readAssociation : readTaxonAssiciations) {
                int taxId = node.getTaxId();
                if (readAssociation[0] == taxId) {
                    readAssociation[1]++;
                    return;
                }
            }
            readTaxonAssiciations.add(new int[]{node.getTaxId(), 1});
        }
    }

    /**
     * Adds a read assignment to the read. If the node is already assigned to the read, the count is replaced.
     *
     * @param node  the node to assign
     * @param count the count of the assignment
     */
    public void addReadAssignment(Node node, int count) {
        int taxId = node.getTaxId();
        if (readTaxonAssiciations.contains(taxId)) {
            readTaxonAssiciations.get(taxId)[1] = count;
        } else {
            readTaxonAssiciations.add(new int[]{taxId, count});
        }
    }

    public void addReadAssignment(int taxId) {
        if (readTaxonAssiciations.contains(taxId)) {
            readTaxonAssiciations.get(taxId)[1]++;
        } else {
            readTaxonAssiciations.add(new int[]{taxId, 1});
        }
    }

    public void addReadAssignment(int taxId, int count) {
        if (readTaxonAssiciations.contains(taxId)) {
            readTaxonAssiciations.get(taxId)[1] = count;
        } else {
            readTaxonAssiciations.add(new int[]{taxId, count});
        }
    }

    public void setAssignedNode(Node node) {
        assignedNode = node;
    }

    public void sortAssociations() {
        readTaxonAssiciations.sort((a1, a2) -> Integer.compare(a2[1], a1[1]));
    }

    public ArrayList<int[]> getAssociations() {
        return readTaxonAssiciations;
    }

    public Node getAssignedNode() {
        return assignedNode;
    }

    public String getHeader() {
        return header;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(header).append("\t");
        sortAssociations();
        getAssociations().forEach(readTaxonAssiciation -> sb
                .append(readTaxonAssiciation[0])
                .append(":")
                .append(readTaxonAssiciation[1]).append(" "));
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }
}
