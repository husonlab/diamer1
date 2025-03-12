package org.husonlab.diamer2.readAssignment.algorithms;

import org.husonlab.diamer2.taxonomy.Node;
import org.husonlab.diamer2.taxonomy.Tree;

public class OVA extends AssignmentAlgorithmOnWeightedSubtree {

    /**
     * <strong>One Versus All</strong> algorithm for assigning reads to taxonomic nodes.
     * @param ratio Between 0 and 1, the factor to multiply the weight sum of all children with before comparing it to
     *              the weight of the node with the highest weight.
     */
    public OVA(float ratio) {
        super(ratio);
    }

    @Override
    protected int recursiveLong(Tree subTree, Node root, float ratio) {
        if (root.isLeaf()){
            return root.getTaxId();

            // Directly jump to child if it is the only one
        } else if (root.getChildren().size() == 1) {
            return recursiveLong(subTree, root.getChildren().getFirst(), ratio);
        }

        // find child with the highest weight and sum up all weights
        long highestWeight = 0L;
        long sumWeight = 0L;
        Node highestNode = root.getChildren().getFirst();
        for (Node child : root.getChildren()) {
            long weightChild = subTree.getLongProperty(child.getTaxId(), "weight (accumulated)");
            sumWeight += weightChild;
            if (weightChild > highestWeight) {
                highestWeight = weightChild;
                highestNode = child;
            }
        }

        // if the highest weight is not much higher (dependent on the ratio), the current node is returned
        if (highestWeight * ratio > sumWeight) {
            return recursiveLong(subTree, highestNode, ratio);
        } else {
            return root.getTaxId();
        }
    }

    @Override
    protected int recursiveDouble(Tree subTree, Node root, float ratio) {
        if (root.isLeaf()){
            return root.getTaxId();

            // Directly jump to child if it is the only one
        } else if (root.getChildren().size() == 1) {
            return recursiveDouble(subTree, root.getChildren().getFirst(), ratio);
        }

        // find child with the highest weight and sum up all weights
        double highestWeight = 0L;
        double sumWeight = 0L;
        Node highestNode = root.getChildren().getFirst();
        for (Node child : root.getChildren()) {
            double weightChild = subTree.getDoubleProperty(child.getTaxId(), "weight (accumulated)");
            sumWeight += weightChild;
            if (weightChild > highestWeight) {
                highestWeight = weightChild;
                highestNode = child;
            }
        }

        // if the highest weight is not much higher (dependent on the ratio), the current node is returned
        if (highestWeight * ratio > sumWeight) {
            return recursiveDouble(subTree, highestNode, ratio);
        } else {
            return root.getTaxId();
        }
    }

    @Override
    public String getName() {
        return "OVA (%.2f)".formatted(ratio);
    }
}
