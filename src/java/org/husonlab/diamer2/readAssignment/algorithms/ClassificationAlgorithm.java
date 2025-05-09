package org.husonlab.diamer2.readAssignment.algorithms;

import org.husonlab.diamer2.readAssignment.ReadAssignment;
import org.husonlab.diamer2.taxonomy.Tree;

import java.util.List;

/**
 * Class to implement algorithms that take all kmer matches of a read as input and calculate a taxonomic assignment
 * on a given taxonomic tree.
 */
public interface ClassificationAlgorithm {

    /**
     * Assigns a read to a taxon based on the kmer matches of the read.
     *
     * @param kmerCounts List of integer arrays of size 2, where the first element is the taxon id and the second
     *                   element is the number of kmers that match the taxon. [[taxonId, KmerCount], ...]
     * @return the taxon id the read is assigned to
     */
    int assignKmerCounts(Tree tree, List<ReadAssignment.KmerCount<Integer>> kmerCounts);

    /**
     * Assigns a read to a taxon based on the normalized kmer matches of the read.
     *
     * @param kmerCounts List of pairs of taxon id and the normalized number of kmers that match the taxon.
     * @return the taxon id the read is assigned to
     */
    int assignNormalizedKmerCounts(Tree tree, List<ReadAssignment.KmerCount<Double>> kmerCounts);

    String getName();
}
