package org.husonlab.diamer2.io.taxonomy;

import org.husonlab.diamer2.taxonomy.Node;
import org.husonlab.diamer2.taxonomy.Tree;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.*;

public class TreeIO {

    /**
     * Save the tree as connection table.
     * <p>
     *     The table is ordered from root to leaves so that it is possible to parse it in one iteration.
     * </p>
     */
    public static void saveTree(Tree tree, Path file) {
        try (BufferedWriter bw = java.nio.file.Files.newBufferedWriter(file)) {
            // write header
            bw.write("parent id\tnode id\trank\tlabel");
            for (String label: tree.getNodeLongPropertyLabels()) {
                bw.write("\t" + label);
            }
            for (String label: tree.getNodeDoublePropertyLabels()) {
                bw.write("\t" + label);
            }
            bw.write("\n");
            // write tree
            LinkedList<Node> queue = new LinkedList<>();
            queue.add(tree.getRoot());
            while (!queue.isEmpty()) {
                Node node = queue.poll();
                if (node.hasParent()) {
                    bw.write(Integer.toString(node.getParent().getTaxId()));
                }
                bw.write("\t" + node.getTaxId() + "\t" + node.getRank() + "\t" + node.getScientificName());
                for (long longPerpoerty : node.getLongProperties()) {
                    bw.write("\t" + longPerpoerty);
                }
                for (double doubleProperty : node.getDoubleProperties()) {
                    bw.write("\t" + doubleProperty);
                }
                bw.write("\n");
                queue.addAll(node.getChildren());
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not save tree.", e);
        }
    }

    // todo: implement double property parsing
    /**
     * Load a tree from a connection table.
     */
    public static Tree loadTree(Path file) {
        Tree tree = new Tree();
        try (BufferedReader br = new BufferedReader(new FileReader(file.toString()))) {
            // parse header
            String[] header = br.readLine().split("\t");
            // parse content
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split("\t");
                Node parent = values[0].isEmpty() ? null : tree.idMap.get(Integer.parseInt(values[0]));
                int taxId = Integer.parseInt(values[1]);
                String rank = values[2];
                String label = values[3];
                Node node = new Node(taxId, parent, rank, label);
                tree.idMap.put(taxId, node);
                for (int i = 4; i < values.length; i++) {
                    tree.setNodeProperty(taxId, header[i], Long.parseLong(values[i]));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not load tree.", e);
        }
        tree.autoFindRoot();
        return tree;
    }

    /**
     * Saves all custom values that are associated with the nodes of the tree to a tsv file.
     *
     * @param tree       the tree to save the values from
     * @param file       the file to save the values to
     * @param nodeLabels if true the node labels are saved instead of only taxIds
     */
    @Deprecated
    public static void saveCustomValues(Tree tree, Path file, boolean nodeLabels) {
        try (BufferedWriter bw = java.nio.file.Files.newBufferedWriter(file)) {
            bw.write(tree.idMap.size() + "\n");
            if (nodeLabels) {
                bw.write("name");
            } else {
                bw.write("taxId");
            }
            for (String label: tree.getNodeLongPropertyLabels()) {
                bw.write("\t" + label);
            }
            for (String label: tree.getNodeDoublePropertyLabels()) {
                bw.write("\t" + label);
            }
            bw.write("\n");
            for (Node node : tree.idMap.values()) {
                if (nodeLabels) {
                    bw.write(node.toString());
                } else {
                    bw.write(Integer.toString(node.getTaxId()));
                }
                for (long longPerpoerty : node.getLongProperties()) {
                    bw.write("\t" + longPerpoerty);
                }
                for (double doubleProperty : node.getDoubleProperties()) {
                    bw.write("\t" + doubleProperty);
                }
                bw.write("\n");
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not save custom values of the tree.", e);
        }
    }
}
