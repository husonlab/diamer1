package org.husonlab.diamer2.io;

import org.husonlab.diamer2.graph.Node;
import org.husonlab.diamer2.graph.Tree;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.zip.GZIPInputStream;
import java.util.concurrent.TimeUnit;

public class NCBIReader {
    private static ThreadPoolExecutor threadPoolExecutor;
    private static ConcurrentHashMap<Integer, Node> idMap;
    private static ConcurrentHashMap<String, Node> accessionMap;

    public NCBIReader() {
        idMap = new ConcurrentHashMap<>(3500000);
        accessionMap = new ConcurrentHashMap<>(4000000);
        threadPoolExecutor = new ThreadPoolExecutor(
                12,
                12,
                500L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(100),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    public Tree readTaxonomy(String nodesDumpfile, String namesDumpfile, String fullAccessionMapping, String deadAccessionMapping) throws IOException {
        /**
         * Parses the NCBI taxonomy from the following files:
         * @param nodesDumpfile: nodes.dmp, containing the taxonomy nodes (format: tax_id | parent_tax_id | rank)
         * @param namesDumpfile: names.dmp, containing the taxonomy names (format: tax_id | name)
         * @param fullAccessionMapping: prot.accession2taxidFULL.gz, containing the mapping of protein accessions to tax_ids (format: accession.version | tax_id)
         * @param deadAccessionMapping: dead_prot.accession2taxid.gz, containing the mapping of dead protein accessions to tax_ids (format: accession | accession.version | tax_id)
         */
        System.out.println("Reading nodes dumpfile...");
        readNodesDumpfile(nodesDumpfile);
        System.out.println("Reading names dumpfile...");
        readNamesDumpfile(namesDumpfile);
        System.out.println("Reading full accession mapping...");
        readAccessionMap(fullAccessionMapping, 1, 2);

        threadPoolExecutor.shutdown();
        return null;
    }

    private void readNodesDumpfile(String nodesDumpfile) throws IOException {
        ConcurrentHashMap<Integer, Integer> parentMap = new ConcurrentHashMap<>();
        try (BufferedReader br = Files.newBufferedReader(Paths.get(nodesDumpfile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split("\t\\|\t");
                int taxId = Integer.parseInt(values[0]);
                int parentTaxId = Integer.parseInt(values[1]);
                String rank = values[2];
                Node node = new Node(taxId, new ArrayList<>(), rank);
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

    public void readNamesDumpfile(String namesDumpfile) throws IOException {
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

    // no multithreading:
    // 4769646500
    // 4722979800
    // 4422426700
    // multithreading
    // 8456561300
    // 8607339500
    // 7978696100

    public void readAccessionMap(String accessionMapFile, int accessionCol, int taxIdCol) throws IOException {
        try (FileInputStream fis = new FileInputStream(accessionMapFile);
             GZIPInputStream gis = new GZIPInputStream(fis);
             BufferedReader br = new BufferedReader(new InputStreamReader(gis))) {
            String line;
            br.readLine(); // skip header
            long start = System.nanoTime();
            while ((line = br.readLine()) != null) {
                String finalLine = line;
                threadPoolExecutor.submit(() -> {
                    String[] values = finalLine.split("\t");
                    String accession = values[accessionCol];
                    int taxId = Integer.parseInt(values[taxIdCol]);
                    if (idMap.containsKey(taxId) && !accessionMap.containsKey(accession)) {
                        Node node = idMap.get(taxId);
                        accessionMap.put(accession, node);
                    } else if (idMap.containsKey(taxId)) {
                        System.err.printf("Accession %s already exists in the tree with a different node. %n\texisting: %s%n\tnew: %s%n", accession, accessionMap.get(accession), idMap.get(taxId));
                    }
                });
            }
            long end = System.nanoTime();
            System.out.println("Reading accession map took " + (end - start));
        }
    }

    public static Tree readTaxonomy(String filename) throws IOException {
        // read the NCBI taxonomy from a nr dump file (nodes.dmp)
        Tree tree = new Tree();
        HashMap<Integer, Integer> parentMap = new HashMap<>(3500000);
        try (BufferedReader br = Files.newBufferedReader(Paths.get(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split("\t\\|\t");
                int taxId = Integer.parseInt(values[0]);
                int parentTaxId = Integer.parseInt(values[1]);
                String rank = values[2];
                Node node = new Node(tree, taxId, new ArrayList<>(), rank);
                parentMap.put(taxId, parentTaxId);
            }
        }
        for (int taxId : parentMap.keySet()) {
            Node node = tree.byId(taxId);
            Node parent = tree.byId(parentMap.get(taxId));
            node.setParent(parent);
            parent.addChild(node);
        }
        return tree;
    }

    public static Tree addTaxonomicLabels(Tree tree, String filename) throws IOException {
        // read the NCBI taxonomy from a nr dump file (names.dmp)
        try (BufferedReader br = Files.newBufferedReader(Paths.get(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split("\t\\|\t");
                int taxId = Integer.parseInt(values[0]);
                String label = values[1];
                Node node = tree.byId(taxId);
                node.addLabel(label);
            }
        }
        return tree;
    }

    public static Tree addAccessions(Tree tree, String filename, short accessionCol, short taxIdCol) throws IOException {
        // add sequence accessions from the NCBI prot.accession2taxid.gz file
        try (FileInputStream fis = new FileInputStream(filename);
             GZIPInputStream gis = new GZIPInputStream(fis);
             BufferedReader br = new BufferedReader(new InputStreamReader(gis))) {
            String line;
            br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                String[] values = line.split("\t");
                String accession = values[accessionCol];
                int taxId = Integer.parseInt(values[taxIdCol]);
                if (tree.byId(taxId) != null) {
                    Node node = tree.byId(taxId);
                    node.addAccession(accession);
                }
            }
        }
        return tree;
    }

    public ConcurrentHashMap<Integer, Node> getIdMap() {
        return idMap;
    }
}
