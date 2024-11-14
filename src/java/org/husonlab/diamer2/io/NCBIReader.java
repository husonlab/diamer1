package org.husonlab.diamer2.io;

import com.sun.source.tree.Tree;
import org.checkerframework.checker.units.qual.N;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.concurrent.TimeUnit;

public class NCBIReader {

    /**
     * Parses the NCBI taxonomy from the following files:
     * @param nodesDumpfile: nodes.dmp, containing the taxonomy nodes (format: tax_id | parent_tax_id | rank)
     * @param namesDumpfile: names.dmp, containing the taxonomy names (format: tax_id | name)
     * @param accessionMappings: array of AccessionMapping objects, containing the path to the mapping file and the column indices for the accession and tax_id.
     */
    @NotNull
    public static Tree readTaxonomy(
            @NotNull String nodesDumpfile,
            @NotNull String namesDumpfile,
            @NotNull AccessionMapping[] accessionMappings) throws IOException {

        final ConcurrentHashMap<Integer, Node> idMap = new ConcurrentHashMap<>(2700000);
        final ConcurrentHashMap<String, Integer> accessionMap = new ConcurrentHashMap<>(1400000000);

        return readTaxonomy(nodesDumpfile, namesDumpfile, accessionMappings, idMap, accessionMap);
    }

    /**
     * Parses the NCBI taxonomy from the following files:
     * @param nodesDumpfile: nodes.dmp, containing the taxonomy nodes (format: tax_id | parent_tax_id | rank)
     * @param namesDumpfile: names.dmp, containing the taxonomy names (format: tax_id | name)
     * @param accessionMappings: array of AccessionMapping objects, containing the path to the mapping file and the column indices for the accession and tax_id.
     * @param debug: boolean to disable initial HashMap sizes and save memory on reduced datasets.
     */
    @NotNull
    public static Tree readTaxonomy(
            @NotNull String nodesDumpfile,
            @NotNull String namesDumpfile,
            @NotNull AccessionMapping[] accessionMappings,
            boolean debug) throws IOException {

        final ConcurrentHashMap<Integer, Node> idMap = new ConcurrentHashMap<>();
        final ConcurrentHashMap<String, Integer> accessionMap = new ConcurrentHashMap<>();

        return readTaxonomy(nodesDumpfile, namesDumpfile, accessionMappings, idMap, accessionMap);
    }

    @NotNull
    private static Tree readTaxonomy(
            @NotNull String nodesDumpfile,
            @NotNull String namesDumpfile,
            @NotNull AccessionMapping[] accessionMappings,
            @NotNull ConcurrentHashMap<Integer, Node> idMap,
            @NotNull ConcurrentHashMap<String, Integer> accessionMap) throws IOException {
        System.out.println("[NCBIReader] Reading nodes dumpfile...");
        readNodesDumpfile(nodesDumpfile, idMap);
        System.out.println("[NCBIReader] Reading names dumpfile...");
        readNamesDumpfile(namesDumpfile, idMap);
        for (AccessionMapping mapping : accessionMappings) {
            System.out.println("[NCBIReader] Reading accession mapping from " + mapping.mappingFile);
            readAccessionMap(mapping.mappingFile, mapping.accessionCol, mapping.taxIdCol, idMap, accessionMap);
        }
        System.out.printf(
                "[NCBIReader] Finished reading taxonomy. Tree with %d nodes and %d accessions.\n",
                idMap.size(), accessionMap.size()
        );
        return new Tree(idMap, accessionMap);
    }

    /**
     * Annotates the entries of the NCBI nr database with the taxon id of the MRCA.
     * The headers in the output file start with the taxon id.
     * @param pathNrInput: path to the input nr file
     * @param pathNrOutput: path to the output nr file
     * @param tree: NCBI taxonomy tree
     */
private static void annotateNrWithMRCA(String pathNrInput, String pathNrOutput, Tree tree) throws IOException {
    int skipped = 0;
    try (BufferedReader br = Files.newBufferedReader(Paths.get(pathNrInput));
         BufferedWriter bw = Files.newBufferedWriter(Paths.get(pathNrOutput))) {
        String line;
        boolean skipping = false;
        while ((line = br.readLine()) != null) {
            if (line.startsWith(">")) {
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
                        taxId = tree.findMRCA(taxId, taxIds.get(i));
                    }
                    bw.write(">%d %s".formatted(taxId, line));
                } else {
                    skipping = true;
                    skipped++;
                }
            } else if (!skipping) {
                bw.write(line);
                bw.newLine();
            }
        }
    }
    System.out.printf("[NCBIReader] Skipped %d entries in the nr file, as no accession could be found in the taxonomy.\n", skipped);
}

    /**
     * Reads the NCBI nodes.dmp file and creates a map of tax_id -> Node objects.
     * @param nodesDumpfile: path to the file
     * @param idMap: map to store the nodes
     */
    private static void readNodesDumpfile(String nodesDumpfile, ConcurrentHashMap<Integer, Node> idMap) throws IOException {
        HashMap<Integer, Integer> parentMap = new HashMap<>();
        try (BufferedReader br = Files.newBufferedReader(Paths.get(nodesDumpfile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split("\t\\|\t");
                int taxId = Integer.parseInt(values[0]);
                int parentTaxId = Integer.parseInt(values[1]);
                String rank = values[2];
                Node node = new Node(taxId, rank);
                idMap.put(taxId, node);
                // parents are recorded separately since the node objects might not have been created yet
                parentMap.put(taxId, parentTaxId);
            }
        }
        // set the parent-child relationships after all nodes have been created
        parentMap.forEach( (nodeId, parentId) -> {
            Node node = idMap.get(nodeId);
            Node parent = idMap.get(parentId);
            node.setParent(parent);
            parent.addChild(node);
        });
    }

    /**
     * Reads the NCBI names.dmp file and adds the names to the corresponding Node objects.
     * @param namesDumpfile: path to the file
     * @param idMap: map of tax_id -> Node objects
     */
    public static void readNamesDumpfile(String namesDumpfile, ConcurrentHashMap<Integer, Node> idMap) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(Paths.get(namesDumpfile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split("\t\\|\t");
                int taxId = Integer.parseInt(values[0]);
                String label = values[1];
                Node node = idMap.get(taxId);
                node.addLabel(label);
            }
        }
    }
    public static void readAccessionMap(String accessionMapFile, int accessionCol, int taxIdCol, ConcurrentHashMap<Integer, Node> idMap, ConcurrentHashMap<String, Integer> accessionMap) throws IOException {
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
                if (idMap.containsKey(taxId) && !accessionMap.containsKey(accession)) {
                    accessionMap.put(accession, taxId);
                // case where the tax_id is in the map, but the accession maps to a different taxId
                } else if (idMap.containsKey(taxId) && accessionMap.get(accession) != taxId) {
                    System.err.printf(
                            "[NCBIReader] Accession %s already exists in the tree with a different node. \n\texisting: %s\n\tnew: %s\n",
                            accession, idMap.get(accessionMap.get(accession)), idMap.get(taxId)
                    );
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
    public static class Node {
        private final int taxId;                // required
        private Node parent;                    // can be null
        private final ArrayList<Node> children; // can be empty
        private final ArrayList<String> labels; // can be empty
        private String rank;                    // can be null

        public Node(int taxId) {
            this.taxId = taxId;
            this.children = new ArrayList<>();
            this.labels = new ArrayList<>();
        }

        public Node(int taxId, Node parent) {
            this.taxId = taxId;
            this.parent = parent;

            this.children = new ArrayList<>();
            this.labels = new ArrayList<>();
        }

        public Node(int taxId, String rank) {
            this.taxId = taxId;
            this.rank = rank;

            this.children = new ArrayList<>();
            this.labels = new ArrayList<>();
        }

        public Node(int taxId, Node parent, String rank) {
            this.taxId = taxId;
            this.parent = parent;
            this.rank = rank;

            this.children = new ArrayList<>();
            this.labels = new ArrayList<>();
        }

        public void addChild(Node child) {
            children.add(child);
        }

        public void addLabel(String label) {
            labels.add(label);
        }

        public void setParent(Node parent) {
            this.parent = parent;
        }

        public String toString() {
            return "(%s) %s (%d)"
                    .formatted(
                            !Objects.isNull(this.rank) ? this.rank : "no rank",
                            this.labels.size() > 0 ? this.labels.get(0) : "no labels",
                            this.taxId
                    );
        }

        public ArrayList<Node> getChildren() {
            return children;
        }

        public Node getParent() {
            return parent;
        }

        public int getTaxId() {
            return taxId;
        }

        public ArrayList<String> getLabels() {
            return labels;
        }

        public String getRank() {
            return rank;
        }

        public void setRank(String rank) {
            this.rank = rank;
        }
    }

    public static class Tree {
        public final ConcurrentHashMap<Integer, Node> idMap;
        public final ConcurrentHashMap<String, Integer> accessionMap;
        public Tree(ConcurrentHashMap<Integer, Node> idMap, ConcurrentHashMap<String, Integer> accessionMap) {
            this.idMap = idMap;
            this.accessionMap = accessionMap;
        }

        public Node findMRCA(Node node1, Node node2) {
            ArrayList<Node> path1 = pathToRoot(node1);
            ArrayList<Node> path2 = pathToRoot(node2);
            Node mrca = null;
            int difference = path1.size() - path2.size();
            if (difference > 0) {
                for (int i = path2.size() - 1; i >= 0; i--) {
                    if (path2.get(i).equals(path1.get(i + difference))) {
                        mrca = path2.get(i);
                    } else {
                        break;
                    }
                }
            } else {
                for (int i = path1.size() - 1; i >= 0; i--) {
                    if (path1.get(i).equals(path2.get(i - difference))) {
                        mrca = path1.get(i);
                    } else {
                        break;
                    }
                }
            }
            return mrca;
        }

        public int findMRCA(int taxId1, int taxId2) {
            return findMRCA(idMap.get(taxId1), idMap.get(taxId2)).getTaxId();
        }

        public ArrayList<Node> pathToRoot(Node node) {
            ArrayList<Node> path = new ArrayList<>();
            Node prevNode = null;
            while (node != prevNode) {
                path.add(node);
                prevNode = node;
                node = node.getParent();
            }
            return path;
        }
    }
}
