package org.husonlab.diamer2.readAssignment;

import org.husonlab.diamer2.taxonomy.Node;
import org.husonlab.diamer2.taxonomy.Tree;

import java.util.ArrayList;

public class OVO extends AssignmentAlgorithm {

    private final float ratio;

    public OVO(Tree tree, float ratio) {
        super(tree);
        this.ratio = ratio;
    }

    @Override
    public int assignRead(ArrayList<int[]> kmerMatches) {
        if (kmerMatches.isEmpty()){
            return -1;
        }
        Tree subTree = tree.getWeightedSubTree(kmerMatches);
        subTree.accumulateWeights(subTree.getRoot());
        return OVORecursive(subTree.getRoot(), ratio);
    }

    @Override
    public String getName() {
        return "OVO (ratio: %f)".formatted(ratio);
    }

    private int OVORecursive(Node node, float ratio){
        if (node.isLeaf()){
            return node.getTaxId();
        } else if (node.getChildren().size() == 1) {
            return OVORecursive(node.getChildren().getFirst(), ratio);
        }
        int highestWeight = 0;
        Node highestNode = node.getChildren().getFirst();
        int secondHighestWeight = 0;
        for (Node child : node.getChildren()) {
            if (child.getCumulativeWeight() > highestWeight) {
                secondHighestWeight = highestWeight;
                highestWeight = child.getCumulativeWeight();
                highestNode = child;
            } else if (child.getCumulativeWeight() > secondHighestWeight) {
                secondHighestWeight = child.getCumulativeWeight();
            }
        }
        if (highestWeight * ratio > secondHighestWeight) {
            return OVORecursive(highestNode, ratio);
        } else {
            return node.getTaxId();
        }
    }

}
