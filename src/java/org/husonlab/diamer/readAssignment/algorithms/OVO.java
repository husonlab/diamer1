package org.husonlab.diamer.readAssignment.algorithms;

import org.husonlab.diamer.taxonomy.Node;
import org.husonlab.diamer.taxonomy.Tree;

public class OVO extends ClassificationAlgorithmOnWeightedSubtree {

    /**
     * @param ratio Between 0 and 1, the ratio of the highest weight to the second-highest weight that is required to
     *              proceed to the child node with the highest weight. The lower the number, the closer to the root will
     *              be the assigned taxon. A value of 0 will always return the root node.
     */
    public OVO(float ratio) {
        super(ratio);
    }

    @Override
    public String getName() {
        return "OVO (%.2f)".formatted(ratio);
    }

    /**
     * Recursive function to assign a read to a taxon using the OVO algorithm
     * @param root node to start from
     * @param ratio ratio between highest and second-highest weight
     * @return assigned taxon
     */
    @Override
    protected int recursiveLong(Tree subTree, Node root, float ratio){
        if (root.isLeaf()){
            return root.getTaxId();

        // Directly jump to child if it is the only one
        } else if (root.getChildren().size() == 1) {
            return recursiveLong(subTree, root.getChildren().getFirst(), ratio);
        }

        // find child with highest and second-highest weight
        long highestWeight = 0L;
        Node highestNode = root.getChildren().getFirst();
        long secondHighestWeight = 0L;
        for (Node child : root.getChildren()) {
            long weightChild = subTree.getLongProperty(child.getTaxId(), "weight (accumulated)");
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
            return recursiveLong(subTree, highestNode, ratio);
        } else {
            return root.getTaxId();
        }
    }

    protected int recursiveDouble(Tree subTree, Node root, float ratio) {
        if (root.isLeaf()){
            return root.getTaxId();

        // Directly jump to child if it is the only one
        } else if (root.getChildren().size() == 1) {
            return recursiveDouble(subTree, root.getChildren().getFirst(), ratio);
        }

        // find child with highest and second-highest weight
        double highestWeight = 0.0;
        Node highestNode = root.getChildren().getFirst();
        double secondHighestWeight = 0.0;
        for (Node child : root.getChildren()) {
            double weightChild = subTree.getDoubleProperty(child.getTaxId(), "weight (accumulated)");
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
            return recursiveDouble(subTree, highestNode, ratio);
        } else {
            return root.getTaxId();
        }
    }

    @Override
    public String toString() {
        return getName();
    }
}
