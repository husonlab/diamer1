package org.husonlab.diamer2.benchmarking;


import org.husonlab.diamer2.graph.Tree;
import org.husonlab.diamer2.io.NCBIReader;

import java.util.concurrent.ConcurrentHashMap;

public class RankMapping {
    public static void computeKmerRankMapping(ConcurrentHashMap<Long, Integer>[] buckets, Tree tree) {
        final ConcurrentHashMap<String, Integer> rankMapping = new ConcurrentHashMap<>();
        for (ConcurrentHashMap<Long, Integer> bucket : buckets) {
            bucket.forEachEntry(12, entry -> {
                String rank = tree.idMap.get(entry.getValue()).getRank();
                rankMapping.computeIfAbsent(rank, k -> 0);
                rankMapping.compute(rank, (k, v) -> v + 1);
            });
        }
        rankMapping.forEach((k, v) -> System.out.println(k + "\t" + v));
    }
}
