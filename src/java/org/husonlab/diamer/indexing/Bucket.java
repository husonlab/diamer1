package org.husonlab.diamer.indexing;

import org.husonlab.diamer.main.encoders.Encoder;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Bucket {
    private final int name;
    @NotNull
    private long[] content;

    /**
     * Creates an empty bucket.
     */
    public Bucket(int name) {
        this.name = name;
        this.content = new long[]{0};
    }

    /**
     * Creates a bucket with the provided content.
     * @param name Name (number) of the bucket.
     * @param bucket Content of the bucket.
     */
    public Bucket(int name, @NotNull long[] bucket) {
        this.name = name;
        this.content = bucket;
    }

    /**
     * Creates a bucket with the provided content of a concurrent LinkedQueue as used in read index generation.
     * @param name Name (number) of the bucket.
     * @param bucketList Content of the bucket as a concurrent linked queue.
     */
    public Bucket(int name, @NotNull ConcurrentLinkedQueue<Long> bucketList, @NotNull Encoder encoder) {
        this.name = name;
        content = bucketList.stream().mapToLong(Long::longValue).toArray();
        sort(encoder.getNrOfKmerBitsInBucketEntry());
    }

    /**
     * Sorts the content of the bucket.
     * @param nBits Number of bits (most significant / from the left) to sort by.
     */
    public void sort(int nBits) {
        content = Sorting.radixSortNBits(content, nBits);
    }

    /**
     * Sets the content of the bucket.
     * @param content Content of the bucket.
     */
    public void setContent(@NotNull long[] content) {
        this.content = content;
    }

    /**
     * Returns the content of the bucket.
     * @return Content of the bucket.
     */
    @NotNull
    public long[] getContent() {
        return content;
    }

    public int getSize() {
        return content.length;
    }

    public int getName() {
        return name;
    }
}
