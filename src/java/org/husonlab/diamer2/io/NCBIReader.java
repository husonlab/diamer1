package org.husonlab.diamer2.io;

import org.husonlab.diamer2.graph.Tree;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class NCBIReader {
    public static Tree readTaxonomy(String filename) throws IOException {
        // read the NCBI taxonomy from a nr dump file (nodes.dmp)
        Tree tree = new Tree();
        try (BufferedReader br = Files.newBufferedReader(Paths.get(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split("\t\|\t");
                String taxId = values[0];
                String parentTaxId = values[1];
                String rank = values[2];
                String phylum = taxonomy[1];
                String clazz = taxonomy[2];
                String order = taxonomy[3];
                String family = taxonomy[4];
                String genus = taxonomy[5];
                String species = taxonomy[6];
                tree.addLeaf(domain, phylum, clazz, order, family, genus, species, seqId);
            }
        }
        return tree;
    }

}
