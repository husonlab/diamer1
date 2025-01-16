package org.husonlab.diamer2.io.taxonomy;

import org.husonlab.diamer2.taxonomy.Node;
import org.husonlab.diamer2.taxonomy.Tree;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TreeIO {

    /**
     * Traverses the tree with BFS and collects the accumulated weight dependent on the rank and the threshold.
     * @return a hashmap with the rank as key and a hashmap with the taxId as key and the cummulative weight as value
     */
    public static Tree.WeightsPerRank[] getAccumulatedeWeightPerRank(Tree tree, int threshold) {
        HashMap<String, HashMap<Integer, Integer>> cummulativeWeightPerRank = new HashMap<>();
        LinkedList<Node> stack = new LinkedList<>();
        stack.add(tree.getRoot());
        while (!stack.isEmpty()) {
            Node node = stack.pollLast();
            if (node.getAccumulatedWeight() >= threshold) {
                cummulativeWeightPerRank.computeIfPresent(node.getRank(), (k, v) -> {
                    v.put(node.getTaxId(), node.getAccumulatedWeight());
                    return v;
                });
                cummulativeWeightPerRank.computeIfAbsent(node.getRank(), k -> new HashMap<>()).put(node.getTaxId(), node.getAccumulatedWeight());
            }
            stack.addAll(node.getChildren());
        }

        ArrayList<Tree.WeightsPerRank> result = new ArrayList<>();
        for (Map.Entry<String, HashMap<Integer, Integer>> entry : cummulativeWeightPerRank.entrySet()) {
            String rank = entry.getKey();
            AtomicInteger totalWeight = new AtomicInteger();
            int[][] kmerMatches = entry.getValue().entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .map(e -> {
                        totalWeight.addAndGet(e.getValue());
                        return new int[]{e.getKey(), e.getValue()};
                    })
                    .toArray(int[][]::new);
            result.add(new Tree.WeightsPerRank(rank, totalWeight.get(), kmerMatches));
        }

        result.sort(Comparator.comparingInt(k -> tree.pathToRoot(tree.idMap.get(k.taxonWeights()[0][0])).size()));
        return result.toArray(new Tree.WeightsPerRank[0]);
    }
}
