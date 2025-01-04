package org.husonlab.diamer2.readAssignment;

import org.husonlab.diamer2.taxonomy.Tree;

public record AssignmentStatistics (
        Tree tree,
        Tree.AccumulatedWeightsPerRank[] kmerStatistics,
        PerAlgorithmStatistics[] perAlgorithmStatistics) {

    /**
     * Represents the statistics of a read assignment with a specific algorithm.
     */
    public record PerAlgorithmStatistics(
            String algorithmName,
            int assignedReads,
            int unassignedReads,
            Tree.AccumulatedWeightsPerRank[] kumulativeAssignmentsPerRank){}
}
