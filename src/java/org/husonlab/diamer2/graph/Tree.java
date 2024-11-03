package org.husonlab.diamer2.graph;

import java.util.HashMap;

public class Tree {
    HashMap<Integer, Node> idMap;
    HashMap<String, Node> accessionMap;

    public Tree() {
        idMap = new HashMap<>(3500000);
        accessionMap = new HashMap<>(4000000);
    }

    public int size() {
        return idMap.keySet().size();
    }

    public String toString() {
        return "Tree with %d nodes.".formatted(size());
    }

    public HashMap<Integer, Node> getNodeMap() {
        return idMap;
    }

    public Node byId(int nodeId) {
        return idMap.get(nodeId);
    }

    public Node byLabel(String label) {
        return accessionMap.get(label);
    }

    public void registerNode(Node node) {
        idMap.put(node.getTaxId(), node);
    }

    public void addAccession(Node node, String label) {
        if (accessionMap.containsKey(label))
            throw new IllegalArgumentException("Label %s already exists in the tree.".formatted(label));
        accessionMap.put(label, node);
    }

    public HashMap<String, Node> getAccessionMap() {
        return accessionMap;
    }
}
