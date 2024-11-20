package org.husonlab.diamer2.io;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.concurrent.TimeUnit;
import org.husonlab.diamer2.graph.Tree;
import org.husonlab.diamer2.graph.Node;

public class NCBIReader {

    /**
     * Parses the NCBI taxonomy, including the node names and the amino acid accessions.
     * @param nodesDumpfile: nodes.dmp, containing the taxonomy nodes (format: tax_id | parent_tax_id | rank)
     * @param namesDumpfile: names.dmp, containing the taxonomy names (format: tax_id | name)
     * @param accessionMappings: array of AccessionMapping objects, containing the path to the mapping file and the column indices for the accession and tax_id.
     */
    @NotNull
    public static Tree readTaxonomyWithAccessions(
            @NotNull File nodesDumpfile,
            @NotNull File namesDumpfile,
            @NotNull AccessionMapping[] accessionMappings) throws IOException {

        final ConcurrentHashMap<Integer, Node> idMap = new ConcurrentHashMap<>(2700000);
        final ConcurrentHashMap<String, Integer> accessionMap = new ConcurrentHashMap<>(1400000000);
        final Tree tree = new Tree(idMap, accessionMap);

        return readTaxonomyWithAccessions(nodesDumpfile, namesDumpfile, accessionMappings, tree);
    }

    /**
     * Parses the NCBI taxonomy, including the node names and the amino acid accessions.
     * Does not set initial HashMap capacities to allow for dynamic resizing in smaller datasets.
     * @param nodesDumpfile: nodes.dmp, containing the taxonomy nodes (format: tax_id | parent_tax_id | rank)
     * @param namesDumpfile: names.dmp, containing the taxonomy names (format: tax_id | name)
     * @param accessionMappings: array of AccessionMapping objects, containing the path to the mapping file and the column indices for the accession and tax_id.
     */
    @NotNull
    public static Tree readTaxonomyWithAccessions(
            @NotNull File nodesDumpfile,
            @NotNull File namesDumpfile,
            @NotNull AccessionMapping[] accessionMappings,
            boolean debug) throws IOException {

        final ConcurrentHashMap<Integer, Node> idMap = new ConcurrentHashMap<>();
        final ConcurrentHashMap<String, Integer> accessionMap = new ConcurrentHashMap<>();
        final Tree tree = new Tree(idMap, accessionMap);

        return readTaxonomyWithAccessions(nodesDumpfile, namesDumpfile, accessionMappings, tree);
    }

    @NotNull
    private static Tree readTaxonomyWithAccessions(
            @NotNull File nodesDumpfile,
            @NotNull File namesDumpfile,
            @NotNull AccessionMapping[] accessionMappings,
            @NotNull Tree tree) throws IOException {
        System.out.println("[NCBIReader] Reading nodes dumpfile...");
        readNodesDumpfile(nodesDumpfile, tree);
        System.out.println("[NCBIReader] Reading names dumpfile...");
        readNamesDumpfile(namesDumpfile, tree);
        for (AccessionMapping mapping : accessionMappings) {
            System.out.println("[NCBIReader] Reading accession mapping from " + mapping.mappingFile);
            readAccessionMap(mapping.mappingFile, mapping.accessionCol, mapping.taxIdCol, tree);
        }
        System.out.printf(
                "[NCBIReader] Finished reading taxonomy. Tree with %d nodes and %d accessions.\n",
                tree.idMap.size(), tree.accessionMap.size()
        );
        return tree;
    }

    /**
     * Reads the NCBI taxonomy from the nodes and names dumpfiles.
     * @param nodesDumpfile path to the nodes dumpfile (nodes.dmp)
     * @param namesDumpfile path to the names dumpfile (names.dmp)
     * @throws IOException
     */
    @NotNull
    public static Tree readTaxonomy(@NotNull File nodesDumpfile, @NotNull File namesDumpfile){
        final ConcurrentHashMap<Integer, Node> idMap = new ConcurrentHashMap<>();
        final Tree tree = new Tree(idMap);
        System.out.println("[NCBIReader] Reading nodes dumpfile...");
        readNodesDumpfile(nodesDumpfile, tree);
        System.out.println("[NCBIReader] Reading names dumpfile...");
        readNamesDumpfile(namesDumpfile, tree);
        System.out.printf(
                "[NCBIReader] Finished reading taxonomy. Tree with %d nodes.\n", idMap.size());
        return tree;
    }

    /**
     * Annotates the entries of the NCBI nr database with the taxon id of the MRCA.
     * The headers in the output file start with the taxon id.
     * @param pathNrInput: path to the input nr file
     * @param pathNrOutput: path to the output nr file
     * @param tree: NCBI taxonomy tree
     */
public static void annotateNrWithLCA(String pathNrInput, String pathNrOutput, Tree tree) throws IOException {
    int skipped = 0;
    int processed = 0;
    int oldProcessed = 0;
    try (BufferedReader br = Files.newBufferedReader(Paths.get(pathNrInput));
         BufferedWriter bw = Files.newBufferedWriter(Paths.get(pathNrOutput))) {
        String line;
        boolean skipping = false;
        long timerStart = System.currentTimeMillis();
        while ((line = br.readLine()) != null) {
            if (line.startsWith(">")) {
                processed++;
                ArrayList<Integer> taxIds = new ArrayList<>();
                for (String value : line.split(" ")) {
                    if (value.startsWith(">") && tree.accessionMap.containsKey(value.substring(1))) {
                        taxIds.add(tree.accessionMap.get(value.substring(1)));
                    }
                }
                if (!taxIds.isEmpty()) {
                    skipping = false;
                    int taxId = taxIds.get(0);
                    for (int i = 1; i < taxIds.size(); i++) {
                        taxId = tree.findLCA(taxId, taxIds.get(i));
                    }
                    bw.write(">%d %s\n".formatted(taxId, line));
                } else {
                    skipping = true;
                    skipped++;
                }
            } else if (!skipping) {
                bw.write(line);
                bw.newLine();
            }
            if (System.currentTimeMillis() - timerStart > 5000) {
                long timerEnd = System.currentTimeMillis();
                System.out.printf("[NCBIReader] Processed %d entries. %f entries per second.\n", processed, (processed - oldProcessed) / ((timerEnd - timerStart) * 10E-2));
                oldProcessed = processed;
                timerStart = timerEnd;
            }
        }
    }
    System.out.printf("[NCBIReader] Skipped %d entries in the nr file, as no accession could be found in the taxonomy.\n", skipped);
}

    /**
     * Reads the NCBI nodes.dmp file and creates a map of tax_id -> Node objects.
     * @param nodesDumpfile: path to the file
     * @param tree: Tree with idMap to store the nodes
     */
    private static void readNodesDumpfile(File nodesDumpfile, Tree tree){
        HashMap<Integer, Integer> parentMap = new HashMap<>();
        try (BufferedReader br = Files.newBufferedReader(nodesDumpfile.toPath())) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split("\t\\|\t");
                int taxId = Integer.parseInt(values[0]);
                int parentTaxId = Integer.parseInt(values[1]);
                String rank = values[2];
                Node node = new Node(taxId, rank);
                tree.idMap.put(taxId, node);
                // parents are recorded separately since the node objects might not have been created yet
                parentMap.put(taxId, parentTaxId);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // set the parent-child relationships after all nodes have been created
        parentMap.forEach( (nodeId, parentId) -> {
            Node node = tree.idMap.get(nodeId);
            Node parent = tree.idMap.get(parentId);
            node.setParent(parent);
            parent.addChild(node);
        });
    }

    /**
     * Reads the NCBI names.dmp file and adds the names to the corresponding Node objects.
     * @param namesDumpfile: path to the file
     * @param tree: Tree with idMap of tax_id -> Node objects
     */
    public static void readNamesDumpfile(File namesDumpfile, Tree tree) {
        try (BufferedReader br = Files.newBufferedReader(namesDumpfile.toPath())) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split("\t\\|\t");
                int taxId = Integer.parseInt(values[0]);
                String label = values[1];
                Node node = tree.idMap.get(taxId);
                node.addLabel(label);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void readAccessionMap(String accessionMapFile, int accessionCol, int taxIdCol, Tree tree) throws IOException {
        try (FileInputStream fis = new FileInputStream(accessionMapFile);
             GZIPInputStream gis = new GZIPInputStream(fis);
             BufferedReader br = new BufferedReader(new InputStreamReader(gis))) {
            String line;
            br.readLine(); // skip header
            long i = 0;
            long start2 = System.nanoTime();
            while ((line = br.readLine()) != null) {
                String[] values = line.split("\t");
                String accession = values[accessionCol];
                int taxId = Integer.parseInt(values[taxIdCol]);
                // only add accessions, if the tax_id is in the tree and the accession is not already in the tree
                if (tree.idMap.containsKey(taxId) && !tree.accessionMap.containsKey(accession)) {
                    tree.accessionMap.put(accession, taxId);
                // case where the tax_id is in the map, but the accession maps to a different taxId
                // find LCA and update the accession map
                } else if (tree.idMap.containsKey(taxId) && tree.accessionMap.get(accession) != taxId) {
                    int oldTaxId = tree.accessionMap.get(accession);
                    int newTaxId = tree.findLCA(oldTaxId, taxId);
                    tree.accessionMap.put(accession, newTaxId);
                }
                if (++i %1000000 == 0) {
                    long end2 = System.nanoTime();
                    System.out.println("[NCBIReader] accessios: " + i/1000000 + "M");
                    System.out.printf("[NCBIReader] accessions per second: %f\n", 1000000/((end2 - start2)*(10E-10)));
                    start2 = System.nanoTime();
                }
            }
        }
    }
    public record AccessionMapping(String mappingFile, int accessionCol, int taxIdCol) {}
}
