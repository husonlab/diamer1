package org.husonlab.diamer2.readAssignment;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BucketAssignmentProcessor implements Runnable {

    private final int bucketId;
    private final ReadAssigner.ReadAssignment[] readAssignments;
    private final Path dbIndex;
    private final Path readsIndex;

    public BucketAssignmentProcessor(int bucketId, ReadAssigner.ReadAssignment[] readAssignments, Path dbIndex, Path readsIndex) {
        this.bucketId = bucketId;
        this.readAssignments = readAssignments;
        this.dbIndex = dbIndex;
        this.readsIndex = readsIndex;
    }

    @Override
    public void run() {
        try (
                FileInputStream fis1 = new FileInputStream(Paths.get(dbIndex.toString(), bucketId + ".bin").toFile());
                DataInputStream db = new DataInputStream(fis1);
                FileInputStream fis2 = new FileInputStream(Paths.get(readsIndex.toString(), bucketId + ".bin").toFile());
                DataInputStream reads = new DataInputStream(fis2)
        ) {
            int dbLength = db.readInt();
            int readsLength = reads.readInt();
            if (dbLength == 0 || readsLength == 0) {
                return;
            }
            long dbEntry = db.readLong();
            int dbCount = 1;
            long dbKmer = (dbEntry >> 22) & 0xFFFFFFFFFFFL;
            for (int readsCount = 0; readsCount < readsLength; readsCount++) {
                long readsEntry = reads.readLong();
                long readKmer = (readsEntry >> 22) & 0xFFFFFFFFFFFL;
                while (dbKmer < readKmer && dbCount < dbLength) {
                    dbEntry = db.readLong();
                    dbKmer = (dbEntry >> 22) & 0xFFFFFFFFFFFL;
                    dbCount++;
                }
                if (dbKmer == readKmer) {
                    int taxId = (int) (dbEntry & 0x3FFFFF);
                    int readId = (int) (readsEntry & 0x3FFFFF);
                    readAssignments[readId].taxIds().computeIfPresent(taxId, (k, v) -> v + 1);
                    readAssignments[readId].taxIds().putIfAbsent(taxId, 1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
