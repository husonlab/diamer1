package org.husonlab.diamer2.readAssignment;

import org.husonlab.diamer2.indexing.CustomThreadPoolExecutor;
import org.husonlab.diamer2.io.indexing.DBIndexIO;
import org.husonlab.diamer2.io.indexing.ReadIndexIO;
import org.husonlab.diamer2.main.GlobalSettings;
import org.husonlab.diamer2.main.encoders.Encoder;
import org.husonlab.diamer2.taxonomy.Tree;
import org.husonlab.diamer2.util.logging.*;

import java.nio.file.Path;
import java.util.concurrent.*;

/**
 * Class to handle the process of matching kmers between the index of the database and the index of the reads.
 */
public class ReadAssigner {
    private final Logger logger;
    private final Tree tree;
    private final DBIndexIO dbIndex;
    private final ReadIndexIO readsIndex;
    private final Encoder encoder;
    private final String[] readHeaderMapping;
    private final GlobalSettings settings;

    /**
     * @param dbIndexPath path to the folder with database index files (buckets)
     * @param readsIndexPath path to the folder with reads index files (buckets)
     * @param encoder encoder with the settings used for encoding the kmers
     */
    public ReadAssigner(Path dbIndexPath, Path readsIndexPath, Encoder encoder, GlobalSettings settings) {
        this.logger = new Logger("ReadAssigner").addElement(new Time());
        this.dbIndex = new DBIndexIO(dbIndexPath, 1024);
        this.readsIndex = new ReadIndexIO(readsIndexPath, 1024);
        this.encoder = encoder;
        this.settings = settings;
        if (readsIndex.readHeaderMappingExists()) {
            this.readHeaderMapping = this.readsIndex.getReadHeaderMapping();
        } else {
            throw new RuntimeException("Read header mapping file is missing from the reads index folder.");
        }
        if (dbIndex.treeExists()) {
            tree = dbIndex.getTree();
        } else {
            throw new RuntimeException("Taxonomic tree file is missing from the database index folder.");
        }
        if (dbIndex.bucketMissing() || readsIndex.bucketMissing()) {
            logger.logWarning("At least one index file is missing, proceeding with available buckets.");
        }
    }

    /**
     * Starts a thread for each pair of buckets that is contained in both indexes.
     * <p>The hits are stored in a {@link ReadAssignment} object. The {@link ReadAssignment} can be used to further
     * analyze the results.</p>
     * @return {@link ReadAssignment} with all the found kmer matches
     */
    public ReadAssignment assignReads() {
        logger.logInfo("Searching kmer matches ...");
        ProgressBar progressBar = new ProgressBar(encoder.getNumberOfBuckets(), 20);
        new OneLineLogger("ReadAssigner", 1000)
                .addElement(new RunningTime())
                .addElement(progressBar);

        final ReadAssignment readAssignment = new ReadAssignment(tree, readHeaderMapping, settings);
        ThreadPoolExecutor threadPoolExecutor = new CustomThreadPoolExecutor(
                settings.MAX_THREADS,
                settings.MAX_THREADS, settings.QUEUE_SIZE,
                1, logger);

        int bucketsSkipped = 0;

        for (int i = 0; i < encoder.getNumberOfBuckets(); i++) {
            if (dbIndex.isBucketAvailable(i) && readsIndex.isBucketAvailable(i)) {
                threadPoolExecutor.submit(new BucketProcessor(readAssignment, dbIndex, readsIndex, i, encoder));
            } else {
                bucketsSkipped++;
            }
        }

        // Wait for all threads to finish and update progress bar
        threadPoolExecutor.shutdown();
        while (!threadPoolExecutor.isTerminated()) {
            progressBar.setProgress(threadPoolExecutor.getCompletedTaskCount() + bucketsSkipped);
            try {
                threadPoolExecutor.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        progressBar.finish();
        if (bucketsSkipped > 0) {
            logger.logWarning("Skipped %d buckets.".formatted(bucketsSkipped));
        }

        logger.logInfo("Sorting, normalizing and saving kmer matches ...");
        readAssignment.sortKmerMatches();
        readAssignment.addKmerCountsToTree();
        readAssignment.normalizeKmerMatchesAndAddToTree();

        return readAssignment;
    }
}
