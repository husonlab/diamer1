package org.husonlab.diamer2.taxonomy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Represents a taxonomic node in a {@link Tree}.
 */
public class Node {
    private final int taxId;
    @Nullable
    private String scientificName;
    // alternative names
    private final ArrayList<String> labels;
    @Nullable
    private String rank;
    private int weight;
    private int accumulatedWeight;

    @Nullable
    private Node parent;
    private final ArrayList<Node> children;

    /**
     * Construct a new {@link Node} with a taxonomic ID.
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
     * Construct a new {@link Node} with a taxonomic ID and a parent.
     * @param taxId the taxonomic ID of the node
     * @param parent the parent of the node
     */
    public Node(int taxId, Node parent) {
        this(taxId);
        this.parent = parent;
    }

    /**
     * Construct a new {@link Node} with a taxonomic ID and a rank.
     * @param taxId the taxonomic ID of the node
     * @param rank the rank of the node
     */
    public Node(int taxId, String rank) {
        this(taxId);
        this.rank = rank;
    }

    /**
     * Construct a new {@link Node} with a taxonomic ID, a parent and a rank.
     * @param taxId the taxonomic ID of the node
     * @param parent the parent of the node
     * @param rank the rank of the node
     */
    public Node(int taxId, Node parent, String rank) {
        this(taxId, parent);
        this.rank = rank;
    }

    /**
     * Construct a new {@link Node} with a taxonomic ID, a rank, a scientific name and a list of alternative names.
     * @param taxId the taxonomic ID of the node
     * @param rank the rank of the node
     * @param scientificName the scientific name of the node
     */
    public Node(int taxId, String rank, String scientificName, ArrayList<String> labels) {
        this(taxId, rank);
        this.scientificName = scientificName;
        this.labels.addAll(labels);
    }

    /**
     * Add a child to the {@link Node}.
     * @param child the child {@link Node} to add
     */
    public void addChild(Node child) {
        if (!children.contains(child)) {
            children.add(child);
        }
    }

    /**
     * Add an alternative name to the List of alternative names.
     * @param name the name to add
     */
    public void addLabel(String name) {
        labels.add(name);
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

    public void addAccumulatedWeight(int weight) {
        synchronized (this) {
            this.accumulatedWeight += weight;
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