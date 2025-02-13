package org.husonlab.diamer2.readAssignment;

import org.husonlab.diamer2.indexing.CustomThreadPoolExecutor;
import org.husonlab.diamer2.main.GlobalSettings;
import org.husonlab.diamer2.util.logging.Logger;
import org.husonlab.diamer2.util.logging.OneLineLogger;
import org.husonlab.diamer2.util.logging.ProgressBar;
import org.husonlab.diamer2.util.logging.Time;
import org.husonlab.diamer2.readAssignment.algorithms.AssignmentAlgorithm;
import org.husonlab.diamer2.taxonomy.Tree;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Phaser;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
    private final ArrayList<KmerMatch<Integer>>[] kmerMatches;
    /**
     * Normalized kmer matches for each read. Each entry is an array of taxon id and the normalized number of kmer matches.
     */
    private final ArrayList<KmerMatch<Double>>[] normalizedKmerMatches;
    /**
     * List of all assignment algorithms that have been run.
     */
    private final ArrayList<AssignmentAlgorithm> assignmentAlgorithms;
    /**
     * Taxon assignments for each read. Each entry is the taxon id assigned by the corresponding assignment algorithm.
     */
    private final ArrayList<Integer>[] taxonAssignments;

    private final GlobalSettings settings;

    /**
     * @param tree Taxonomic tree
     * @param readHeaderMapping Array of all read headers. The position of the header in the array is the read id.
     */
    public ReadAssignment(Tree tree, String[] readHeaderMapping, GlobalSettings settings) {
        logger = new Logger("ReadAssignment");
        logger.addElement(new Time());
        size = readHeaderMapping.length;
        this.tree = tree;
        this.readHeaderMapping = readHeaderMapping;
        kmerMatches = new ArrayList[size];
        normalizedKmerMatches = new ArrayList[size];
        assignmentAlgorithms = new ArrayList<>();
        taxonAssignments = new ArrayList[size];
        for (int i = 0; i < size; i++) {
            this.kmerMatches[i] = new ArrayList<>();
            this.taxonAssignments[i] = new ArrayList<>();
        }
        this.settings = settings;
    }

    /**
     * Constructor to create a ReadAssignment with already existing raw read assignments.
     * @param tree Taxonomic tree
     * @param readHeaderMapping Array of all read headers. The position of the header in the array is the read id.
     * @param kmerMatches Array of kmer matches for each read. Each entry is an array of taxon id and the number of kmer matches.
     */
    public ReadAssignment(Tree tree, String[] readHeaderMapping, ArrayList<KmerMatch<Integer>>[] kmerMatches, GlobalSettings settings) {
        this(tree, readHeaderMapping, settings);
        for (int i = 0; i < size; i++) {
            for (KmerMatch<Integer> kmerMatch : kmerMatches[i]) {
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
            for (KmerMatch<Integer> kmerMatch : kmerMatches[readId]) {
                if (kmerMatch.getTaxId() == taxId) {
                    kmerMatch.count++;
                    return;
                }
            }
            kmerMatches[readId].add(new KmerMatch<>(taxId, 1));
        }
    }

    /**
     * Sort the kmer matches for each read by the number of kmer matches per taxon.
     */
    public void sortKmerMatches() {
        for (int i = 0; i < size; i++) {
            kmerMatches[i].sort(Comparator.comparingInt(kmerMatch -> ((KmerMatch<Integer>)kmerMatch).count).reversed());
        }
    }

    /**
     * Normalize the kmer matches for each read by the number of kmers in the database for each taxon.
     * <p>
     *     The normalized kmer matches will be also added to the tree.
     * </p>
     */
    public void normalizeKmerMatchesAndAddToTree() {
        tree.addNodeDoubleProperty("kmer count normalized", 0);
        for (int i = 0; i < size; i++) {
            normalizedKmerMatches[i] = new ArrayList<>();
            for (KmerMatch<Integer> kmerMatch : kmerMatches[i]) {
                double normalizedKmerMatch = (double) kmerMatch.getCount() / tree.getNodeLongProperty(kmerMatch.getTaxId(), "kmers in database");
                tree.addToNodeProperty(kmerMatch.getTaxId(), "kmer count normalized", normalizedKmerMatch);
                normalizedKmerMatches[i].add(new KmerMatch<Double>(kmerMatch.taxonId, normalizedKmerMatch));
            }
        }
        sortNormalizedKmerMatches();
    }

    /**
     * Sort the normalized kmer matches for each read by the number of kmer matches per taxon.
     */
    public void sortNormalizedKmerMatches() {
        for (int i = 0; i < size; i++) {
            normalizedKmerMatches[i].sort(Comparator.comparingDouble(kmerMatch -> ((KmerMatch<Double>) kmerMatch).count).reversed());
        }
    }

    /**
     * Assigns the kmer matches to the tree, accumulates and saves them.
     */
    public void addKmerCountsToTree() {
        logger.logInfo("Adding kmer counts to the tree ...");
        tree.addNodeLongProperty("kmer count", 0L);
        for (int i = 0; i < size; i++) {
            for (KmerMatch<Integer> kmerMatch : kmerMatches[i]) {
                tree.addToNodeProperty(kmerMatch.getTaxId(), "kmer count", kmerMatch.getCount());
            }
        }
        logger.logInfo("Accumulating and saving weights on the tree ...");
        tree.accumulateNodeLongProperty("kmer count", "kmer count (accumulated)");
    }

    /**
     * Run an assignment algorithm on the raw read assignments.
     * <p>The results will be added to the {@link ReadAssignment} as well as the nodes of the tree.</p>
     * @param algorithm Assignment algorithm to run
     */
    public void runAssignmentAlgorithm(AssignmentAlgorithm algorithm) {
        ThreadPoolExecutor threadPoolExecutor = new CustomThreadPoolExecutor(
                settings.MAX_THREADS,
                settings.MAX_THREADS,
                500L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(500),
                new ThreadPoolExecutor.CallerRunsPolicy());

        logger.logInfo("Running assignment algorithm: " + algorithm.getName());
        ProgressBar progressBar = new ProgressBar(size, 20);
        new OneLineLogger("ReadAssignment", 500).addElement(progressBar);

        assignmentAlgorithms.add(algorithm);
        assignmentAlgorithms.add(algorithm);
        tree.addNodeLongProperty(algorithm.getName() + " read count", 0);
        tree.addNodeLongProperty(algorithm.getName() + " read count normalized", 0);
        for (int i = 0; i < size; i++) {
            int finalI = i;
            threadPoolExecutor.submit(() -> {
                progressBar.incrementProgress();
                int taxId = algorithm.assignRawReadKmerMatches(kmerMatches[finalI]);
                int taxIdNormalized = algorithm.assignNormalizedReadKmerMatches(normalizedKmerMatches[finalI]);
                if (taxId != -1) {
                    tree.addToNodeProperty(taxId, algorithm.getName() + " read count", 1);
                }
                if (taxIdNormalized != -1) {
                    tree.addToNodeProperty(taxIdNormalized, algorithm.getName() + " read count normalized", 1);
                }
                taxonAssignments[finalI].add(taxId);
                taxonAssignments[finalI].add(taxIdNormalized);
            });
        }

        threadPoolExecutor.shutdown();
        try {
            if (threadPoolExecutor.awaitTermination(1, TimeUnit.MINUTES)) {
                progressBar.finish();
                logger.logInfo("Assignment complete.");
            } else {
                logger.logError("Assignment timed out.");
                threadPoolExecutor.shutdownNow();
                throw new RuntimeException("Assignment timed out.");
            }
        } catch (InterruptedException e) {
            logger.logError("Assignment interrupted.");
            threadPoolExecutor.shutdownNow();
            throw new RuntimeException("Assignment interrupted.");
        }
        logger.logInfo("Accumulating and saving weights on the tree ...");
        tree.accumulateNodeLongProperty(algorithm.getName() + " read count", algorithm.getName() + " read count (accumulated)");
        tree.accumulateNodeLongProperty(algorithm.getName() + " read count normalized", algorithm.getName() + " read count normalized (accumulated)");
    }

    /**
     * @return Taxonomic tree used for the read assignment
     */
    public Tree getTree() {
        return tree;
    }

    /**
     * @param readId The id of the read
     * @return The header string that belongs to the input read id
     */
    public String getReadHeader(int readId) {
        return readHeaderMapping[readId];
    }

    /**
     * @param readId The id of the read
     * @return The taxon assignments for the input read id in the form [[taxon id, number of kmer matches], ...]
     */
    public ArrayList<KmerMatch<Integer>> getKmerMatches(int readId) {
        return kmerMatches[readId];
    }

    /**
     * @return List of all assignment algorithms that have been run
     */
    public ArrayList<AssignmentAlgorithm> getAssignmentAlgorithms() {
        return assignmentAlgorithms;
    }

    /**
     * @param readId The id of the read
     * @return The taxon assignments for the input read id
     */
    public ArrayList<Integer> getTaxonAssignments(int readId) {
        return taxonAssignments[readId];
    }

    public static class KmerMatch<T extends Number> {
        private final int taxonId;
        protected T count;

        public KmerMatch(int taxonId, T count) {
            this.taxonId = taxonId;
            this.count = count;
        }

        protected void setCount(T count) {
            this.count = count;
        }

        public int getTaxId() {
            return taxonId;
        }

        public T getCount() {
            return count;
        }
    }
}
