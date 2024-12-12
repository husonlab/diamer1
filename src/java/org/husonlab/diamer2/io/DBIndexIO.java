package org.husonlab.diamer2.io;

import org.husonlab.diamer2.logging.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;

/**
 * Represents an index folder.
 */
public class DBIndexIO {
    protected final Logger logger;
    protected final Path indexFolder;
    protected final boolean[] availableBuckets;

    /**
     * Create a new DBIndexIO object.
     * @param indexFolder path to the index folder
     * @throws FileNotFoundException if the index folder or the read header mapping file (in case of a READS index)
     * is missing
     */
    public DBIndexIO(Path indexFolder) throws FileNotFoundException {
        this.logger = new Logger("DBIndexIO", false);
        if (!indexFolder.toFile().isDirectory()) {
            logger.logWarning("DBIndexIO folder does not exist.");
            throw new FileNotFoundException("DBIndexIO folder does not exist.");
        }
        this.indexFolder = indexFolder;
        this.availableBuckets = new boolean[1024];
        checkAvailableBuckets();
    }

    /**
     * Checks which of the bucket files are available.
     */
    private void checkAvailableBuckets() {
        for (int i = 0; i < 1024; i++) {
            File bucket = indexFolder.resolve("bucket" + i + ".bin").toFile();
            if (bucket.exists()) {
                availableBuckets[i] = true;
            } else {
                logger.logWarning("Bucket " + i + " is missing.");
                availableBuckets[i] = false;
            }
        }
    }



    @Nullable
    public BucketReader getBucketReader(int bucket) {
        if (!availableBuckets[bucket]) {
            logger.logWarning("Bucket " + bucket + " is missing.");
            return null;
        }
        return new BucketReader(indexFolder.resolve("bucket" + bucket + ".bin").toFile());
    }

    public Path getIndexFolder() {
        return indexFolder;
    }

    public boolean[] getAvailableBuckets() {
        return availableBuckets;
    }

    public boolean isBucketAvailable(int bucket) {
        return availableBuckets[bucket];
    }
}
