package org.husonlab.diamer2.readAssignment;

import org.husonlab.diamer2.io.indexing.BucketIO;
import org.husonlab.diamer2.io.indexing.DBIndexIO;
import org.husonlab.diamer2.io.indexing.ReadIndexIO;
import org.husonlab.diamer2.main.encodingSettings.EncodingSettings;
import org.husonlab.diamer2.util.logging.Logger;

public class BucketProcessor implements Runnable {

    Logger logger;
    private final ReadAssignment readAssignment;
    private final DBIndexIO dbIndex;
    private final ReadIndexIO readIndex;
    private final int bucketId;
    private final EncodingSettings encodingSettings;

    public BucketProcessor(
            ReadAssignment readAssignment,
            DBIndexIO dbIndex,
            ReadIndexIO readIndex,
            int bucketId,
            EncodingSettings encodingSettings) {
        this.logger = new Logger("BucketProcessor");
        this.readAssignment = readAssignment;
        this.dbIndex = dbIndex;
        this.readIndex = readIndex;
        this.bucketId = bucketId;
        this.encodingSettings = encodingSettings;
    }

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
            long dbKmer = encodingSettings.getKmerFromIndexEntry(dbEntry);
            for (int readsCount = 0; readsCount < readsLength; readsCount++) {
                long readsEntry = reads.next();
                long readKmer = encodingSettings.getKmerFromIndexEntry(readsEntry);
                while (dbKmer < readKmer && dbCount < dbLength) {
                    dbEntry = db.next();
                    dbKmer = encodingSettings.getKmerFromIndexEntry(dbEntry);
                    dbCount++;
                }
                if (dbKmer == readKmer) {
                    int taxId = encodingSettings.getIdFromIndexEntry(dbEntry);
                    int readId = encodingSettings.getIdFromIndexEntry(readsEntry);
                    readAssignment.addReadAssignment(readId, taxId);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not process bucket " + bucketId, e);
        }
    }
}
