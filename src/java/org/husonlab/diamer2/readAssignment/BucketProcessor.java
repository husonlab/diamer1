package org.husonlab.diamer2.readAssignment;

import org.husonlab.diamer2.io.indexing.BucketIO;
import org.husonlab.diamer2.io.indexing.DBIndexIO;
import org.husonlab.diamer2.io.indexing.ReadIndexIO;
import org.husonlab.diamer2.main.encoders.Encoder;
import org.husonlab.diamer2.util.logging.Logger;
import org.husonlab.diamer2.util.logging.Time;


/**
 * Class to find matching kmers in two buckets.
 */
public class BucketProcessor implements Runnable {

    Logger logger;
    private final ReadAssignment readAssignment;
    private final DBIndexIO dbIndex;
    private final ReadIndexIO readIndex;
    private final int bucketId;
    private final Encoder encoder;

    /**
     * @param readAssignment to store the matching kmers in
     * @param dbIndex database index
     * @param readIndex reads index
     * @param bucketId id of the bucket to process
     * @param encoder encoder with the settings that were used for encoding the kmers
     */
    public BucketProcessor(
            ReadAssignment readAssignment,
            DBIndexIO dbIndex,
            ReadIndexIO readIndex,
            int bucketId,
            Encoder encoder) {
        this.logger = new Logger("BucketProcessor").addElement(new Time());
        this.readAssignment = readAssignment;
        this.dbIndex = dbIndex;
        this.readIndex = readIndex;
        this.bucketId = bucketId;
        this.encoder = encoder;
    }

    /**
     * Reads over the ascending sorted database and reads bucket simultaneously and advances only the bucket with the
     * smaller kmer. This way, all matching kmers are found and stored in the {@link ReadAssignment}.
     */
    @Override
    public void run() {
        try (BucketIO.BucketReader db = dbIndex.getBucketReader(bucketId);
             BucketIO.BucketReader reads = readIndex.getBucketReader(bucketId)) {
            int dbLength = db.getLength();
            int readsLength = reads.getLength();
            if (dbLength == 0 || readsLength == 0) {
                logger.logWarning("Bucket " + bucketId + " is empty.");
                return;
            }
            long dbEntry = db.next();
            int dbCount = 1;
            long dbKmer = encoder.getKmerFromIndexEntry(dbEntry);
            // iterate over all kmers in the reads bucket
            for (int readsCount = 0; readsCount < readsLength; readsCount++) {
                long readsEntry = reads.next();
                long readKmer = encoder.getKmerFromIndexEntry(readsEntry);
                // advance the db bucket until the kmer is equal or larger than the read kmer
                while (dbKmer < readKmer && dbCount < dbLength) {
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
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not process bucket " + bucketId, e);
        }
    }
}
