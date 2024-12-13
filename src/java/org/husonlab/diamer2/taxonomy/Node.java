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

    /**
     * Node with no parent.
     * @param taxId the taxonomic ID of the node
     */
    public Node(int taxId) {
        this.taxId = taxId;
        this.children = new ArrayList<Node>();
        this.labels = new ArrayList<String>();
    }

    /**
     * Node with a parent.
     * @param taxId the taxonomic ID of the node
     * @param parent the parent of the node
     */
    public Node(int taxId, Node parent) {
        this.taxId = taxId;
        this.parent = parent;
        this.children = new ArrayList<Node>();
        this.labels = new ArrayList<String>();
    }

    /**
     * Node with no parent and a taxonomic rank.
     * @param taxId the taxonomic ID of the node
     * @param rank the rank of the node
     */
    public Node(int taxId, String rank) {
        this.taxId = taxId;
        this.rank = rank;
        this.children = new ArrayList<Node>();
        this.labels = new ArrayList<String>();
    }

    /**
     * Node with a parent and a taxonomic rank.
     * @param taxId the taxonomic ID of the node
     * @param parent the parent of the node
     * @param rank the rank of the node
     */
    public Node(int taxId, Node parent, String rank) {
        this.taxId = taxId;
        this.parent = parent;
        this.rank = rank;
        this.children = new ArrayList<Node>();
        this.labels = new ArrayList<String>();
    }

    /**
     * Add a child to the node.
     * @param child the child to add
     */
    public void addChild(Node child) {
        children.add(child);
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