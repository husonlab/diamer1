package org.husonlab.diamer2.taxonomy;

import org.jetbrains.annotations.Nullable;

import java.util.*;

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
    public Node setRoot() {
        if (!idMap.isEmpty()) {
            root = pathToRoot(idMap.values().iterator().next()).getLast();
            return root;
        } else {
            root = null;
            return null;
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

    public void addWeight(int taxId, int weight) {
        Node node = idMap.get(taxId);
        node.setWeight(node.getWeight() + weight);
    }

    /**
     * Adds weights to the nodes in the tree.
     * @param nodesAndWeights an array of arrays of arrays (size 2) of node IDs and weights
     *                        [[[nodeId1, weight1], [nodeId2, weight2], ...], ...]
     */
    public void addWeights(ArrayList<int[]>[] nodesAndWeights) {
        for (ArrayList<int[]> nodesAndWeight : nodesAndWeights) {
            addWeights(nodesAndWeight);
        }
    }

    /**
     * Adds weights to the nodes in the tree.
     * @param nodesAndWeights an array of arrays of node IDs and weights [[nodeId1, weight1], [nodeId2, weight2], ...]
     */
    public void addWeights(ArrayList<int[]> nodesAndWeights) {
        for (int[] nodeAndWeight : nodesAndWeights) {
            int nodeId = nodeAndWeight[0];
            int weight = nodeAndWeight[1];
            Node node = idMap.get(nodeId);
            node.setWeight(node.getWeight() + weight);
        }
    }

    public Tree getWeightedSubTree(ArrayList<int[]> nodesAndWeights) {
        Tree tree = new Tree();
        for (int[] nodeAndWeight : nodesAndWeights) {
            int nodeId = nodeAndWeight[0];
            int weight = nodeAndWeight[1];
            Node node = idMap.get(nodeId);
            Node finalNode = node;
            Node nodeCopy = tree.idMap.computeIfAbsent(nodeId, k -> finalNode.copy());
            nodeCopy.setWeight(weight);

            while (node.hasParent()) {
                Node parent = node.getParent();
                if (tree.idMap.containsKey(parent.getTaxId())) {
                    tree.idMap.get(parent.getTaxId()).addChild(nodeCopy);
                    nodeCopy.setParent(tree.idMap.get(parent.getTaxId()));
                    break;
                }
                Node parentCopy = tree.idMap.computeIfAbsent(parent.getTaxId(), k -> parent.copy());
                parentCopy.addChild(nodeCopy);
                nodeCopy.setParent(parentCopy);
                node = parent;
                nodeCopy = parentCopy;
            }
        }
        tree.setRoot();
        return tree;
    }

    public void accumulateWeights(Node root) {
        root.setCumulativeWeight(root.getWeight());
        if (root.isLeaf()) {
            return;
        }
        for (Node child : root.getChildren()) {
            accumulateWeights(child);
            root.setCumulativeWeight(root.getCumulativeWeight() + child.getCumulativeWeight());
        }
    }

    /**
     * Traverses the tree with BFS and collects the cummulative weight dependent on the rank and the threshold.
     * @return a hashmap with the rank as key and a hashmap with the taxId as key and the cummulative weight as value
     */
    public KummulativeWeightsPerRank[] getCummulativeWeightPerRank(int threshold) {
        HashMap<String, HashMap<Integer, Integer>> cummulativeWeightPerRank = new HashMap<>();
        LinkedList<Node> stack = new LinkedList<>();
        stack.add(root);
        while (!stack.isEmpty()) {
            Node node = stack.pollLast();
            if (node.getCumulativeWeight() >= threshold) {
                cummulativeWeightPerRank.computeIfPresent(node.getRank(), (k, v) -> {
                    v.put(node.getTaxId(), node.getCumulativeWeight());
                    return v;
                });
                cummulativeWeightPerRank.computeIfAbsent(node.getRank(), k -> new HashMap<>()).put(node.getTaxId(), node.getCumulativeWeight());
            }
            stack.addAll(node.getChildren());
        }

        ArrayList<KummulativeWeightsPerRank> result = new ArrayList<>();
        for (Map.Entry<String, HashMap<Integer, Integer>> entry : cummulativeWeightPerRank.entrySet()) {
            String rank = entry.getKey();
            int[][] kmerMatches = entry.getValue().entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .map(e -> new int[]{e.getKey(), e.getValue()})
                    .toArray(int[][]::new);
            result.add(new KummulativeWeightsPerRank(rank, kmerMatches));
        }

        result.sort(Comparator.comparingInt(k -> pathToRoot(idMap.get(k.taxonWeight()[0][0])).size()));
        return result.toArray(new KummulativeWeightsPerRank[0]);
    }

    public void resetWeights() {
        for (Node node : idMap.values()) {
            node.setWeight(0);
            node.setCumulativeWeight(0);
        }
    }

    public int getSpecies(int taxId) {
        Node node = getSpecies(idMap.get(taxId));
        return node == null ? -1 : node.getTaxId();
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

    public record KummulativeWeightsPerRank(String rank, int[][] taxonWeight) {
    }
}
