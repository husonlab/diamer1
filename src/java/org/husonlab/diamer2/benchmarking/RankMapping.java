package org.husonlab.diamer2.benchmarking;


import org.husonlab.diamer2.taxonomy.Tree;
import org.husonlab.diamer2.indexing.Bucket;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;

public class RankMapping {

    private final Path index;
    private final Tree tree;

    public RankMapping(Path index, Tree tree) {
        this.index = index;
        this.tree = tree;
    }

    public void writeRankMapping(int start, int end, File output) {
        ArrayList<Bucket> buckets = Bucket.readBucketRange(index, start, end);
        HashMap<String, Integer> rankMapping = getKmerRankMapping(buckets);
        try (FileWriter fw = new FileWriter(output)) {
            for (String rank : rankMapping.keySet()) {
                fw.write(rank + "\t" + rankMapping.get(rank) + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public HashMap<String, Integer> getRankMapping(int start, int end) {
        ArrayList<Bucket> buckets = Bucket.readBucketRange(index, start, end);
        return getKmerRankMapping(buckets);
    }

    public HashMap<String, Integer> getKmerRankMapping(ArrayList<Bucket> buckets) {
        final HashMap<String, Integer> rankMapping = new HashMap<>(41);
        for (Bucket bucket : buckets) {
            for (int i = 0; i < bucket.getContent().length; i++) {
                int taxId = (int) (bucket.getContent()[i] & 0x3FFFFF);
                String rank = tree.idMap.get(taxId).getRank();
                rankMapping.computeIfPresent(rank, (k, v) -> v + 1);
                rankMapping.computeIfAbsent(rank, k -> 1);
            }
        }
        return rankMapping;
    }
}
