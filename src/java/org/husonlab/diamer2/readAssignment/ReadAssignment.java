package org.husonlab.diamer2.readAssignment;

import org.husonlab.diamer2.util.logging.Logger;
import org.husonlab.diamer2.util.logging.OneLineLogger;
import org.husonlab.diamer2.util.logging.ProgressBar;
import org.husonlab.diamer2.util.logging.Time;
import org.husonlab.diamer2.readAssignment.algorithms.AssignmentAlgorithm;
import org.husonlab.diamer2.taxonomy.Tree;

import java.util.*;

import static org.husonlab.diamer2.io.taxonomy.TreeIO.getAccumulatedeWeightPerRank;

public class ReadAssignment {

    private final Logger logger;
    private final Tree tree;
    private final int size;
    private final String[] readHeaderMapping;
    private final ArrayList<int[]>[] kmerMatches;
    private final ArrayList<AssignmentAlgorithm> assignmentAlgorithms;
    private final ArrayList<Integer>[] taxonAssignments;

    public ReadAssignment(Tree tree, String[] readHeaderMapping) {
        logger = new Logger("ReadAssignment");
        logger.addElement(new Time());
        this.size = readHeaderMapping.length;
        this.tree = tree;
        this.readHeaderMapping = readHeaderMapping;
        this.kmerMatches = new ArrayList[size];
        this.assignmentAlgorithms = new ArrayList<>();
        this.taxonAssignments = new ArrayList[size];
        for (int i = 0; i < size; i++) {
            this.kmerMatches[i] = new ArrayList<>();
            this.taxonAssignments[i] = new ArrayList<>();
        }
    }

    public ReadAssignment(Tree tree, String[] readHeaderMapping, ArrayList<int[]>[] kmerMatches) {
        this(tree, readHeaderMapping);
        for (int i = 0; i < size; i++) {
            for (int[] kmerMatch : kmerMatches[i]) {
                this.kmerMatches[i].add(kmerMatch);
            }
        }
    }

    public int size() {
        return this.size;
    }

    public void addReadAssignment(int readId, int taxId) {
        synchronized (kmerMatches[readId]) {
            if (kmerMatches[readId].contains(taxId)) {
                kmerMatches[readId].get(taxId)[1]++;
            } else {
                kmerMatches[readId].add(new int[]{taxId, 1});
            }
        }
    }

    public void runAssignmentAlgorithm(AssignmentAlgorithm algorithm) {
        logger.logInfo("Running assignment algorithm: " + algorithm.getName());
        ProgressBar progressBar = new ProgressBar(size, 20);
        new OneLineLogger("ReadAssignment", 500).addElement(progressBar);
        assignmentAlgorithms.add(algorithm);
        for (int i = 0; i < size; i++) {
            progressBar.incrementProgress();
            taxonAssignments[i].add(algorithm.assignRead(kmerMatches[i]));
        }
        progressBar.finish();
    }

    public AssignmentStatistics calculateStatistics() {
        return new AssignmentStatistics(
                tree,
                calculateKmerStatistics(),
                calculateReadStatistics()
        );
    }

    private Tree.WeightsPerRank[] calculateKmerStatistics() {
        logger.logInfo("Adding kmer counts to the tree ...");
        tree.resetWeights();
        tree.addWeights(kmerMatches);
        logger.logInfo("Accumulating kmer counts ...");
        tree.autoFindRoot();
        tree.accumulateWeights();
        logger.logInfo("Calculating kumulative kmer matches per rank ...");
        return getAccumulatedeWeightPerRank(tree, 10);
    }

    private AssignmentStatistics.PerAlgorithmStatistics[] calculateReadStatistics() {
        logger.logInfo("Calculating read statistics ...");
        AssignmentStatistics.PerAlgorithmStatistics[] perAlgorithmStatistics = new AssignmentStatistics.PerAlgorithmStatistics[assignmentAlgorithms.size()];

        for (int i = 0; i < assignmentAlgorithms.size(); i++) {
            int assignedReads = 0;
            int unassignedReads = 0;
            AssignmentAlgorithm assignmentAlgorithm = assignmentAlgorithms.get(i);
            logger.logInfo("Calculating statistics for algorithm: " + assignmentAlgorithm.getName());
            tree.resetWeights();
            for (int j = 0; j < size; j++) {
                int assignment = taxonAssignments[j].get(i);
                if (assignment == -1) {
                    unassignedReads++;
                } else {
                    assignedReads++;
                    tree.addWeight(assignment, 1);
                }
            }
            tree.autoFindRoot();
            tree.accumulateWeights();
            perAlgorithmStatistics[i] = new AssignmentStatistics.PerAlgorithmStatistics(
                    assignmentAlgorithm.getName(),
                    assignedReads,
                    unassignedReads,
                    getAccumulatedeWeightPerRank(tree, 1)
            );
        }
        return perAlgorithmStatistics;
    }


    public Tree getTree() {
        return tree;
    }

    public String getReadHeader(int readId) {
        return readHeaderMapping[readId];
    }

    public ArrayList<int[]> getKmerMatches(int readId) {
        return kmerMatches[readId];
    }
}
