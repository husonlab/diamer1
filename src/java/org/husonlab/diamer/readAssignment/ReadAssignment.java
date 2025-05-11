package org.husonlab.diamer.readAssignment;

import org.husonlab.diamer.indexing.CustomThreadPoolExecutor;
import org.husonlab.diamer.main.GlobalSettings;
import org.husonlab.diamer.util.logging.Logger;
import org.husonlab.diamer.util.logging.OneLineLogger;
import org.husonlab.diamer.util.logging.ProgressBar;
import org.husonlab.diamer.util.logging.Time;
import org.husonlab.diamer.readAssignment.algorithms.ClassificationAlgorithm;
import org.husonlab.diamer.taxonomy.Tree;

import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;

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
    private final ArrayList<KmerCount<Integer>>[] kmerCounts;
    /**
     * Normalized kmer matches for each read. Each entry is an array of taxon id and the normalized number of kmer matches.
     */
    private final ArrayList<KmerCount<Double>>[] normalizedKmerCounts;
    /**
     * List of all assignment algorithms that have been run.
     */
    private final ArrayList<String> assignmentAlgorithms;
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
        kmerCounts = new ArrayList[size];
        normalizedKmerCounts = new ArrayList[size];
        assignmentAlgorithms = new ArrayList<>();
        taxonAssignments = new ArrayList[size];
        for (int i = 0; i < size; i++) {
            this.kmerCounts[i] = new ArrayList<>();
            this.taxonAssignments[i] = new ArrayList<>();
        }
        this.settings = settings;
    }

    /**
     * Constructor to create a ReadAssignment with already existing raw read assignments.
     * @param tree Taxonomic tree
     * @param readHeaderMapping Array of all read headers. The position of the header in the array is the read id.
     * @param kmerCounts Array of kmer matches for each read. Each entry is an array of taxon id and the number of kmer matches.
     */
    public ReadAssignment(Tree tree, String[] readHeaderMapping, ArrayList<KmerCount<Integer>>[] kmerCounts, GlobalSettings settings) {
        this(tree, readHeaderMapping, settings);
        for (int i = 0; i < size; i++) {
            for (KmerCount<Integer> kmerCount : kmerCounts[i]) {
                this.kmerCounts[i].add(kmerCount);
            }
        }
    }

    public ReadAssignment(Tree tree, String[] readHeaderMapping, ArrayList<KmerCount<Integer>>[] kmerCounts, ArrayList<String> assignmentAlgorithms, ArrayList<Integer>[] taxonAssignments, GlobalSettings settings) {
        this(tree, readHeaderMapping, kmerCounts, settings);
        this.assignmentAlgorithms.addAll(assignmentAlgorithms);
        for (int i = 0; i < size; i++) {
            for (int readAssignment : taxonAssignments[i]) {
                this.taxonAssignments[i].add(readAssignment);
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
        synchronized (kmerCounts[readId]) {
            for (KmerCount<Integer> kmerCount : kmerCounts[readId]) {
                if (kmerCount.getTaxId() == taxId) {
                    kmerCount.count++;
                    return;
                }
            }
            kmerCounts[readId].add(new KmerCount<>(taxId, 1));
        }
    }

    /**
     * Sort the kmer matches for each read by the number of kmer matches per taxon.
     */
    public void sortKmerCounts() {
        for (int i = 0; i < size; i++) {
            kmerCounts[i].sort(Comparator.comparingInt(kmerMatch -> ((KmerCount<Integer>)kmerMatch).count).reversed());
        }
    }

    /**
     * Sort the normalized kmer matches for each read by the number of kmer matches per taxon.
     */
    public void sortNormalizedKmerCounts() {
        for (int i = 0; i < size; i++) {
            normalizedKmerCounts[i].sort(Comparator.comparingDouble(kmerMatch -> ((KmerCount<Double>) kmerMatch).count).reversed());
        }
    }

    /**
     * Adds the kmer counts of the {@link ReadAssignment} to the {@link org.husonlab.diamer.taxonomy.Node}s of the
     * tree, accumulates and saves them.
     */
    public void addKmerCountsToTree() {
        logger.logInfo("Adding kmer counts to the tree ...");
        tree.addLongProperty("kmer count", 0L);
        for (int i = 0; i < size; i++) {
            for (KmerCount<Integer> kmerCount : kmerCounts[i]) {
                tree.addToProperty(kmerCount.getTaxId(), "kmer count", kmerCount.getCount());
            }
        }
        logger.logInfo("Accumulating kmer counts ...");
        tree.accumulateLongProperty("kmer count", "kmer count (accumulated)");
    }

    /**
     * Uses the long property {@code kmers in database} of the tree to normalize the kmer matches for each read.
     * The normalized kmer counts are stored in the {@link #normalizedKmerCounts} array and in the node long property
     * {@code normalized kmer count}.
     * @throws RuntimeException if the tree does not have the property {@code kmers in database}
     */
    public void normalizeKmerCounts() {
        if (!tree.hasLongProperty("kmers in database")) {
            throw new RuntimeException("Tree does not have the property 'kmers in database'.");
        }
        tree.addDoubleProperty("normalized kmer count", 0);
        for (int i = 0; i < size; i++) {
            normalizedKmerCounts[i] = new ArrayList<>();
            for (KmerCount<Integer> kmerCount : kmerCounts[i]) {
                int taxId = kmerCount.getTaxId();
                double normalizedKmerMatch = (double) kmerCount.getCount() / tree.getLongProperty(taxId, "kmers in database");
                tree.addToProperty(taxId, "normalized kmer count", normalizedKmerMatch);
                normalizedKmerCounts[i].add(new KmerCount<>(kmerCount.taxonId, normalizedKmerMatch));
            }
        }
        sortNormalizedKmerCounts();
    }

    /**
     * Run an assignment algorithm on the raw read assignments.
     * <p>The results will be added to the {@link ReadAssignment} as well as the nodes of the tree.</p>
     * @param algorithm Assignment algorithm to run
     */
    public void runAssignmentAlgorithmOnKmerCounts(ClassificationAlgorithm algorithm) {
        ProgressBar progressBar = new ProgressBar(size, 20);
        try (ThreadPoolExecutor threadPoolExecutor = new CustomThreadPoolExecutor(
                settings.MAX_THREADS,
                settings.MAX_THREADS, settings.QUEUE_SIZE,
                1, logger)) {
            logger.logInfo("Running assignment algorithm on kmer counts: " + algorithm.getName());
            new OneLineLogger("ReadAssignment", 500).addElement(progressBar);

            assignmentAlgorithms.add(algorithm.getName() + " read count");
            for (int i = 0; i < size; i++) {
                int finalI = i;
                threadPoolExecutor.submit(() -> {
                    progressBar.incrementProgress();
                    taxonAssignments[finalI].add(algorithm.assignKmerCounts(tree, kmerCounts[finalI]));
                });
            }
        }
        progressBar.finish();
    }

    /**
     * Run an assignment algorithm on the raw read assignments.
     * <p>The results will be added to the {@link ReadAssignment} as well as the nodes of the tree.</p>
     * @param algorithm Assignment algorithm to run
     */
    public void runAssignmentAlgorithmOnNormalizedKmerCounts(ClassificationAlgorithm algorithm) {
        ProgressBar progressBar = new ProgressBar(size, 20);
        try (ThreadPoolExecutor threadPoolExecutor = new CustomThreadPoolExecutor(
                settings.MAX_THREADS,
                settings.MAX_THREADS, settings.QUEUE_SIZE,
                1, logger)) {
            logger.logInfo("Running assignment algorithm on normalized kmer counts: " + algorithm.getName());
            new OneLineLogger("ReadAssignment", 500).addElement(progressBar);

            assignmentAlgorithms.add(algorithm.getName() + " read count (norm. kmers)");
            for (int i = 0; i < size; i++) {
                int finalI = i;
                threadPoolExecutor.submit(() -> {
                    progressBar.incrementProgress();
                    taxonAssignments[finalI].add(algorithm.assignNormalizedKmerCounts(tree, normalizedKmerCounts[finalI]));
                });
            }
        }
        progressBar.finish();
    }

    /**
     * Adds the number of reads assigned by each assignment algorithm to the corresponding taxonomic node.
     */
    public void addReadCountsToTree() {
        logger.logInfo("Adding read counts to the tree ...");
        for (String algorithm: assignmentAlgorithms) {
            tree.addLongProperty(algorithm, 0);
            for (int i = 0; i < size; i++) {
                int taxId = taxonAssignments[i].get(assignmentAlgorithms.indexOf(algorithm));
                if (taxId != -1) {
                    tree.addToProperty(taxId, algorithm, 1);
                }
            }
            tree.accumulateLongProperty(algorithm, algorithm + " cumulative");
        }
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

    public ArrayList<KmerCount<Integer>>[] getKmerCounts() {
        return kmerCounts;
    }

    /**
     * @param readId The id of the read
     * @return The taxon assignments for the input read id in the form [[taxon id, number of kmer matches], ...]
     */
    public ArrayList<KmerCount<Integer>> getKmerCounts(int readId) {
        return kmerCounts[readId];
    }

    /**
     * @return List of all assignment algorithms that have been run
     */
    public ArrayList<String> getAssignmentAlgorithms() {
        return assignmentAlgorithms;
    }

    /**
     * @param readId The id of the read
     * @return The taxon assignments for the input read id
     */
    public ArrayList<Integer> getTaxonAssignments(int readId) {
        return taxonAssignments[readId];
    }

    public static class KmerCount<T extends Number> {
        private final int taxonId;
        protected T count;

        public KmerCount(int taxonId, T count) {
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
