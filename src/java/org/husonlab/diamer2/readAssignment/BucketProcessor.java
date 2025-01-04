package org.husonlab.diamer2.readAssignment;

import org.husonlab.diamer2.indexing.IndexEncoding;
import org.husonlab.diamer2.io.indexing.BucketIO;
import org.husonlab.diamer2.io.indexing.DBIndexIO;
import org.husonlab.diamer2.io.indexing.ReadIndexIO;
import org.husonlab.diamer2.util.logging.Logger;

public class BucketProcessor implements Runnable {

    Logger logger;
    private final ReadAssignment readAssignment;
    private final DBIndexIO dbIndex;
    private final ReadIndexIO readIndex;
    private final int bucketId;

    public BucketProcessor(
            ReadAssignment readAssignment,
            DBIndexIO dbIndex,
            ReadIndexIO readIndex,
            int bucketId) {
        this.logger = new Logger("BucketProcessor");
        this.readAssignment = readAssignment;
        this.dbIndex = dbIndex;
        this.readIndex = readIndex;
        this.bucketId = bucketId;
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
            long dbKmer = IndexEncoding.getKmer(dbEntry);
            for (int readsCount = 0; readsCount < readsLength; readsCount++) {
                long readsEntry = reads.next();
                long readKmer = IndexEncoding.getKmer(readsEntry);
                while (dbKmer < readKmer && dbCount < dbLength) {
                    dbEntry = db.next();
                    dbKmer = IndexEncoding.getKmer(dbEntry);
                    dbCount++;
                }
                if (dbKmer == readKmer) {
                    int taxId = IndexEncoding.getTaxId(dbEntry);
                    int readId = IndexEncoding.getReadId(readsEntry);
                    readAssignment.addReadAssignment(readId, taxId);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not process bucket " + bucketId, e);
        }
    }
}
