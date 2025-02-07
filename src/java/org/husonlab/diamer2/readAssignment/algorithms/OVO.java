package org.husonlab.diamer2.readAssignment.algorithms;

import org.husonlab.diamer2.taxonomy.Node;
import org.husonlab.diamer2.taxonomy.Tree;

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
    public int assignRead(List<int[]> kmerMatches) {
        if (kmerMatches.isEmpty()){
            return -1;
        }
        Tree subTree = tree.getWeightedSubTree(kmerMatches);
        subTree.accumulateWeights();
        return OVORecursive(subTree.getRoot(), ratio);
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
    private int OVORecursive(Node node, float ratio){
        if (node.isLeaf()){
            return node.getTaxId();

        // Directly jump to child if it is the only one
        } else if (node.getChildren().size() == 1) {
            return OVORecursive(node.getChildren().getFirst(), ratio);
        }

        // find child with highest and second-highest weight
        long highestWeight = 0L;
        Node highestNode = node.getChildren().getFirst();
        long secondHighestWeight = 0L;
        for (Node child : node.getChildren()) {
            if (child.getWeight() > highestWeight) {
                secondHighestWeight = highestWeight;
                highestWeight = child.getWeight();
                highestNode = child;
            } else if (child.getWeight() > secondHighestWeight) {
                secondHighestWeight = child.getWeight();
            }
        }

        // if the highest weight is not much higher (dependent on the ratio), the current node is returned
        if (highestWeight * ratio > secondHighestWeight) {
            return OVORecursive(highestNode, ratio);
        } else {
            return node.getTaxId();
        }
    }

}
