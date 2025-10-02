package org.husonlab.dbReducer;

import org.husonlab.diamer.io.NCBIReader;
import org.husonlab.diamer.taxonomy.Node;
import org.husonlab.diamer.taxonomy.Tree;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class Main {
    public static void main(@NotNull String[] args) {
        if (args.length != 6) {
            System.err.println("Usage: java -jar dbReducer.jar" +
                    "<database.seqs.gz> <taxonomyFile> <names.dmp> <taxids.txt> <rank> <output>");
            System.exit(1);
        }
        Path database = Path.of(args[0]);
        Path taxonomyFile = Path.of(args[1]);
        Path namesFile = Path.of(args[2]);
        Path taxidsFile = Path.of(args[3]);
        String rank = args[4];

        Tree tree = NCBIReader.readNCBITree(taxonomyFile, namesFile, true);
        Iterable<Integer> taxIdsToOmit = getTaxidsToOmit(tree, readTaxIds(taxidsFile), rank);

        for (Integer taxId : taxIdsToOmit) {
            System.out.println(taxId);
        }
    }

    private static List<Integer> readTaxIds(Path taxidsFile) {
        try {
            return java.nio.file.Files.readAllLines(taxidsFile).stream()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .map(Integer::parseInt)
                    .toList();
        } catch (Exception e) {
            System.err.println("Error reading taxids from file: " + e.getMessage());
            System.exit(1);
            return List.of(); // Unreachable, but required for compilation
        }
    }

    private static Iterable<Integer> getTaxidsToOmit(Tree tree, Iterable<Integer> taxids, String rank) {
        HashSet<Integer> toOmit = new HashSet<>();
        // searching for root of clade of specified rank
        for (Integer taxid : taxids) {
            Node node = tree.getNode(taxid);
            while(node != null && node.hasParent() && !node.isRoot() && !Objects.equals(node.getRank(), rank)) {
                node = node.getParent();
            }
            if (node != null && !node.isRoot()) {
                System.out.println("Found clade root :" + node.getScientificName());
                toOmit.add(node.getTaxId());
                toOmit.addAll(getAllChildrenOfNode(node));
            }
        }
        return toOmit;
    }

    private static @NotNull List<Integer> getAllChildrenOfNode(@NotNull Node node) {
        List<Integer> children = new LinkedList<>();
        for (Node child : node.getChildren()) {
            children.add(child.getTaxId());
            children.addAll(getAllChildrenOfNode(child));
        }
        return children;
    }
}
