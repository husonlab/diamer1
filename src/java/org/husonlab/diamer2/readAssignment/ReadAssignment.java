package org.husonlab.diamer2.readAssignment;

import org.husonlab.diamer2.util.logging.Logger;
import org.husonlab.diamer2.util.logging.OneLineLogger;
import org.husonlab.diamer2.util.logging.ProgressBar;
import org.husonlab.diamer2.util.logging.Time;
import org.husonlab.diamer2.readAssignment.algorithms.AssignmentAlgorithm;
import org.husonlab.diamer2.taxonomy.Tree;

import java.util.*;

/**
 * Class to store and handle the taxon assignments of all reads.
 */
public class ReadAssignment {

    private final Logger logger;
    private final Tree tree;
    /**
     * Number of reads
     */
    private final int size;
    private final String[] readHeaderMapping;
    /**
     * Raw kmer matches for each read. Each entry is an array of taxon id and the number of kmer matches.
     */
    private final ArrayList<int[]>[] kmerMatches;
    /**
     * List of all assignment algorithms that have been run.
     */
    private final ArrayList<AssignmentAlgorithm> assignmentAlgorithms;
    /**
     * Taxon assignments for each read. Each entry is the taxon id assigned by the corresponding assignment algorithm.
     */
    private final ArrayList<Integer>[] taxonAssignments;

    /**
     * @param tree Taxonomic tree
     * @param readHeaderMapping Array of all read headers. The position of the header in the array is the read id.
     */
    public ReadAssignment(Tree tree, String[] readHeaderMapping) {
        logger = new Logger("ReadAssignment");
        logger.addElement(new Time());
        size = readHeaderMapping.length;
        this.tree = tree;
        this.readHeaderMapping = readHeaderMapping;
        kmerMatches = new ArrayList[size];
        assignmentAlgorithms = new ArrayList<>();
        taxonAssignments = new ArrayList[size];
        for (int i = 0; i < size; i++) {
            this.kmerMatches[i] = new ArrayList<>();
            this.taxonAssignments[i] = new ArrayList<>();
        }
    }

    /**
     * Constructor to create a ReadAssignment with already existing raw read assignments.
     * @param tree Taxonomic tree
     * @param readHeaderMapping Array of all read headers. The position of the header in the array is the read id.
     * @param kmerMatches Array of kmer matches for each read. Each entry is an array of taxon id and the number of kmer matches.
     */
    public ReadAssignment(Tree tree, String[] readHeaderMapping, ArrayList<int[]>[] kmerMatches) {
        this(tree, readHeaderMapping);
        for (int i = 0; i < size; i++) {
            for (int[] kmerMatch : kmerMatches[i]) {
                this.kmerMatches[i].add(kmerMatch);
            }
        }
    }

    /**
     * @return the number of reads
     */
    public int size() {
        return this.size;
    }

    /**
     * Add a kmer match to the read assignment.
     * @param readId Id of the read
     * @param taxId Id of the taxon
     */
    public void addReadAssignment(int readId, int taxId) {
        synchronized (kmerMatches[readId]) {
            for (int[] kmerMatch : kmerMatches[readId]) {
                if (kmerMatch[0] == taxId) {
                    kmerMatch[1]++;
                    return;
                }
            }
            kmerMatches[readId].add(new int[]{taxId, 1});
        }
    }

    /**
     * Sort the kmer matches for each read by the number of kmer matches per taxon.
     */
    public void sortKmerMatches() {
        for (int i = 0; i < size; i++) {
            kmerMatches[i].sort(Comparator.comparingInt(a -> ((int[])a)[1]).reversed());
        }
    }

    /**
     * Assigns the kmer matches to the tree, accumulates and saves them.
     */
    public void addKmerCounts() {
        logger.logInfo("Adding kmer counts to the tree ...");
        tree.resetWeights();
        tree.addWeights(kmerMatches);
        logger.logInfo("Accumulating and saving weights on the tree ...");
        tree.accumulateWeights();
        tree.transferAccumulatedWeightToCustomValue("kmer count");
    }

    /**
     * Run an assignment algorithm on the raw read assignments.
     * <p>The results will be added to the {@link ReadAssignment} as well as the nodes of the tree.</p>
     * @param algorithm Assignment algorithm to run
     */
    public void runAssignmentAlgorithm(AssignmentAlgorithm algorithm) {
        logger.logInfo("Running assignment algorithm: " + algorithm.getName());
        ProgressBar progressBar = new ProgressBar(size, 20);
        new OneLineLogger("ReadAssignment", 500).addElement(progressBar);
        assignmentAlgorithms.add(algorithm);
        tree.resetWeights();
        for (int i = 0; i < size; i++) {
            progressBar.incrementProgress();
            int taxId = algorithm.assignRead(kmerMatches[i]);
            if (taxId != -1) {
                tree.addWeight(taxId, 1);
                taxonAssignments[i].add(taxId);
            }
        }
        progressBar.finish();
        logger.logInfo("Accumulating and saving weights on the tree ...");
        tree.accumulateWeights();
        tree.transferAccumulatedWeightToCustomValue(algorithm.getName());
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
