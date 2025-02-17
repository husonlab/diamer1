package org.husonlab.diamer2.taxonomy;

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
    /**
     * ArrayList to store properties of type long. A description of the values can be stored in the containing tree.
     */
    protected final ArrayList<Long> longProperties;
    /**
     * ArrayList to store properties of type double. A description of the values can be stored in the containing tree.
     */
    protected final ArrayList<Double> doubleProperties;

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
        this.longProperties = new ArrayList<Long>();
        this.doubleProperties = new ArrayList<Double>();
    }

    /**
     * Construct a new {@link Node} with a taxonomic ID and a parent.
     * @param taxId the taxonomic ID of the node
     * @param parent the parent of the node
     */
    public Node(int taxId, @Nullable Node parent) {
        this(taxId);
        this.parent = parent;
        if (parent != null) {
            parent.addChild(this);
        }
    }

    /**
     * Construct a new {@link Node} with a taxonomic ID and a rank.
     * @param taxId the taxonomic ID of the node
     * @param rank the rank of the node
     */
    public Node(int taxId, @Nullable String rank) {
        this(taxId);
        this.rank = rank;
    }

    /**
     * Construct a new {@link Node} with a taxonomic ID, a parent, a rank and a scientific name.
     * @param taxId the taxonomic ID of the node
     * @param parent the parent of the node
     * @param rank the rank of the node
     * @param scientificName the scientific name of the node
     */
    public Node(int taxId, Node parent, @Nullable String rank, @Nullable String scientificName) {
        this(taxId, parent);
        this.rank = rank;
        this.scientificName = scientificName;
    }

    /**
     * Construct a new {@link Node} with a taxonomic ID, a rank, a scientific name and a list of alternative names.
     * @param taxId the taxonomic ID of the node
     * @param rank the rank of the node
     * @param scientificName the scientific name of the node
     */
    public Node(int taxId, String rank, @Nullable String scientificName, ArrayList<String> labels) {
        this(taxId, rank);
        this.scientificName = scientificName;
        this.labels.addAll(labels);
    }

    /**
     * Get the taxonomic ID of the node.
     * @return the taxonomic ID of the node
     */
    public int getTaxId() {
        return taxId;
    }

    /**
     * Set the scientific name of the node.
     * <p>
     *     This name is meant to reflect NCBIs scientific name and should be unique.
     * </p>
     * @param scientificName the scientific name of the node
     */
    public void setScientificName(@Nullable String scientificName) {
        this.scientificName = scientificName;
    }

    /**
     * Get the scientific name of the node.
     * <p>This name is meant to reflect NCBIs scientific name and should be unique.</p>
     * @return the scientific name of the node
     */
    @Nullable
    public String getScientificName() {
        return scientificName;
    }

    public String getScientificNameOrFirstLabel() {
        return scientificName != null ? scientificName : labels.isEmpty() ? "" : labels.getFirst();
    }

    /**
     * Add an alternative name to the List of alternative names.
     * <p>Does not have to be unique.</p>
     * @param name the name to add
     */
    public void addLabel(String name) {
        if (!labels.contains(name)) {
            labels.add(name);
        }
    }

    /**
     * Get the labels (alternative names) of the node.
     * <p>The labels don't have to be unique.</p>
     * @return the labels of the node
     */
    public ArrayList<String> getLabels() {
        return labels;
    }

    /**
     * Set the rank of the node.
     * @param rank the rank to set
     */
    public void setRank(@Nullable String rank) {
        this.rank = rank;
    }

    /**
     * Get the rank of the node.
     * @return the rank of the node
     */
    @Nullable
    public String getRank() {
        return rank;
    }

    /**
     * @return the properties of type long
     */
    public ArrayList<Long> getLongProperties() {
        return longProperties;
    }

    /**
     * @return the properties of type double
     */
    public ArrayList<Double> getDoubleProperties() {
        return doubleProperties;
    }


    /**
     * Set the parent of the node.
     * @param parent the parent to set
     */
    public void setParent(@Nullable Node parent) {
        this.parent = parent;
    }

    /**
     * Check if the node has a parent.
     * @return false if the parent is null or the parent is the node itself.
     */
    public boolean hasParent() {
        return !(parent == null || parent.equals(this));
    }

    /**
     * Get the parent of the node.
     * @return the parent of the node
     */
    @Nullable
    public Node getParent() {
        return parent;
    }

    /**
     * Add a child to the node.
     * @param child the child node to add
     */
    public void addChild(Node child) {
        if (!children.contains(child)) {
            children.add(child);
        }
    }

    /**
     * Checks if the node has children.
     * @return true if the node has children
     */
    public boolean isLeaf() {
        return children.isEmpty();
    }

    /**
     * Get the children of the node.
     * @return the children of the node
     */
    public ArrayList<Node> getChildren() {
        return children;
    }

    /**
     * Creates a copy of the node.
     * <p>Properties included in the copy:
     * <ul>
     *     <li>taxonomic ID</li>
     *     <li>rank</li>
     *     <li>scientific name</li>
     *     <li>labels</li>
     * </ul></p>
     * <p>Properties not included:
     * <ul>
     *     <li>parent</li>
     *     <li>children</li>
     * </ul></p>
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

    /**
     * Check if two nodes are equal.
     * <p>Does NOT consider parent and children!</p>
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return taxId == node.taxId && Objects.equals(scientificName, node.scientificName) && Objects.equals(labels, node.labels) && Objects.equals(rank, node.rank) && Objects.equals(longProperties, node.longProperties) && Objects.equals(doubleProperties, node.doubleProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taxId, scientificName, labels, rank, longProperties, doubleProperties);
    }
}