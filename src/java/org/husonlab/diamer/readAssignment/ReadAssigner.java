package org.husonlab.diamer.readAssignment;

import org.husonlab.diamer.indexing.CustomThreadPoolExecutor;
import org.husonlab.diamer.io.indexing.BucketIO;
import org.husonlab.diamer.io.indexing.DBIndexIO;
import org.husonlab.diamer.io.indexing.ReadIndexIO;
import org.husonlab.diamer.main.GlobalSettings;
import org.husonlab.diamer.main.encoders.Encoder;
import org.husonlab.diamer.taxonomy.Tree;
import org.husonlab.diamer.util.logging.*;

import java.util.ArrayList;
import java.util.concurrent.*;

/**
 * Class to handle the process of matching kmers between the index of the database and the index of the reads.
 */
public class ReadAssigner {
    private final Logger logger;
    private final int progressBarStepsPerBucket;
    private final ProgressBar progressBar;
    private final DBIndexIO dbIndex;
    private final ReadIndexIO readsIndex;
    final ReadAssignment readAssignment;
    private final Encoder encoder;
    private final GlobalSettings settings;

    /**
     * @param encoder encoder with the settings used for encoding the kmers
     */
    public ReadAssigner(Encoder encoder, GlobalSettings settings) {
        this.logger = new Logger("ReadAssigner").addElement(new Time());
        progressBarStepsPerBucket = 100;
        progressBar = new ProgressBar((long) encoder.getNrOfBuckets() * progressBarStepsPerBucket, 20);
        this.dbIndex = encoder.getDBIndexIO();
        this.readsIndex = encoder.getReadIndexIO();
        this.encoder = encoder;
        this.settings = settings;
        String[] readHeaderMapping;
        if (readsIndex.readHeaderMappingExists()) {
            readHeaderMapping = this.readsIndex.getReadHeaderMapping();
        } else {
            throw new RuntimeException("Read header mapping file is missing from the reads index folder.");
        }
        Tree tree;
        if (dbIndex.treeExists()) {
            tree = dbIndex.getTree();
        } else {
            throw new RuntimeException("Taxonomic tree file is missing from the database index folder.");
        }
        if (dbIndex.bucketMissing() || readsIndex.bucketMissing()) {
            logger.logWarning("At least one index file is missing, proceeding with available buckets.");
        }
        readAssignment = new ReadAssignment(tree, readHeaderMapping, settings);
    }

    /**
     * Starts a thread for each pair of buckets that is contained in both indexes.
     * <p>The hits are stored in a {@link ReadAssignment} object. The {@link ReadAssignment} can be used to further
     * analyze the results.</p>
     * @return {@link ReadAssignment} with all the found kmer matches
     */
    public String assignReads() {
        logger.logInfo("Searching kmer matches ...");
        new OneLineLogger("ReadAssigner", 1000)
                .addElement(new RunningTime())
                .addElement(progressBar);

        int bucketsSkipped = 0;
        try (ThreadPoolExecutor threadPoolExecutor = new CustomThreadPoolExecutor(
                settings.MAX_THREADS,
                settings.MAX_THREADS, encoder.getNrOfBuckets(),
                Integer.MAX_VALUE, logger)) {

            for (int i = 0; i < encoder.getNrOfBuckets(); i++) {
                if (dbIndex.isBucketAvailable(i) && readsIndex.isBucketAvailable(i)) {
                    threadPoolExecutor.submit(new BucketProcessor(i));
                } else {
                    progressBar.incrementProgress(progressBarStepsPerBucket);
                    bucketsSkipped++;
                }
            }
        }
        progressBar.finish();

        logger.logInfo("Sorting, normalizing and saving kmer matches ...");
        long nrOfMatchingKmers = 0;
        int nrOfReadsWithMatches = 0;
        int nrOfReadsWithoutMatches = 0;
        for (ArrayList<ReadAssignment.KmerCount<Integer>> readAssignments: readAssignment.getKmerCounts()) {
            if (!readAssignments.isEmpty()) {
                nrOfReadsWithMatches++;
                for (ReadAssignment.KmerCount<Integer> readAssignment: readAssignments) {
                    nrOfMatchingKmers += readAssignment.getCount();
                }
            } else {
                nrOfReadsWithoutMatches++;
            }
        }
        readAssignment.sortKmerCounts();
        readAssignment.addKmerCountsToTree();
        readAssignment.normalizeKmerCounts();

        return "Buckets skipped: " + bucketsSkipped + "\n" +
                "Reads with matches: " + nrOfReadsWithMatches + "\n" +
                "Reads without matches: " + nrOfReadsWithoutMatches + "\n" +
                "Matching kmers: " + nrOfMatchingKmers;
    }

    public ReadAssignment getReadAssignment() {
        return readAssignment;
    }

    /**
     * Class to find matching kmers in two buckets.
     */
    private class BucketProcessor implements Runnable {
        Logger logger;
        private final int bucketId;

        /**
         * @param bucketId id of the bucket to process
         */
        public BucketProcessor(int bucketId) {
            this.logger = new Logger("BucketProcessor").addElement(new Time());
            this.bucketId = bucketId;
        }

        /**
         * Reads over the ascending sorted database and reads bucket simultaneously and advances only the bucket with the
         * smaller kmer. This way, all matching kmers are found and stored in the {@link ReadAssignment}.
         */
        @Override
        public void run() {
            try (BucketIO.BucketReader db = dbIndex.getBucketReader(bucketId);
                 BucketIO.BucketReader reads = readsIndex.getBucketReader(bucketId)) {
                int dbLength = db.getLength();
                int readsLength = reads.getLength();
                if (dbLength == 0 || readsLength == 0) {
                    logger.logWarning("Bucket " + bucketId + " is empty.");
                    return;
                }
                long dbEntry = db.next();
                int dbCount = 1;
                long dbKmer = encoder.getKmerFromIndexEntry(dbEntry);
                float progressUpdateInterval = (readsLength + 1) / (float) progressBarStepsPerBucket;
                // iterate over all kmers in the reads bucket
                for (int readsCount = 0; readsCount < readsLength; readsCount++) {
                    long readsEntry = reads.next();
                    long readKmer = encoder.getKmerFromIndexEntry(readsEntry);
                    // advance the db bucket until the kmer is equal or larger than the read kmer
                    while (Long.compareUnsigned(dbKmer, readKmer) < 0 && dbCount < dbLength) {
                        dbEntry = db.next();
                        dbKmer = encoder.getKmerFromIndexEntry(dbEntry);
                        dbCount++;
                    }
                    // check if the kmers are equal and store hits
                    if (dbKmer == readKmer) {
                        int taxId = encoder.getIdFromIndexEntry(dbEntry);
                        int readId = encoder.getIdFromIndexEntry(readsEntry);
                        readAssignment.addReadAssignment(readId, taxId);
                    }
                    if ((int)((readsCount + 1) % progressUpdateInterval) == 0) progressBar.incrementProgress();
                }
            } catch (Exception e) {
                throw new RuntimeException("Could not process bucket " + bucketId, e);
            }
        }
    }

}
