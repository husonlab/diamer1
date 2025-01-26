package org.husonlab.diamer2.readAssignment.algorithms;

import org.husonlab.diamer2.taxonomy.Tree;

import java.util.List;

/**
 * Class to implement algorithms that take all kmer matches of a read as input and calculate a taxonomic assignment
 * on a given taxonomic tree.
 */
public abstract class AssignmentAlgorithm {
    protected Tree tree;

    /**
     * @param tree The taxonomic tree to use for the assignment
     */
    public AssignmentAlgorithm(Tree tree) {
        this.tree = tree;
    }

    /**
     * Assigns a read to a taxon based on the kmer matches of the read.
     * @param kmerMatches List of integer arrays of size 2, where the first element is the taxon id and the second
     *                    element is the number of kmers that match the taxon. [[taxonId, kmerCount], ...]
     * @return the taxon id the read is assigned to
     */
    public abstract int assignRead(List<int[]> kmerMatches);
    public abstract String getName();
}
