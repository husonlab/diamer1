package org.husonlab.diamer2.graph;

import java.util.ArrayList;
import java.util.Objects;

public class Node {
    private final int taxId;                // required
    private Node parent;                    // can be null
    private final ArrayList<Node> children; // can be empty
    private final ArrayList<String> labels; // can be empty
    private String rank;                    // can be null

    public Node(int taxId) {
        this.taxId = taxId;
        this.children = new ArrayList<Node>();
        this.labels = new ArrayList<String>();
    }

    public Node(int taxId, Node parent) {
        this.taxId = taxId;
        this.parent = parent;
        this.children = new ArrayList<Node>();
        this.labels = new ArrayList<String>();
    }

    public Node(int taxId, String rank) {
        this.taxId = taxId;
        this.rank = rank;
        this.children = new ArrayList<Node>();
        this.labels = new ArrayList<String>();
    }

    public Node(int taxId, Node parent, String rank) {
        this.taxId = taxId;
        this.parent = parent;
        this.rank = rank;
        this.children = new ArrayList<Node>();
        this.labels = new ArrayList<String>();
    }

    public void addChild(Node child) {
        children.add(child);
    }

    public void addLabel(String label) {
        labels.add(label);
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    public String toString() {
        return "(%s) %s (%d)"
                .formatted(
                        !Objects.isNull(this.rank) ? this.rank : "no rank",
                        this.labels.size() > 0 ? this.labels.get(0) : "no labels",
                        this.taxId
                );
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

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }
}