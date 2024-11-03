package org.husonlab.diamer2.graph;

import java.util.ArrayList;

public class Node {
    int taxId;
    Node parent;
    Tree owner;
    ArrayList<Node> children;
    ArrayList<String> labels;
    String rank;

    public Node(Tree owner, int taxId, ArrayList<String> labels, String rank) {
        this.taxId = taxId;
        this.parent = null;
        this.owner = owner;
        this.children = new ArrayList<>();
        this.labels = labels;
        this.rank = rank;
        owner.registerNode(this);
    }

    public void addChild(Node child) {
        children.add(child);
    }

    public void addLabel(String label) {
        labels.add(label);
    }

    public void addAccession(String label) {
        owner.addAccession(this, label);
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    public String toString() {
        return "%s (%d)".formatted(this.labels.size() > 0 ? this.labels.get(0) : "", this.taxId);
    }

    public ArrayList<Node> getChildren() {
        return children;
    }

    public Node getParent() {
        return parent;
    }

    public int getTaxId() {
        return taxId;
    }

    public ArrayList<String> getLabels() {
        return labels;
    }

}
