package org.husonlab.diamer2.taxonomy;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;

public class Tree {
    public final HashMap<Integer, Node> idMap;
    @Nullable
    private Node root;
    @Nullable
    public final HashMap<String, Integer> accessionMap;
    public Tree(HashMap<Integer, Node> idMap, @Nullable HashMap<String, Integer> accessionMap) {
        this.idMap = idMap;
        this.accessionMap = accessionMap;
    }

    public Tree() {
        this.idMap = new HashMap<>();
        this.accessionMap = null;
    }

    public void setRoot(Node root) {
        this.root = root;
    }

    /**
     * Tries to find the root of the tree. Will work if the root has a self-loop or is the only node with no parent.
     * Fails if there are multiple nodes with no parent.
     */
    public void setRoot() {
        if (!idMap.isEmpty()) {
            root = pathToRoot(idMap.values().iterator().next()).getLast();
        } else {
            root = null;
        }
    }

    public Node findLCA(Node node1, Node node2) {
        ArrayList<Node> path1 = pathToRoot(node1);
        ArrayList<Node> path2 = pathToRoot(node2);
        Node mrca = null;
        int difference = path1.size() - path2.size();
        if (difference > 0) {
            for (int i = path2.size() - 1; i >= 0; i--) {
                if (path2.get(i).equals(path1.get(i + difference))) {
                    mrca = path2.get(i);
                } else {
                    break;
                }
            }
        } else {
            for (int i = path1.size() - 1; i >= 0; i--) {
                if (path1.get(i).equals(path2.get(i - difference))) {
                    mrca = path1.get(i);
                } else {
                    break;
                }
            }
        }
        return mrca;
    }

    public int findLCA(int taxId1, int taxId2) {
        return findLCA(idMap.get(taxId1), idMap.get(taxId2)).getTaxId();
    }

    public ArrayList<Node> pathToRoot(Node node) {
        ArrayList<Node> path = new ArrayList<>();
        while (node.hasParent()) {
            path.add(node);
            node = node.getParent();
        }
        path.add(node);
        return path;
    }

    public Tree getSubTree(ArrayList<Node> nodes) {
        Tree tree = new Tree();
        for (Node node : nodes) {
            Node nodeCopy = node.copy();
            tree.idMap.put(nodeCopy.getTaxId(), nodeCopy);
            while (node.hasParent()) {
                Node parent = node.getParent();
                if (!tree.idMap.containsKey(parent.getTaxId())) {
                    Node parentCopy = parent.copy();
                    tree.idMap.put(parentCopy.getTaxId(), parentCopy);
                }
                nodeCopy.setParent(tree.idMap.get(parent.getTaxId()));
                tree.idMap.get(parent.getTaxId()).addChild(nodeCopy);
                node = parent;
            }
        }
        tree.setRoot();
        return tree;
    }

    public Node getSpecies(Node node) {
        return getRank(node, "species");
    }

    public Node getGenus(Node node) {
        return getRank(node, "genus");
    }

    public Node getRank(Node node, String rank) {
        Node rankNode = null;
        Node previousNode = null;
        while (node != previousNode && node != null) {
            if (node.getRank().equals(rank)) {
                rankNode = node;
                break;
            }
            previousNode = node;
            node = node.getParent();
        }
        return rankNode;
    }

    public Node getRoot() {
        return root;
    }
}
