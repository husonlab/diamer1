package org.husonlab.diamer.readAssignment.algorithms;

import org.husonlab.diamer.readAssignment.ReadAssignment;
import org.husonlab.diamer.taxonomy.Node;
import org.husonlab.diamer.taxonomy.Tree;

import java.util.List;

public abstract class ClassificationAlgorithmOnWeightedSubtree implements ClassificationAlgorithm {

    protected final float ratio;

    public ClassificationAlgorithmOnWeightedSubtree(float ratio) {
        this.ratio = ratio;
    }

    @Override
    public int assignKmerCounts(Tree tree, List<ReadAssignment.KmerCount<Integer>> kmerCounts) {
        if (kmerCounts.isEmpty()){
            return -1;
        }
        Tree subTree = tree.getWeightedSubTreeInt(kmerCounts, "weight");
        subTree.accumulateLongProperty("weight", "weight (accumulated)");
        return recursiveLong(subTree, subTree.getRoot(), ratio);
    }

    @Override
    public int assignNormalizedKmerCounts(Tree tree, List<ReadAssignment.KmerCount<Double>> normalizedKmerCounts) {
        if (normalizedKmerCounts.isEmpty()){
            return -1;
        }
        Tree subTree = tree.getWeightedSubTreeDouble(normalizedKmerCounts, "weight");
        subTree.accumulateDoubleProperty("weight", "weight (accumulated)");
        return recursiveDouble(subTree, subTree.getRoot(), ratio);
    }

    protected abstract int recursiveLong(Tree subTree, Node root, float ratio);

    protected abstract int recursiveDouble(Tree subTree, Node root, float ratio);
}
