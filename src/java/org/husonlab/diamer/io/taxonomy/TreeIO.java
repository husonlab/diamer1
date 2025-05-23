package org.husonlab.diamer.io.taxonomy;

import org.husonlab.diamer.io.CountingInputStream;
import org.husonlab.diamer.taxonomy.Node;
import org.husonlab.diamer.taxonomy.Tree;
import org.husonlab.diamer.util.logging.Logger;
import org.husonlab.diamer.util.logging.OneLineLogger;
import org.husonlab.diamer.util.logging.ProgressBar;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class TreeIO {

    /**
     * Save the tree as a configurable connection table.
     * <p>
     *     The table is ordered from root to leaves so that it is possible to parse it in one iteration.
     * </p>
     * @param tree the tree to save
     * @param file the file to save the tree to
     * @param writeParent whether to write the parent id or not (no connection table anymore)
     * @param writeTaxId whether to write the node id or not
     * @param writeRank whether to write the rank or not
     * @param writeLabel whether to write the node labels or not
     * @param longProperties the long properties to write
     * @param doubleProperties the double properties to write
     */
    public static void saveTree(Tree tree, Path file, boolean writeParent, boolean writeTaxId, boolean writeRank, boolean writeLabel, Iterable<String> longProperties, Iterable<String> doubleProperties) {
        try (BufferedWriter bw = java.nio.file.Files.newBufferedWriter(file)) {
            // write header
            String header =
                    (writeParent ? "parent id\t" : "") +
                    (writeTaxId ? "node id\t" : "") +
                    (writeRank ? "rank\t" : "") +
                    (writeLabel ? "label\t" : "");
            bw.write(header.substring(0, header.length() - 1));
            for (String label: longProperties) {
                bw.write("\t" + label);
            }
            for (String label: doubleProperties) {
                bw.write("\t" + label);
            }
            bw.write("\n");
            // write tree
            LinkedList<Node> queue = new LinkedList<>();
            queue.add(tree.getRoot());
            while (!queue.isEmpty()) {
                Node node = queue.poll();
                String line =
                        (node.hasParent() && writeParent ? node.getParent().getTaxId() + "\t" : (writeParent ? "\t" : "")) +
                        (writeTaxId ? node.getTaxId() + "\t" : "") +
                        (writeRank ? node.getRank() + "\t" : "") +
                        (writeLabel ? node.getScientificNameOrFirstLabel() + "\t" : "");
                bw.write(line.substring(0, line.length() - 1));
                for (String longPerpoerty : longProperties) {
                    bw.write("\t" + tree.getLongProperty(node.getTaxId(), longPerpoerty));
                }
                for (String doubleProperty : doubleProperties) {
                    bw.write("\t" + tree.getDoubleProperty(node.getTaxId(), doubleProperty));
                }
                bw.write("\n");
                queue.addAll(node.getChildren());
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not save tree.", e);
        }
    }

    /**
     * Save the tree as connection table with all node properties.
     * <p>
     *     The table is ordered from root to leaves so that it is possible to parse it in one iteration.
     * </p>
     */
    public static void saveTree(Tree tree, Path file) {
        saveTree(tree, file, true, true, true, true, tree.getLongPropertyLabels(), tree.getDoublePropertyLabels());
    }

    /**
     * Write only taxon labels and properties
     * @param tree the tree to save
     * @param file the file to save the tree to
     */
    public static void savePerTaxonAssignment(Tree tree, Path file) {
        saveTree(tree, file, false, true, true, true, tree.getLongPropertyLabels(), tree.getDoublePropertyLabels());
    }

    /**
     * Write only taxon ids and selected properties for the import in MEGAN.
     * @param tree the tree to save
     * @param file the file to save the tree to
     * @param longProperties the long properties to write
     * @param doubleProperties the double properties to write
     */
    public static void saveForMegan(Tree tree, Path file, Iterable<String> longProperties, Iterable<String> doubleProperties) {
        saveTree(tree, file, false, true, false, false, longProperties, doubleProperties);
    }

    // todo: implement double property parsing
    /**
     * Load a tree from a connection table.
     */
    public static Tree loadTree(Path file) {
        Logger logger = new Logger("TreeIO");
        logger.logInfo("Loading tree from " + file);
        Tree tree = new Tree();
        try (CountingInputStream cis = new CountingInputStream(new FileInputStream(file.toString()));
                BufferedReader br = new BufferedReader(new InputStreamReader(cis))) {
            ProgressBar progressBar = new ProgressBar(file.toFile().length(), 20);
            new OneLineLogger("TreeIO", 100).addElement(progressBar);
            // parse header
            String[] header = br.readLine().split("\t");
            for (int i = 4; i < header.length; i++) {
                tree.addLongProperty(header[i], 0L);
            }
            // parse content
            String line;
            while ((line = br.readLine()) != null) {
                progressBar.setProgress(cis.getBytesRead());
                String[] values = line.split("\t");
                Node parent = values[0].isEmpty() ? null : tree.idMap.get(Integer.parseInt(values[0]));
                int taxId = Integer.parseInt(values[1]);
                String rank = values[2];
                String label = values[3];
                Node node = new Node(taxId, parent, rank, label);
                tree.addNode(taxId, node);
                for (int i = 4; i < values.length; i++) {
                    tree.setProperty(taxId, header[i], Long.parseLong(values[i]));
                }
            }
            progressBar.finish();
        } catch (Exception e) {
            throw new RuntimeException("Could not load tree.", e);
        }
        tree.autoFindRoot();
        return tree;
    }
}
