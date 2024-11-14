package org.husonlab.diamer2.benchmarking;

import org.husonlab.diamer2.indexing.Bucket;
import org.husonlab.diamer2.io.NCBIReader;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

public class RankMapping {
    public static void computeKmerRankMapping(Bucket[] buckets) {
        final ConcurrentHashMap<String, Integer> rankMapping = new ConcurrentHashMap<>();
        final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                1,
                12,
                500L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(1000),
                new ThreadPoolExecutor.CallerRunsPolicy());
        
    }
}
