package org.husonlab.diamer2.taxonomy;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Represents a node in a taxonomic tree.
 */
public class Node {
    private final int taxId;
    @Nullable
    private Node parent;
    private final ArrayList<Node> children;
    private final ArrayList<String> labels;
    private String scientificName;
    private String rank;
    private int weight;
    private int accumulatedWeight;

    /**
     * Node with no parent.
     * @param taxId the taxonomic ID of the node
     */
    public Node(int taxId) {
        this.taxId = taxId;
        this.children = new ArrayList<Node>();
        this.labels = new ArrayList<String>();
        this.weight = 0;
        this.accumulatedWeight = 0;
    }

    /**
     * Node with a parent.
     * @param taxId the taxonomic ID of the node
     * @param parent the parent of the node
     */
    public Node(int taxId, Node parent) {
        this(taxId);
        this.parent = parent;
    }

    /**
     * Node with no parent and a taxonomic rank.
     * @param taxId the taxonomic ID of the node
     * @param rank the rank of the node
     */
    public Node(int taxId, String rank) {
        this(taxId);
        this.rank = rank;
    }

    /**
     * Node with a parent and a taxonomic rank.
     * @param taxId the taxonomic ID of the node
     * @param parent the parent of the node
     * @param rank the rank of the node
     */
    public Node(int taxId, Node parent, String rank) {
        this(taxId, parent);
        this.rank = rank;
    }

    public Node(int taxId, String rank, String scientificName, ArrayList<String> labels) {
        this(taxId, rank);
        this.scientificName = scientificName;
        this.labels.addAll(labels);
    }

    /**
     * Add a child to the node.
     * @param child the child to add
     */
    public void addChild(Node child) {
        if (!children.contains(child)) {
            children.add(child);
        }
    }

    /**
     * Add a label to the node.
     * @param label the label to add
     */
    public void addLabel(String label) {
        labels.add(label);
    }

    /**
     * Set the parent of the node.
     * @param parent the parent to set
     */
    public void setParent(Node parent) {
        this.parent = parent;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public void addWeight(int weight) {
        synchronized (this) {
            this.weight += weight;
        }
    }

    public void setAccumulatedWeight(int accumulatedWeight) {
        this.accumulatedWeight = accumulatedWeight;
    }

    /**
     * Set the rank of the node.
     * @param rank the rank to set
     */
    public void setRank(String rank) {
        this.rank = rank;
    }

    /**
     * Set the scientific name of the node.
     * @param scientificName the scientific name of the node
     */
    public void setScientificName(String scientificName) {
        this.scientificName = scientificName;
    }

    /**
     * Get the children of the node.
     * @return the children of the node
     */
    public ArrayList<Node> getChildren() {
        return children;
    }

    /**
     * Get the parent of the node.
     * @return the parent of the node
     */
    public Node getParent() {
        return parent;
    }

    /**
     * Get the taxonomic ID of the node.
     * @return the taxonomic ID of the node
     */
    public int getTaxId() {
        return taxId;
    }

    /**
     * Get the labels of the node.
     * @return the labels of the node
     */
    public ArrayList<String> getLabels() {
        return labels;
    }

    /**
     * Get the rank of the node.
     * @return the rank of the node
     */
    public String getRank() {
        return rank;
    }

    /**
     * Get the scientific name of the node.
     * @return the scientific name of the node
     */
    public String getScientificName() {
        return scientificName;
    }

    /**
     * Get the scientific name or the first label of the node.
     * @return the label of the node
     */
    public String getLabel() {
        return scientificName != null ? scientificName :
                !labels.isEmpty() ? labels.getFirst() : "no label";
    }

    public int getWeight() {
        return weight;
    }

    public int getAccumulatedWeight() {
        return accumulatedWeight;
    }

    /**
     * Check if the node has a parent.
     * @return false if the parent is null or the parent is the node itself.
     */
    public boolean hasParent() {
        return !(parent == null || parent.equals(this));
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    /**
     * Creates a copy of the node without the parent and children.
     * @return a copy of the node
     */
    public Node copy() {
        return new Node(taxId, rank, scientificName, labels);
    }

    @Override
    public String toString() {
        return "(%s) %s (%d)"
                .formatted(
                        !Objects.isNull(this.rank) ? this.rank : "no rank",
                        !Objects.isNull(this.scientificName) ? this.scientificName :
                                !this.labels.isEmpty() ? this.labels.getFirst() : "no labels",
                        this.taxId
                );
    }
}