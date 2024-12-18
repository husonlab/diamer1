package org.husonlab.diamer2.indexing;

import org.husonlab.diamer2.io.BucketIO;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public class Bucket {
    private final int name;
    @Nullable
    private long[] content;

    public Bucket(int name) {
        this.name = name;
        this.content = null;
    }

    /**
     * Creates a bucket with the provided content.
     * @param name Name (number) of the bucket.
     * @param bucket Content of the bucket.
     */
    public Bucket(int name, @Nullable long[] bucket) {
        this.name = name;
        this.content = bucket;
    }

    /**
     * Creates a bucket with the provided content of a concurrent HashMap as used for index generation.
     * @param bucketMap Content of the bucket as a concurrent HashMap of kmer and taxId/readId.
     * @param name Name (number) of the bucket.
     */
    public Bucket(int name, ConcurrentHashMap<Long, Integer> bucketMap) {
        this.name = name;
        content = new long[bucketMap.size()];
        int i = 0;
        for (ConcurrentHashMap.Entry<Long, Integer> entry : bucketMap.entrySet()) {
            content[i] = IndexEncoding.getIndexEntry(entry.getKey(), entry.getValue());
            i++;
        }
        sort();
    }

    /**
     * Sorts the content of the bucket.
     * @throws NullPointerException If the bucket is empty.
     */
    public void sort() {
        if (content != null) {
            content = Sorting.radixSort44bits(content);
        } else {
            throw new NullPointerException("Bucket is empty and can not be sorted.");
        }
    }

    /**
     * Sets the content of the bucket.
     * @param content Content of the bucket.
     */
    public void setContent(@Nullable long[] content) {
        this.content = content;
    }

    /**
     * Returns the content of the bucket.
     * @return Content of the bucket.
     */
    @Nullable
    public long[] getContent() {
        return content;
    }

    public int getName() {
        return name;
    }
}
