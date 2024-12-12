package org.husonlab.diamer2.readAssignment;

import org.husonlab.diamer2.taxonomy.Node;

import java.util.LinkedList;

public class Read {

    private final String header;
    private final LinkedList<ReadTaxonAssiciation> readTaxonAssiciations;

    public Read(String header) {
        this.header = header;
        this.readTaxonAssiciations = new LinkedList<>();
    }

    public void addReadAssignment(Node node) {
        synchronized (this) {
            for (ReadTaxonAssiciation readTaxonAssociation : readTaxonAssiciations) {
                if (readTaxonAssociation.getNode() == node) {
                    readTaxonAssociation.incrementCount();
                    return;
                }
            }
            readTaxonAssiciations.add(new ReadTaxonAssiciation(node));
        }
    }

    public LinkedList<ReadTaxonAssiciation> getSortedAssociations() {
        LinkedList<ReadTaxonAssiciation> sortedAssociations = new LinkedList<>(readTaxonAssiciations);
        sortedAssociations.sort((a1, a2) -> Integer.compare(a2.getCount(), a1.getCount()));
        return sortedAssociations;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(header).append("\t");
        getSortedAssociations().forEach(readTaxonAssiciation -> sb
                .append(readTaxonAssiciation.getNode().getTaxId())
                .append(":")
                .append(readTaxonAssiciation.getCount()).append(" "));
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }
}
