package org.husonlab.diamer2.readAssignment;

import org.husonlab.diamer2.io.BucketReader;
import org.husonlab.diamer2.io.DBIndexIO;
import org.husonlab.diamer2.io.ReadIndexIO;
import org.husonlab.diamer2.logging.Logger;
import org.husonlab.diamer2.taxonomy.Tree;

public class BucketProcessor implements Runnable {

    Logger logger;
    private final Tree tree;
    private final ReadAssignment readAssignment;
    private final DBIndexIO dbIndex;
    private final ReadIndexIO readIndex;
    private final int bucketId;

    public BucketProcessor(
            Tree tree,
            ReadAssignment readAssignment,
            DBIndexIO dbIndex,
            ReadIndexIO readIndex,
            int bucketId) {
        this.logger = new Logger("BucketProcessor");
        this.tree = tree;
        this.readAssignment = readAssignment;
        this.dbIndex = dbIndex;
        this.readIndex = readIndex;
        this.bucketId = bucketId;
    }

    @Override
    public void run() {
        try (BucketReader db = dbIndex.getBucketReader(bucketId);
             BucketReader reads = readIndex.getBucketReader(bucketId)) {
            int dbLength = db.getLength();
            int readsLength = reads.getLength();
            if (dbLength == 0 || readsLength == 0) {
                logger.logWarning("Bucket " + bucketId + " is empty.");
                return;
            }
            long dbEntry = db.next();
            int dbCount = 1;
            long dbKmer = (dbEntry >> 22) & 0xFFFFFFFFFFFL;
            for (int readsCount = 0; readsCount < readsLength; readsCount++) {
                long readsEntry = reads.next();
                long readKmer = (readsEntry >> 22) & 0xFFFFFFFFFFFL;
                while (dbKmer < readKmer && dbCount < dbLength) {
                    dbEntry = db.next();
                    dbKmer = (dbEntry >> 22) & 0xFFFFFFFFFFFL;
                    dbCount++;
                }
                if (dbKmer == readKmer) {
                    int taxId = (int) (dbEntry & 0x3FFFFF);
                    int readId = (int) (readsEntry & 0x3FFFFF);
                    this.readAssignment.addReadAssignment(readId, taxId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
