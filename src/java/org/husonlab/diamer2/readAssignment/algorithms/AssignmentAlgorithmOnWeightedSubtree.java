package org.husonlab.diamer2.readAssignment.algorithms;

import org.husonlab.diamer2.readAssignment.ReadAssignment;
import org.husonlab.diamer2.taxonomy.Node;
import org.husonlab.diamer2.taxonomy.Tree;

import java.util.List;

public abstract class AssignmentAlgorithmOnWeightedSubtree implements AssignmentAlgorithm {

    protected final float ratio;

    public AssignmentAlgorithmOnWeightedSubtree(float ratio) {
        this.ratio = ratio;
    }

    @Override
    public int assignKmerCounts(Tree tree, List<ReadAssignment.KmerCount<Integer>> kmerCounts) {
        if (kmerCounts.isEmpty()){
            return -1;
        }
        Tree subTree = tree.getWeightedSubTreeInt(kmerCounts, "weight");
        subTree.accumulateLongProperty("weight", "weight (accumulated)");
        return RecursiveLong(subTree, subTree.getRoot(), ratio);
    }

    @Override
    public int assignNormalizedKmerCounts(Tree tree, List<ReadAssignment.KmerCount<Double>> normalizedKmerCounts) {
        if (normalizedKmerCounts.isEmpty()){
            return -1;
        }
        Tree subTree = tree.getWeightedSubTreeDouble(normalizedKmerCounts, "weight");
        subTree.accumulateDoubleProperty("weight", "weight (accumulated)");
        return RecursiveDouble(subTree, subTree.getRoot(), ratio);
    }

    protected abstract int RecursiveLong(Tree subTree, Node root, float ratio);

    protected abstract int RecursiveDouble(Tree subTree, Node root, float ratio);
}
