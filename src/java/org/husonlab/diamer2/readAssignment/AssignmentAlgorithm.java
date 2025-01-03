package org.husonlab.diamer2.readAssignment;

import org.husonlab.diamer2.taxonomy.Tree;

import java.util.ArrayList;

public abstract class AssignmentAlgorithm {
    protected Tree tree;

    public AssignmentAlgorithm(Tree tree) {
        this.tree = tree;
    }

    public abstract int assignRead(ArrayList<int[]> kmerMatches);
    public abstract String getName();
}
