package org.husonlab.diamer2.readAssignment.algorithms;

import org.husonlab.diamer2.readAssignment.ReadAssignment;
import org.husonlab.diamer2.taxonomy.Node;
import org.husonlab.diamer2.taxonomy.Tree;
import org.husonlab.diamer2.util.Pair;

import java.util.List;

public class OVO extends AssignmentAlgorithm {

    private final float ratio;

    /**
     * @param tree The taxonomic tree to use as reference
     * @param ratio Between 0 and 1, the ratio of the highest weight to the second-highest weight that is required to
     *              proceed to the child node with the highest weight. The lower the number, the closer to the root will
     *              be the assigned taxon. A value of 0 will always return the root node.
     */
    public OVO(Tree tree, float ratio) {
        super(tree);
        this.ratio = ratio;
    }

    @Override
    public int assignRawReadKmerMatches(List<ReadAssignment.KmerMatch<Integer>> kmerMatches) {
        if (kmerMatches.isEmpty()){
            return -1;
        }
        Tree subTree = tree.getWeightedSubTreeInt(kmerMatches, "weight");
        subTree.accumulateNodeLongProperty("weight", "weight (accumulated)");
        return OVORecursiveLong(subTree, subTree.getRoot(), ratio);
    }

    @Override
    public int assignNormalizedReadKmerMatches(List<ReadAssignment.KmerMatch<Double>> normalizedKmerMatches) {
        if (normalizedKmerMatches.isEmpty()){
            return -1;
        }
        Tree subTree = tree.getWeightedSubTreeDouble(normalizedKmerMatches, "weight");
        subTree.accumulateNodeDoubleProperty("weight", "weight (accumulated)");
        return OVORecursiveDouble(subTree, subTree.getRoot(), ratio);
    }

    @Override
    public String getName() {
        return "OVO (ratio: %f)".formatted(ratio);
    }

    /**
     * Recursive function to assign a read to a taxon using the OVO algorithm
     * @param node node to start from
     * @param ratio ratio between highest and second-highest weight
     * @return assigned taxon
     */
    private int OVORecursiveLong(Tree subTree, Node node, float ratio){
        if (node.isLeaf()){
            return node.getTaxId();

        // Directly jump to child if it is the only one
        } else if (node.getChildren().size() == 1) {
            return OVORecursiveLong(subTree, node.getChildren().getFirst(), ratio);
        }

        // find child with highest and second-highest weight
        long highestWeight = 0L;
        Node highestNode = node.getChildren().getFirst();
        long secondHighestWeight = 0L;
        for (Node child : node.getChildren()) {
            long weightChild = subTree.getNodeLongProperty(child.getTaxId(), "weight (accumulated)");
            if (weightChild > highestWeight) {
                secondHighestWeight = highestWeight;
                highestWeight = weightChild;
                highestNode = child;
            } else if (weightChild > secondHighestWeight) {
                secondHighestWeight = weightChild;
            }
        }

        // if the highest weight is not much higher (dependent on the ratio), the current node is returned
        if (highestWeight * ratio > secondHighestWeight) {
            return OVORecursiveLong(subTree, highestNode, ratio);
        } else {
            return node.getTaxId();
        }
    }

    private int OVORecursiveDouble(Tree subTree, Node node, float ratio) {
        if (node.isLeaf()){
            return node.getTaxId();

        // Directly jump to child if it is the only one
        } else if (node.getChildren().size() == 1) {
            return OVORecursiveDouble(subTree, node.getChildren().getFirst(), ratio);
        }

        // find child with highest and second-highest weight
        double highestWeight = 0.0;
        Node highestNode = node.getChildren().getFirst();
        double secondHighestWeight = 0.0;
        for (Node child : node.getChildren()) {
            double weightChild = subTree.getNodeDoubleProperty(child.getTaxId(), "weight (accumulated)");
            if (weightChild > highestWeight) {
                secondHighestWeight = highestWeight;
                highestWeight = weightChild;
                highestNode = child;
            } else if (weightChild > secondHighestWeight) {
                secondHighestWeight = weightChild;
            }
        }

        // if the highest weight is not much higher (dependent on the ratio), the current node is returned
        if (highestWeight * ratio > secondHighestWeight) {
            return OVORecursiveDouble(subTree, highestNode, ratio);
        } else {
            return node.getTaxId();
        }
    }

}
