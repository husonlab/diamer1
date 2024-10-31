package org.husonlab.diamer2.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class Tree {

    Node root;
    ArrayList<Node> nodes;
    HashMap<String, Integer> labelMap;

    public Tree() {
        root = null;
        nodes = new ArrayList<>();
        labelMap = new HashMap<>();
    }

    public int add(Node node, boolean root) {
        if (root) {
            this.root = node;
        }
        nodes.add(node);
        int nodeId = nodes.size() - 1;
        labelMap.put(node.taxId, nodeId);
        return nodeId;
    }

    public int size() {
        return nodes.size();
    }

    public String toString() {
        return "Tree with %d nodes.".formatted(size());
    }

    public Node getRoot() {
        return root;
    }

    public ArrayList<Node> getNodes() {
        return nodes;
    }

    public Node byId(int nodeId) {
        return nodes.get(nodeId);
    }

    public Node byTaxId(int taxId) {
        return nodes.get(labelMap.get(taxId));
    }

    public Node addLeaf(String label1) {
        if (root == null) {
            new Node(this, label1);
            return this.getRoot();
        } else if (Objects.equals(this.root.label, label1)) {
            return this.getRoot();
        } else {
            throw new IllegalArgumentException(
                    "You are trying to change the root of the tree from %s to %s.".formatted(this.root.label, label1)
            );
        }
    }

    public Node addLeaf(String label1, String label2) {
        Node node1 = this.addLeaf(label1);
        return getOrCreateNode(label2, node1);
    }

    public Node addLeaf(String label1, String label2, String label3) {
        Node node2 = this.addLeaf(label1, label2);
        return getOrCreateNode(label3, node2);
    }

    public Node addLeaf(String label1, String label2, String label3, String label4) {
        Node node3 = this.addLeaf(label1, label2, label3);
        return getOrCreateNode(label4, node3);
    }

    public Node addLeaf(String label1, String label2, String label3, String label4, String label5) {
        Node node4 = this.addLeaf(label1, label2, label3, label4);
        return getOrCreateNode(label5, node4);
    }

    public Node addLeaf(String label1, String label2, String label3, String label4, String label5, String label6) {
        Node node5 = this.addLeaf(label1, label2, label3, label4, label5);
        return getOrCreateNode(label6, node5);
    }

    public Node addLeaf(String label1, String label2, String label3, String label4, String label5, String label6, String label7) {
        Node node6 = this.addLeaf(label1, label2, label3, label4, label5, label6);
        return getOrCreateNode(label7, node6);
    }

    public Node addLeaf(String label1, String label2, String label3, String label4, String label5, String label6, String label7, String label8) {
        Node node7 = this.addLeaf(label1, label2, label3, label4, label5, label6, label7);
        return getOrCreateNode(label8, node7);
    }

    private Node getOrCreateNode(String label, Node node) {
        // node of the given label already exists and is a child of the given node
        if (this.labelMap.containsKey(label) && this.byId(this.labelMap.get(label)).parent == node) {
            return this.byId(this.labelMap.get(label));
        // node of the given label already exists but is not a child of the given node
        } else if (this.labelMap.containsKey(label)) {
            throw new IllegalArgumentException("The node with label %s can not be a child of %s.".formatted(label, node.label));
        // node of the given label does not exist
        } else {
            return new Node(label, node);
        }
    }
}
