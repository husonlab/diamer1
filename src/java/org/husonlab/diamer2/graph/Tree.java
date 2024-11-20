package org.husonlab.diamer2.graph;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class Tree {
    public final ConcurrentHashMap<Integer, Node> idMap;
    @Nullable
    public final ConcurrentHashMap<String, Integer> accessionMap;
    public Tree(ConcurrentHashMap<Integer, Node> idMap, @Nullable ConcurrentHashMap<String, Integer> accessionMap) {
        this.idMap = idMap;
        this.accessionMap = accessionMap;
    }

    public Tree(ConcurrentHashMap<Integer, Node> idMap) {
        this.idMap = idMap;
        this.accessionMap = null;
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
        Node prevNode = null;
        while (node != prevNode) {
            path.add(node);
            prevNode = node;
            node = node.getParent();
        }
        return path;
    }
}
