package org.husonlab.diamer.io.indexing;

import org.husonlab.diamer.util.logging.Logger;

import java.nio.file.Path;

/**
 * Represents an index folder with one binary file per bucket.
 */
public class IndexIO {
    protected final Logger logger;
    protected final Path indexFolder;
    protected final int nrOfBuckets;
    protected final BucketIO[] bucketIOs;

    /**
     * Create a new IndexIO object.
     * @param indexFolder path to the index folder
     * @param nrOfBuckets number of buckets
     */
    public IndexIO(Path indexFolder, int nrOfBuckets) {
        this.logger = new Logger("IndexIO");
        this.indexFolder = indexFolder;
        this.nrOfBuckets = nrOfBuckets;
        this.bucketIOs = new BucketIO[nrOfBuckets];
        for (int i = 0; i < nrOfBuckets; i++) {
            bucketIOs[i] = new BucketIO(indexFolder.resolve(i + ".bin"), i);
        }
    }

    /**
     * Checks whether all bucket files are available.
     * @return true if all buckets are available, false otherwise
     */
    public boolean bucketMissing() {
        boolean bucketMissing = false;
        for (int i = 0; i < nrOfBuckets; i++) {
            if (!bucketIOs[i].exists()) {
                bucketMissing = true;
            }
        }
        return bucketMissing;
    }

    /**
     * @return a {@link BucketIO.BucketReader} for the specified bucket
     */
    public BucketIO.BucketReader getBucketReader(int bucketName) {
        if (bucketIOs[bucketName].exists()) {
            return bucketIOs[bucketName].getBucketReader();
        } else {
            throw new RuntimeException("Bucket " + bucketName + " is missing.");
        }
    }

    /**
     * @return a {@link BucketIO} for the specified bucket
     */
    public BucketIO getBucketIO(int bucketName) {
        return bucketIOs[bucketName];
    }

    /**
     * @return the path to the index folder
     */
    public Path getIndexFolder() {
        return indexFolder;
    }

    /**
     * Checks if a specific bucket file is available.
     */
    public boolean isBucketAvailable(int bucket) {
        return bucketIOs[bucket].exists();
    }
}
