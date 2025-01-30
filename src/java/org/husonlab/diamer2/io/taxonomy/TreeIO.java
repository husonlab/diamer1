package org.husonlab.diamer2.io.taxonomy;

import org.husonlab.diamer2.taxonomy.Node;
import org.husonlab.diamer2.taxonomy.Tree;

import java.io.BufferedWriter;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TreeIO {

    /**
     * Saves all custom values that are associated with the nodes of the tree to a tsv file.
     * @param tree the tree to save the values from
     * @param threshold the minimum value a custom value must have to be saved
     * @param file the file to save the values to
     * @param nodeLabels if true the node labels are saved instead of only taxIds
     */
    @Deprecated
    public static void saveCustomValues(Tree tree, int threshold, Path file, boolean nodeLabels) {
        try (BufferedWriter writer = java.nio.file.Files.newBufferedWriter(file)) {
            writer.write(tree.idMap.size() + "\n");
            if (nodeLabels) {
                writer.write("name");
            } else {
                writer.write("taxId");
            }
            for (String description: tree.getNodeCustomValueDescriptors()) {
                writer.write("\t" + description);
            }
            writer.write("\n");
            for (Node node : tree.idMap.values()) {
                // Skip nodes that do not have a custom value above the threshold
                if (node.customValues.stream().max(Integer::compareTo).orElse(0) < threshold) {
                    continue;
                }
                if (nodeLabels) {
                    writer.write(node.toString());
                } else {
                    writer.write(Integer.toString(node.getTaxId()));
                }
                for (int value: node.customValues) {
                    writer.write("\t" + value);
                }
                writer.write("\n");
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not save custom values of the tree.", e);
        }
    }

    /**
     * Traverses the tree with BFS and collects the accumulated weight dependent on the rank and the threshold.
     * @return a hashmap with the rank as key and a hashmap with the taxId as key and the cummulative weight as value
     */
    @Deprecated
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
