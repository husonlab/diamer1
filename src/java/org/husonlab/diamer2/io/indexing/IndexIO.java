package org.husonlab.diamer2.io.indexing;

import org.husonlab.diamer2.util.logging.Logger;

import java.nio.file.Path;

/**
 * Represents an index folder.
 */
public class IndexIO {
    protected final Logger logger;
    protected final Path indexFolder;
    protected final BucketIO[] bucketIOs;

    /**
     * Create a new IndexIO object.
     * @param indexFolder path to the index folder
     * is missing
     */
    public IndexIO(Path indexFolder) {
        this.logger = new Logger("IndexIO");
        this.indexFolder = indexFolder;
        this.bucketIOs = new BucketIO[1024];
        for (int i = 0; i < 1024; i++) {
            bucketIOs[i] = new BucketIO(indexFolder.resolve(i + ".bin").toFile(), i);
        }
    }

    /**
     * Checks which of the bucket files are available.
     * @return true if all buckets are available, false otherwise
     */
    public boolean bucketMissing() {
        boolean bucketMissing = false;
        for (int i = 0; i < 1024; i++) {
            if (!bucketIOs[i].exists()) {
                bucketMissing = true;
            }
        }
        return bucketMissing;
    }

    public BucketIO.BucketReader getBucketReader(int bucketName) {
        if (bucketIOs[bucketName].exists()) {
            return bucketIOs[bucketName].getBucketReader();
        } else {
            throw new RuntimeException("Bucket " + bucketName + " is missing.");
        }
    }

    public BucketIO getBucketIO(int bucketName) {
        return bucketIOs[bucketName];
    }

    public Path getIndexFolder() {
        return indexFolder;
    }

    public boolean isBucketAvailable(int bucket) {
        return bucketIOs[bucket].exists();
    }
}
