package org.husonlab.diamer.taxonomy;

import org.husonlab.diamer.readAssignment.ReadAssignment;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

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
    private final ArrayList<Long> defaultLongProperties;
    /**
     * Map description -> index to map descriptions to double node properties.
     */
    private final HashMap<String, Integer> doublePropertyDescriptions;
    private final ArrayList<Double> defaultDoubleProperties;

    /**
     * Construct an empty tree.
     */
    public Tree() {
        this.idMap = new HashMap<>();
        longPropertyDescriptions = new HashMap<>();
        defaultLongProperties = new ArrayList<>();
        doublePropertyDescriptions = new HashMap<>();
        defaultDoubleProperties = new ArrayList<>();
    }

    /**
     * @param taxId Taxonomic ID of a node
     * @return {@code true} if the tree contains the specified node
     */
    public boolean hasNode(int taxId) {
        return idMap.containsKey(taxId);
    }

    /**
     * Adds a node to the tree.
     * @param taxId Taxonomic ID of the node
     * @param node the node to add
     */
    public void addNode(int taxId, Node node) {
        for (int i = node.longProperties.size(); i < defaultLongProperties.size(); i++) {
            node.longProperties.add(defaultLongProperties.get(i));
        }
        for (int i = node.doubleProperties.size(); i < defaultDoubleProperties.size(); i++) {
            node.doubleProperties.add(defaultDoubleProperties.get(i));
        }
        synchronized (idMap) {
            idMap.put(taxId, node);
        }
    }

    /**
     * @param taxId Taxonomic ID
     * @return Node with the input taxonomic ID or {@code null} if the tree does not contain the node
     */
    @Nullable
    public Node getNode(int taxId) {
        if (idMap.containsKey(taxId)) {
            return idMap.get(taxId);
        }
        return null;
    }

    /**
     * @param taxId the taxonomic ID of the node
     * @return the node corresponding to the taxonomic ID
     * @throws RuntimeException if the node does not exist
     */
    private Node getNodeOrError(int taxId) {
        if (!idMap.containsKey(taxId)) {
            throw new RuntimeException("Tried access non-existing node: " + taxId);
        }
        return idMap.get(taxId);
    }

    @Nullable
    public Node getRoot() {
        if (root == null) {
            autoFindRoot();
        }
        return root;
    }

    public Node getRoorOrError() {
        if (getRoot() == null) {
            throw new RuntimeException("Root not found");
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
     * Finds the lowest common ancestor of two nodes.
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

    public Node findLCAByNodes(Node[] nodes) {
        ArrayList<ArrayList<Node>> pathsToRoot = new ArrayList<>();
        for (Node node : nodes) {
            if (node != null) {
                pathsToRoot.add(pathToRoot(node));
            }
        }
        if (pathsToRoot.isEmpty()) {
            return null;
        }
        int[] differences = new int[pathsToRoot.size()];
        for (int i = 0; i < pathsToRoot.size(); i++) {
            differences[i] = pathsToRoot.get(i).size() - pathsToRoot.getFirst().size();
        }
        Node mrca = null;
        for (int i = pathsToRoot.getFirst().size() - 1; i >= 0; i--) {
            for (int j = 1; j < pathsToRoot.size(); j++) {
                if (i + differences[j] < 0 ||
                    !pathsToRoot.get(j).get(i + differences[j]).equals(pathsToRoot.getFirst().get(i))) {
                    return mrca;
                }
            }
            mrca = pathsToRoot.getFirst().get(i);
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
        Node lca = findLCA(getNode(taxId1), getNode(taxId2));
        return lca == null ? -1 : lca.getTaxId();
    }

    public int findLCA(int[] taxIds) {
        Node[] nodes = new Node[taxIds.length];
        for (int i = 0; i < taxIds.length; i++) {
            nodes[i] = getNode(taxIds[i]);
        }
        Node lca = findLCAByNodes(nodes);
        return lca == null ? -1 : lca.getTaxId();
    }

    /**
     * @param label of the property
     * @return true if the property exists
     */
    public boolean hasLongProperty(String label) {
        return longPropertyDescriptions.containsKey(label);
    }

    /**
     * @param label of the property
     * @return true if the property exists
     */
    public boolean hasDoubleProperty(String label) {
        return doublePropertyDescriptions.containsKey(label);
    }

    /**
     * Add a new long property to each node.
     * <p>
     *     If there already is a long property with the same label, it will be overwritten.
     * </p>
     * @param label   the label of the property
     * @param initial the initial value of the property
     */
    public void addLongProperty(String label, long initial) {
        if (longPropertyDescriptions.containsKey(label)) {
            int index = longPropertyDescriptions.get(label);
            for (Node node: idMap.values()) {
                node.longProperties.set(index, initial);
            }
        } else {
            longPropertyDescriptions.put(label, longPropertyDescriptions.size());
            defaultLongProperties.add(initial);
            for (Node node: idMap.values()) {
                node.longProperties.add(initial);
            }
        }
    }

    /**
     * Add a new double property to each node.
     * <p>
     *     If there already is a double property with the same label, it will be overwritten.
     * </p>
     * @param label   the label of the property
     * @param initial the initial value of the property
     */
    public void addDoubleProperty(String label, double initial) {
        if (doublePropertyDescriptions.containsKey(label)) {
            int index = doublePropertyDescriptions.get(label);
            for (Node node: idMap.values()) {
                node.doubleProperties.set(index, initial);
            }
        } else {
            doublePropertyDescriptions.put(label, doublePropertyDescriptions.size());
            defaultDoubleProperties.add(initial);
            for (Node node: idMap.values()) {
                node.doubleProperties.add(initial);
            }
        }
    }

    /**
     * @param label the label of the property
     * @return the node corresponding to the taxonomic ID
     * @throws RuntimeException if the property does not exist
     */
    private int getLongPropertyIndex(String label) {
        if (!longPropertyDescriptions.containsKey(label)) {
            throw new RuntimeException("Tried to access non-existing long property: " + label);
        }
        return longPropertyDescriptions.get(label);
    }

    /**
     * @param label the label of the property
     * @return the node corresponding to the taxonomic ID
     * @throws RuntimeException if the property does not exist
     */
    private int getDoublePropertyIndex(String label) {
        if (!doublePropertyDescriptions.containsKey(label)) {
            throw new RuntimeException("Tried to access non-existing double property: " + label);
        }
        return doublePropertyDescriptions.get(label);
    }

    /**
     * Set the value of an <strong>existing</strong> long property of a specific node.
     * @param taxId the taxonomic ID of the node
     * @param label the label of the property
     * @param value the value of the property
     */
    public void setProperty(int taxId, String label, long value) {
        Node node = getNodeOrError(taxId);
        int index = getLongPropertyIndex(label);
        synchronized (node) {
            node.longProperties.set(index, value);
        }
    }

    /**
     * Set the value of an <strong>existing</strong> double property of a specific node.
     * @param taxId the taxonomic ID of the node
     * @param label the label of the property
     * @param value the value of the property
     */
    public void setProperty(int taxId, String label, double value) {
        Node node = getNodeOrError(taxId);
        int index = getDoublePropertyIndex(label);
        synchronized (node) {
            node.doubleProperties.set(index, value);
        }
    }

    /**
     * Add a value to an <strong>existing</strong> long property of a specific node.
     * @param taxId the taxonomic ID of the node
     * @param label the label of the property
     * @param value the value to add
     */
    public void addToProperty(int taxId, String label, long value) {
        Node node = getNodeOrError(taxId);
        int index = getLongPropertyIndex(label);
        synchronized (node) {
            node.longProperties.set(index, node.longProperties.get(index) + value);
        }
    }

    /**
     * Add a value to an <strong>existing</strong> property of type double for a node in the tree.
     * @param taxId the taxonomic ID of the node
     * @param label the label of the property
     * @param value the value to add
     */
    public void addToProperty(int taxId, String label, double value) {
        Node node = getNodeOrError(taxId);
        int index = getDoublePropertyIndex(label);
        synchronized (node) {
            node.doubleProperties.set(index, node.doubleProperties.get(index) + value);
        }
    }

    /**
     * @return a list of all labels of properties of type long
     */
    public List<String> getLongPropertyLabels() {
        return longPropertyDescriptions.entrySet().stream()
        .sorted(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
    }

    /**
     * @return a list of all labels of properties of type double
     */
    public List<String> getDoublePropertyLabels() {
        return doublePropertyDescriptions.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Get a property of type long for a node in the tree.
     * @param taxId the taxonomic ID of the node
     * @param label the label of the property
     * @return the value of the property
     */
    public long getLongProperty(int taxId, String label) {
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
    public double getDoubleProperty(int taxId, String label) {
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
     * Accumulates a long property over all nodes in the tree.
     * @param label the label of the property
     * @param targetLabel the label of the property to accumulate the values to
     */
    public void accumulateLongProperty(String label, String targetLabel) {
        if (!longPropertyDescriptions.containsKey(label)) {
            throw new RuntimeException("Tried to access non-existing property: " + label);
        }
        addLongProperty(targetLabel, 0);
        accumulateLongProperty(label, targetLabel, getRoorOrError());
    }

    /**
     * Recursive helper function to accumulate a long property over all nodes in the tree.
     */
    private void accumulateLongProperty(String label, String targetLabel, Node root) {
        setProperty(root.getTaxId(), targetLabel, getLongProperty(root.getTaxId(), label));
        if (!root.isLeaf()) {
            for (Node child : root.getChildren()) {
                accumulateLongProperty(label, targetLabel, child);
                addToProperty(root.getTaxId(), targetLabel, getLongProperty(child.getTaxId(), targetLabel));
            }
        }
    }

    /**
     * Accumulates a double property over all nodes in the tree.
     * @param label the label of the property
     * @param targetLabel the label of the property to accumulate the values to
     */
    public void accumulateDoubleProperty(String label, String targetLabel) {
        if (!doublePropertyDescriptions.containsKey(label)) {
            throw new RuntimeException("Tried to access non-existing property: " + label);
        }
        addDoubleProperty(targetLabel, 0);
        accumulateDoubleProperty(label, targetLabel, getRoorOrError());
    }

    /**
     * Recursive helper function to accumulate a double property over all nodes in the tree.
     */
    private void accumulateDoubleProperty(String label, String targetLabel, Node root) {
        setProperty(root.getTaxId(), targetLabel, getDoubleProperty(root.getTaxId(), label));
        if (!root.isLeaf()) {
            for (Node child : root.getChildren()) {
                accumulateDoubleProperty(label, targetLabel, child);
                addToProperty(root.getTaxId(), targetLabel, getDoubleProperty(child.getTaxId(), targetLabel));
            }
        }
    }

    /**
     * Resets the numeric properties of all nodes
     */
    public void resetProperties() {
        longPropertyDescriptions.clear();
        doublePropertyDescriptions.clear();
        defaultLongProperties.clear();
        defaultDoubleProperties.clear();
        for (Node node : idMap.values()) {
            node.longProperties.clear();
            node.doubleProperties.clear();
        }
    }

    /**
     * Creates a new subtree with the nodes and weights given in the input array.
     * <p>The subtree will contain the root of the original tree and all nodes that lie on the paths from each input
     * node to the root. The accumulated weights will not be set.</p>
     * @param nodesAndWeights an array of arrays of node IDs and weights [[nodeId1, weight1], [nodeId2, weight2], ...]
     * @param targetLabel the label of the property to store the weights in
     * @return a new tree with the nodes and weights given in the array
     */
    public Tree getWeightedSubTreeInt(List<ReadAssignment.KmerCount<Integer>> nodesAndWeights, String targetLabel) {

        Tree subTree = new Tree();
        subTree.addLongProperty(targetLabel, 0L);

        for (ReadAssignment.KmerCount<Integer> kmerCount : nodesAndWeights) {

            // Skip nodes that are not in the tree
            if (!idMap.containsKey(kmerCount.getTaxId())) {
                continue;
            }
            // Get the taxonomic ID of the node the id is mapped to.
            // This can happen if the tree was reduced to the standard ranks before.
            int nodeId = idMap.get(kmerCount.getTaxId()).getTaxId();

            // In case the node is already in the new tree, only the weight is updated
            if (subTree.hasNode(nodeId)) {
                subTree.addToProperty(nodeId, targetLabel, kmerCount.getCount());
                continue;
            }

            // copy node to new tree
            Node node = idMap.get(nodeId);
            Node nodeCopy = node.copy();
            int originalId = nodeId;
            // Add all nodes on the path to the root to the new tree (if they are not already in the new tree)
            while (node.hasParent()) {
                subTree.addNode(nodeId, nodeCopy);
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
            subTree.addNode(nodeId, nodeCopy);
            subTree.addToProperty(originalId, targetLabel, kmerCount.getCount());
        }
        subTree.autoFindRoot();
        return subTree;
    }

    /**
     * Creates a new subtree with the nodes and weights given in the list.
     * <p>The subtree will contain the root of the original tree and all nodes that lie on the paths from each input
     * node to the root. The accumulated weights will not be set.</p>
     * @param nodesAndWeights a list of pairs of node IDs and weights [[nodeId1, weight1], [nodeId2, weight2], ...]
     * @param targetLabel the label of the property to store the weights in
     * @return a new tree with the nodes and weights given in the array as node double properties
     */
    public Tree getWeightedSubTreeDouble(List<ReadAssignment.KmerCount<Double>> nodesAndWeights, String targetLabel) {

        Tree subTree = new Tree();
        subTree.addDoubleProperty(targetLabel, 0L);

        for (ReadAssignment.KmerCount<Double> kmerCount : nodesAndWeights) {
            // Skip nodes that are not in the tree
            if (!idMap.containsKey(kmerCount.getTaxId())) {
                continue;
            }
            // Get the taxonomic ID of the node the id is mapped to.
            // This can happen if the tree was reduced to the standard ranks before.
            int nodeId = idMap.get(kmerCount.getTaxId()).getTaxId();

            // In case the node is already in the new tree, only the weight is updated
            if (subTree.hasNode(nodeId)) {
                subTree.addToProperty(nodeId, targetLabel, kmerCount.getCount());
                continue;
            }

            // copy node to new tree
            Node node = idMap.get(nodeId);
            Node nodeCopy = node.copy();
            int originalId = nodeId;
            // Add all nodes on the path to the root to the new tree (if they are not already in the new tree)
            while (node.hasParent()) {
                subTree.addNode(nodeId, nodeCopy);
                Node parent = node.getParent();
                if (subTree.hasNode(parent.getTaxId())) {
                    subTree.getNode(parent.getTaxId()).addChild(nodeCopy);
                    nodeCopy.setParent(subTree.getNode(parent.getTaxId()));
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
            subTree.addNode(nodeId, nodeCopy);
            subTree.addToProperty(originalId, targetLabel, kmerCount.getCount());
        }
        subTree.autoFindRoot();
        return subTree;
    }

    /**
     * Searches for the node with the given rank in the path from the given node to the root.
     * @param node the node to start from
     * @param rank the rank to search for
     * @return the node with the given rank or null if the rank was not found
     */
    @Contract("null, _ -> null")
    public Node getRank(Node node, @NotNull String rank) {
        Node rankNode = null;
        Node previousNode = null;
        while (node != previousNode && node != null) {
            if (rank.equals(node.getRank())) {
                rankNode = node;
                break;
            }
            previousNode = node;
            node = node.getParent();
        }
        return rankNode;
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
                // add properties to new node
                for (int i = 0; i < node.longProperties.size(); i++) {
                    parent.longProperties.set(i, parent.longProperties.get(i) + node.longProperties.get(i));
                }
                for (int i = 0; i < node.doubleProperties.size(); i++) {
                    parent.doubleProperties.set(i, parent.doubleProperties.get(i) + node.doubleProperties.get(i));
                }
                // add children to the standard-rank parent
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
}
