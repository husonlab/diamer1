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
import java.util.zip.GZIPInputStream;

public class NCBIReader {
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

}
