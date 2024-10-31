package org.husonlab.diamer2.graph;

import org.husonlab.diamer2.graph.Tree;

import java.util.ArrayList;

public class Node {
    String label;
    int id;
    Node parent;
    ArrayList<Node> children = new ArrayList<>();
    Tree owner;

    public Node(Tree owner, String label) {
        this.label = label;
        this.parent = null;
        this.owner = owner;
        this.id = owner.add(this);
    }

    public Node(String label, Node parent) {
        this.label = label;
        this.parent = parent;
        parent.addChild(this);
        this.owner = parent.owner;
        this.id = owner.add(this);
    }

    public void addChild(Node child) {
        children.add(child);
    }

    public String toString() {
        return "%s (%d)".formatted(this.label, this.id);
    }

    public ArrayList<Node> getChildren() {
        return children;
    }

    public Node getParent() {
        return parent;
    }

    public int getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

}
