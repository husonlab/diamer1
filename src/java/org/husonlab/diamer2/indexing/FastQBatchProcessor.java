package org.husonlab.diamer2.indexing;

import org.husonlab.diamer2.seq.Sequence;

import java.util.concurrent.ConcurrentHashMap;

public class FastQBatchProcessor implements Runnable {

    private static Sequence[] batch;
    private static ConcurrentHashMap<Long, Integer>[] bucketMaps;
    private static ConcurrentHashMap<Integer, String> readHeaderMap;
    private static int[] currentBucketRange;
    private static int startIndex;

    public FastQBatchProcessor(Sequence[] batch, ConcurrentHashMap<Long, Integer>[] bucketMaps, ConcurrentHashMap<Integer, String> readHeaderMap, int[] currentBucketRange, int startIndex) {
        this.batch = batch;
        this.bucketMaps = bucketMaps;
        this.readHeaderMap = readHeaderMap;
        this.currentBucketRange = currentBucketRange;
        this.startIndex = startIndex;
    }

    @Override
    public void run() {

    }
}
