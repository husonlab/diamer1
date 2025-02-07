package org.husonlab.diamer2.taxonomy;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Tree {

    /**
     * The standard ranks in the NCBI taxonomy.
     */
    HashSet<String> standardRanks = new HashSet<>(
            Arrays.asList("superkingdom", "kingdom", "phylum", "class", "order", "family", "genus", "species"));
    /**
     * Maps all taxonomic IDs to their respective nodes.
     */
    public final HashMap<Integer, Node> idMap;
    @Nullable
    private Node root;

    /**
     * Map description -> index to map descriptions to long node properties.
     */
    private final HashMap<String, Integer> longPropertyDescriptions;
    /**
     * Map description -> index to map descriptions to double node properties.
     */
    private final HashMap<String, Integer> doublePropertyDescriptions;

    public Tree() {
        this.idMap = new HashMap<>();
        longPropertyDescriptions = new HashMap<>();
        doublePropertyDescriptions = new HashMap<>();
    }

    public Node getRoot() {
        if (root == null) {
            autoFindRoot();
        }
        return root;
    }

    /**
     * Lists all nodes on the path from the given node to the root, including the node itself and the root.
     * @param node the node to start from
     * @return a list of nodes on the path from the given node to the root
     */
    @Contract("null -> null")
    public ArrayList<Node> pathToRoot(Node node) {
        if (node == null) {
            return null;
        }
        ArrayList<Node> path = new ArrayList<>();
        while (node.hasParent()) {
            path.add(node);
            node = node.getParent();
        }
        path.add(node);
        return path;
    }

    /**
     * Tries to find the root of the tree and sets it.
     * @return the root of the tree or null if the tree is empty
     */
    public Node autoFindRoot() {
        if (!idMap.isEmpty()) {
            root = pathToRoot(idMap.values().iterator().next()).getLast();
            return root;
        } else {
            root = null;
            return null;
        }
    }

    /**
     * Funds the lowest common ancestor of two nodes.
     * @param node1 the first node
     * @param node2 the second node
     * @return the lowest common ancestor of the two nodes or null if the nodes are null or not in the same tree.
     */
    @Contract("null, null -> null; null, _ -> _; _, null -> _")
    public Node findLCA(Node node1, Node node2) {
        if (node1 == null || node2 == null) {
            if (node1 != null) {
                return node1;
            } else return node2;
        }
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

    /**
     * Finds the lowest common ancestor of two nodes given their taxonomic IDs.
     * @param taxId1 the first taxonomic ID
     * @param taxId2 the second taxonomic ID
     * @return the taxonomic ID of the lowest common ancestor or -1 if the nodes are not in this tree.
     */
    public int findLCA(int taxId1, int taxId2) {
        Node lca = findLCA(idMap.get(taxId1), idMap.get(taxId2));
        return lca == null ? -1 : lca.getTaxId();
    }

    /**
     * Set a property of type double for a node in the tree.
     * @param taxId the taxonomic ID of the node
     * @param label the label of the property
     * @param value the value of the property
     */
    public void setNodeProperty(int taxId, String label, long value) {
        Node node = ensureLongProperty(taxId, label);
        node.longProperties.set(longPropertyDescriptions.get(label), value);
    }

    /**
     * Set a property of type double for a node in the tree.
     * @param taxId the taxonomic ID of the node
     * @param label the label of the property
     * @param value the value of the property
     */
    public void setNodeProperty(int taxId, String label, double value) {
        Node node = ensureDoubleProperty(taxId, label);
        node.doubleProperties.set(doublePropertyDescriptions.get(label), value);
    }

    /**
     * Add a value to a property of type long for a node in the tree.
     * @param taxId the taxonomic ID of the node
     * @param label the label of the property
     * @param value the value to add
     */
    public void addToNodeProperty(int taxId, String label, long value) {
        Node node = ensureLongProperty(taxId, label);
        int index = longPropertyDescriptions.get(label);
        node.longProperties.set(index, node.longProperties.get(index) + value);
    }

    /**
     * Add a value to a property of type double for a node in the tree.
     * @param taxId the taxonomic ID of the node
     * @param label the label of the property
     * @param value the value to add
     */
    public void addToNodeProperty(int taxId, String label, double value) {
        Node node = ensureDoubleProperty(taxId, label);
        int index = doublePropertyDescriptions.get(label);
        node.doubleProperties.set(index, node.doubleProperties.get(index) + value);
    }

    /**
     * Ensures that a property of type long exists for a node in the tree.
     * @param taxId the taxonomic ID of the node
     * @param label the label of the property
     * @return the node corresponding to the taxonomic ID
     */
    private Node ensureLongProperty(int taxId, String label) {
        if (!idMap.containsKey(taxId)) {
            throw new RuntimeException("Tried to set property of non-existing node: " + taxId);
        }
        Node node = idMap.get(taxId);
        if (!longPropertyDescriptions.containsKey(label)) {
            longPropertyDescriptions.put(label, longPropertyDescriptions.size());
        }
        while (node.longProperties.size() <= longPropertyDescriptions.get(label)) {
            node.longProperties.add(0L);
        }
        return node;
    }

    /**
     * Ensures that a property of type double exists for a node in the tree.
     * @param taxId the taxonomic ID of the node
     * @param label the label of the property
     * @return the node corresponding to the taxonomic ID
     */
    private Node ensureDoubleProperty(int taxId, String label) {
        if (!idMap.containsKey(taxId)) {
            throw new RuntimeException("Tried to set property of non-existing node: " + taxId);
        }
        Node node = idMap.get(taxId);
        if (!doublePropertyDescriptions.containsKey(label)) {
            doublePropertyDescriptions.put(label, doublePropertyDescriptions.size());
        }
        while (node.doubleProperties.size() <= doublePropertyDescriptions.get(label)) {
            node.doubleProperties.add(0.0);
        }
        return node;
    }

    public Set<String> getNodeLongPropertyLabels() {
        return longPropertyDescriptions.keySet();
    }

    public Set<String> getNodeDoublePropertyLabels() {
        return doublePropertyDescriptions.keySet();
    }

    /**
     * Get a property of type long for a node in the tree.
     * @param taxId the taxonomic ID of the node
     * @param label the label of the property
     * @return the value of the property
     */
    public long getNodeLongProperty(int taxId, String label) {
        if (!idMap.containsKey(taxId)) {
            throw new RuntimeException("Tried to get property of non-existing node: " + taxId);
        }
        Node node = idMap.get(taxId);
        if (!longPropertyDescriptions.containsKey(label)) {
            throw new RuntimeException("Tried to access non-existing property: " + label);
        }
        return node.longProperties.get(longPropertyDescriptions.get(label));
    }

    /**
     * Get a property of type double for a node in the tree.
     * @param taxId the taxonomic ID of the node
     * @param label the label of the property
     * @return the value of the property
     */
    public double getNodeDoubleProperty(int taxId, String label) {
        if (!idMap.containsKey(taxId)) {
            throw new RuntimeException("Tried to get property of non-existing node: " + taxId);
        }
        Node node = idMap.get(taxId);
        if (!doublePropertyDescriptions.containsKey(label)) {
            throw new RuntimeException("Tried to access non-existing property: " + label);
        }
        return node.doubleProperties.get(doublePropertyDescriptions.get(label));
    }

    /**
     * Resets the numeric properties of all nodes
     */
    public void resetNodeProperties() {
        longPropertyDescriptions.clear();
        doublePropertyDescriptions.clear();
        for (Node node : idMap.values()) {
            node.longProperties.clear();
            node.doubleProperties.clear();
        }
    }

    public void accumulateNodePropertiy(String label, String targetLabel) {
        if (!longPropertyDescriptions.containsKey(label) && !doublePropertyDescriptions.containsKey(label)) {
            throw new RuntimeException("Tried to access non-existing property: " + label);
        }
        if (!longPropertyDescriptions.containsKey(label)) {
            accumulateNodeLongProperty(label, targetLabel, getRoot());
        }
        if (!doublePropertyDescriptions.containsKey(label)) {
            accumulateNodeDoubleProperty(label, targetLabel, getRoot());
        }
    }

    private void accumulateNodeLongProperty(String label, String targetLabel, Node root) {
        if (root.isLeaf()) {
            setNodeProperty(root.getTaxId(), targetLabel, getNodeLongProperty(root.getTaxId(), label));
        } else {
            for (Node child : root.getChildren()) {
                accumulateNodeLongProperty(label, targetLabel, child);
                addToNodeProperty(root.getTaxId(), targetLabel, getNodeLongProperty(child.getTaxId(), targetLabel));
            }
        }
    }

    private void accumulateNodeDoubleProperty(String label, String targetLabel, Node root) {
        if (root.isLeaf()) {
            setNodeProperty(root.getTaxId(), targetLabel, getNodeDoubleProperty(root.getTaxId(), label));
        } else {
            for (Node child : root.getChildren()) {
                accumulateNodeDoubleProperty(label, targetLabel, child);
                addToNodeProperty(root.getTaxId(), targetLabel, getNodeDoubleProperty(child.getTaxId(), targetLabel));
            }
        }
    }

    /**
     * recursively accumulates the weights of all nodes in the tree starting from the root.
     */
    @Deprecated
    private void accumulateWeights(Node root) {
        if (root.isLeaf()) {
            return;
        }
        for (Node child : root.getChildren()) {
            accumulateWeights(child);
            root.addWeight(child.getWeight());
        }
    }

    /**
     * Accumulates the weights of all nodes in the tree.
     */
    @Deprecated
    public void accumulateWeights() {
        if (root != null) {
            accumulateWeights(root);
        } else if (autoFindRoot() != null) {
            accumulateWeights(root);
        }
    }

    /**
     * Adds the weight of each node to the nodes custom values.
     * @param description the description of the custom value that is added
     */
    @Deprecated
    public void transferWeightToCustomValue(String description) {
        for (Node node : idMap.values()) {
            setNodeProperty(node.getTaxId(), description, node.getWeight());
        }
    }

    /**
     * Searches for the node with the given rank in the path from the given node to the root.
     * @param node the node to start from
     * @param rank the rank to search for
     * @return the node with the given rank or null if the rank was not found
     */
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

    /**
     * Creates a new subtree with the nodes and weights given in the array.
     * <p>The subtree will contain the root of the original tree and all nodes that lie on the paths from each input
     * node to the root. The accumulated weights will not be set.</p>
     * @param nodesAndWeights an array of arrays of node IDs and weights [[nodeId1, weight1], [nodeId2, weight2], ...]
     * @return a new tree with the nodes and weights given in the array
     */
    public Tree getWeightedSubTree(List<int[]> nodesAndWeights) {

        Tree subTree = new Tree();

        for (int[] nodeAndWeight : nodesAndWeights) {
            int nodeId = nodeAndWeight[0];
            int weight = nodeAndWeight[1];

            // Skip nodes that are not in the tree
            if (!idMap.containsKey(nodeId)) {
                continue;
            }
            // Get the taxonomic ID of the node the id is mapped to.
            // This can happen if the tree was reduced to the standard ranks before.
            nodeId = idMap.get(nodeId).getTaxId();

            // In case the node is already in the new tree, only the weight is updated
            if (subTree.idMap.containsKey(nodeId)) {
                subTree.idMap.get(nodeId).addWeight(weight);
                continue;
            }

            // copy node to new tree
            Node node = idMap.get(nodeId);
            Node nodeCopy = node.copy();
            nodeCopy.setWeight(weight);

            // Add all nodes on the path to the root to the new tree (if they are not already in the new tree)
            while (node.hasParent()) {
                subTree.idMap.put(nodeId, nodeCopy);
                Node parent = node.getParent();
                if (subTree.idMap.containsKey(parent.getTaxId())) {
                    subTree.idMap.get(parent.getTaxId()).addChild(nodeCopy);
                    nodeCopy.setParent(subTree.idMap.get(parent.getTaxId()));
                    break;
                }
                Node parentCopy = parent.copy();
                parentCopy.addChild(nodeCopy);
                nodeCopy.setParent(parentCopy);
                node = parent;
                nodeCopy = parentCopy;
                nodeId = parent.getTaxId();
            }

            // Add the root node to the new tree
            subTree.idMap.put(nodeId, nodeCopy);
        }
        subTree.autoFindRoot();
        return subTree;
    }

    /**
     * Reduces a phylogenetic tree to the 8 standard ranks
     * (superkingdom, kingdom, phylum, class, order, family, genus, species).
     * <p>
     *     Each node with a rank different from the standard ranks is removed and the children and weights are added to
     *     the next higher node with a standard rank. The idMap will still contain the removed nodes taxIds, but they
     *     will map to the higher node.
     * </p>
     */
    public void reduceToStandardRanks() {
        // DFS over the tree
        Stack<Node> stack = new Stack<>();
        stack.add(root);
        while (!stack.isEmpty()) {
            Node node = stack.pop();
            // check for parent to avoid removing the root
            if (!standardRanks.contains(node.getRank()) && node.hasParent()) {
                Node parent = node.getParent();
                // move up the tree until a standard rank is found
                while (parent.hasParent() && !standardRanks.contains(parent.getRank())) {
                    parent = parent.getParent();
                }
                // add weights and children to the standard-rank parent
                parent.addWeight(node.getWeight());
                node.getParent().getChildren().remove(node);
                for (Node child : node.getChildren()) {
                    parent.addChild(child);
                    child.setParent(parent);
                }
                // keep "old" taxId in idMap but map to new node
                idMap.put(node.getTaxId(), parent);
            }
            stack.addAll(node.getChildren());
        }
    }

    /**
     * @return a HashMap with an ArrayList for every rank containing all nodes of that rank
     */
    public HashMap<String, ArrayList<Node>> getNodesPerRank() {
        HashMap<String, ArrayList<Node>> nodesPerRank = new HashMap<>();
        for (Node node : idMap.values()) {
            String rank = node.getRank();
            if (!nodesPerRank.containsKey(rank)) {
                nodesPerRank.put(rank, new ArrayList<>());
            }
            nodesPerRank.get(rank).add(node);
        }
        return nodesPerRank;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Tree tree = (Tree) o;
        return Objects.equals(idMap, tree.idMap) && Objects.equals(root, tree.root);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idMap, root);
    }

    @Deprecated
    public record WeightsPerRank(String rank, long totalWeight, int[][] taxonWeights) {
    }
}
