package org.husonlab.diamer2.indexing;

import java.util.concurrent.ConcurrentHashMap;

public class BucketMapConverter implements Runnable{
    private final ConcurrentHashMap<Long, Integer> bucketMap;
    private final Bucket bucket;

    /**
     * Converts a ConcurrentHashMap to a Bucket.
     * @param bucketMap ConcurrentHashMap to convert.
     * @param bucket Bucket to store the converted data.
     */
    public BucketMapConverter(ConcurrentHashMap<Long, Integer> bucketMap, Bucket bucket) {
        this.bucketMap = bucketMap;
        this.bucket = bucket;
    }

    @Override
    public void run() {
        long[] bucketArray = new long[bucketMap.size()];
        int i = 0;
        for (ConcurrentHashMap.Entry<Long, Integer> entry : bucketMap.entrySet()) {
            bucketArray[i] = ((entry.getKey() << 10) & 0xffffffffffc00000L ) | entry.getValue();
            i++;
        }
        bucket.setContent(bucketArray);
        bucket.sort();
    }
}
