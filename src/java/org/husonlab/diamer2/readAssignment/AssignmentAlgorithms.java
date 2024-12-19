package org.husonlab.diamer2.readAssignment;

import org.husonlab.diamer2.taxonomy.Node;
import org.husonlab.diamer2.taxonomy.Tree;

import java.util.LinkedList;

public class AssignmentAlgorithms {
    private final Tree tree;

    public AssignmentAlgorithms(Tree tree) {
        this.tree = tree;
    }

    public Node OVO(LinkedList<int[]> nodesAndWeights, float ratio){
        Tree subTree = tree.getWeightedSubTree(nodesAndWeights);
        subTree.accumulateWeights(tree.getRoot());
        return OVORecursive(subTree.getRoot(), ratio);
    }

    private Node OVORecursive(Node node, float ratio){
        if (node.isLeaf()){
            return node;
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
            return node;
        }
    }

}
