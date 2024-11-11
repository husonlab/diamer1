package org.husonlab.diamer2.reduceDatasets;

import org.husonlab.diamer2.graph.Node;
import org.husonlab.diamer2.graph.Tree;
import org.husonlab.diamer2.io.NCBIReader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;

public class ReduceTaxonomy {

    private static final String pathNodes = "C:\\Users\\noel\\Documents\\diamer2\\src\\test\\resources\\NCBI\\taxdmp\\nodes.dmp";
    private static final String pathNodeLabels = "C:\\Users\\noel\\Documents\\diamer2\\src\\test\\resources\\NCBI\\taxdmp\\names.dmp";
    private static final String pathAccessionsFull = "C:\\Users\\noel\\Documents\\diamer2\\src\\test\\resources\\NCBI\\reduced\\prot.accession2taxid.FULL100.gz";
    private static final String pathAccessionsDead = "C:\\Users\\noel\\Documents\\diamer2\\src\\test\\resources\\NCBI\\reduced\\dead_prot.accession2taxid100.gz";
    private static final String pathOutput = "F:\\Studium\\Master\\semester5\\thesis\\data\\NCBI\\reduced\\";
    public static void main(String[] args) throws IOException {
        Tree tree = NCBIReader.readTaxonomy(pathNodes);
        NCBIReader.addTaxonomicLabels(tree, pathNodeLabels);
        NCBIReader.addAccessions(tree, pathAccessionsFull, (short) 0, (short) 1);
        NCBIReader.addAccessions(tree, pathAccessionsDead, (short) 1, (short) 2);

        HashSet<Integer> writtenNodes = new HashSet<>();

        try (BufferedWriter bwNodes = Files.newBufferedWriter(Paths.get(pathOutput + "nodes100.dmp"));
             BufferedWriter bwNames = Files.newBufferedWriter(Paths.get(pathOutput + "names100.dmp"))) {
            for (Node node : tree.getAccessionMap().values()) {
                Node parent = node;
                while (parent != null && !writtenNodes.contains(parent.getTaxId()) && parent.getParent() != parent) {
                    bwNodes.write(parent.getTaxId() + "\t|\t" + parent.getParent().getTaxId() + "\t|\t" + parent.getRank());
                    bwNodes.newLine();
                    writtenNodes.add(parent.getTaxId());

                    for (String label : parent.getLabels()) {
                        bwNames.write(parent.getTaxId() + "\t|\t" + label);
                        bwNames.newLine();
                    }

                    parent = parent.getParent();
                }
            }
        }
    }
}
