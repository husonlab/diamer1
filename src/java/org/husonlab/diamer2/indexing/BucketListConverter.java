package org.husonlab.diamer2.indexing;

import java.util.concurrent.ConcurrentLinkedQueue;

public class BucketListConverter implements Runnable{
    private final ConcurrentLinkedQueue<Long> bucketList;
    private final Bucket bucket;

    public BucketListConverter(ConcurrentLinkedQueue<Long> bucketList, Bucket bucket) {
        this.bucketList = bucketList;
        this.bucket = bucket;
    }

    @Override
    public void run() {
        try {
            System.out.println("[Indexer] Converting bucket " + bucket.getName() + " to array...");
            bucket.setContent(bucketList.stream().mapToLong(Long::longValue).toArray());
            bucketList.clear();
            System.out.println("[Indexer] Sorting bucket " + bucket.getName() + "...");
            bucket.sort();
            System.out.println("[Indexer] Finished converting bucket " + bucket.getName() + ".");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
