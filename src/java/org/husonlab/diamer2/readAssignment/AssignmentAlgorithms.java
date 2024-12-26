package org.husonlab.diamer2.readAssignment;

import org.husonlab.diamer2.taxonomy.Node;
import org.husonlab.diamer2.taxonomy.Tree;

import java.util.ArrayList;
import java.util.LinkedList;

public class AssignmentAlgorithms {
    private final Tree tree;

    public AssignmentAlgorithms(Tree tree) {
        this.tree = tree;
    }

    public void OVO(Read read, float ratio){
        Node node = OVO(read.getAssociations(), ratio);
        read.setAssignedNode(node);
    }

    public Node OVO(ArrayList<int[]> nodesAndWeights, float ratio){
        if (nodesAndWeights.isEmpty()){
            return new Node(-1, "unassigned");
        }
        Tree subTree = tree.getWeightedSubTree(nodesAndWeights);
        subTree.accumulateWeights(subTree.getRoot());
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
