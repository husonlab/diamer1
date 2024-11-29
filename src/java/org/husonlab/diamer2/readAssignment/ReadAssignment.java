package org.husonlab.diamer2.readAssignment;

import org.husonlab.diamer2.graph.Node;
import org.husonlab.diamer2.graph.Tree;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

public class ReadAssignment {

    private Read[] reads;
    Tree tree;

    public ReadAssignment(Tree tree, File file) throws IOException {
        this.tree = tree;
        try(BufferedReader reader = new BufferedReader(new FileReader(file))) {
            reads = new Read[Integer.parseInt(reader.readLine())];
            String line;
            int i = 0;
            while((line = reader.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length == 2) {
                    String header = parts[0];
                    String[] assignmentStrings = parts[1].split(" ");
                    Assignment[] assignments = new Assignment[assignmentStrings.length];
                    for(int j = 0; j < assignmentStrings.length; j++) {
                        String[] assignmentParts = assignmentStrings[j].split(":");
                        int taxId = Integer.parseInt(assignmentParts[0]);
                        int count = Integer.parseInt(assignmentParts[1]);
                        assignments[j] = new Assignment(taxId, count);
                    }
                    assignments = Arrays.stream(assignments).sorted().toArray(Assignment[]::new);
                    reads[i] = new Read(header, assignments, tree.idMap.get(assignments[assignments.length - 1].taxId()));
                } else if (parts.length == 1) {
                    String header = parts[0];
                    reads[i] = new Read(header, new Assignment[0], new Node(-1, "unclassified"));
                } else {
                    continue;
                }
                i++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void printStatistics() {
        int totalReads = reads.length;
        int readsWithAssignments = (int) Arrays.stream(reads).filter(read -> read.assignments().length > 0).count();
        int readsWithoutAssignments = totalReads - readsWithAssignments;
        int totalAssignments = Arrays.stream(reads).mapToInt(read -> read.assignments().length).sum();
        int uniqueTaxIds = Arrays.stream(reads).flatMap(read -> Arrays.stream(read.assignments())).mapToInt(assignment -> assignment.taxId()).distinct().toArray().length;
        int totalTaxIds = Arrays.stream(reads).flatMap(read -> Arrays.stream(read.assignments())).mapToInt(assignment -> assignment.taxId()).sum();
        int maxAssignments = Arrays.stream(reads).mapToInt(read -> read.assignments().length).max().orElse(0);
        int minAssignments = Arrays.stream(reads).mapToInt(read -> read.assignments().length).min().orElse(0);
        double avgAssignments = Arrays.stream(reads).mapToInt(read -> read.assignments().length).average().orElse(0);
        System.out.println("Total reads: " + totalReads);
        System.out.println("Reads with assignments: " + readsWithAssignments);
        System.out.println("Reads without assignments: " + readsWithoutAssignments);
        System.out.println("Total assignments: " + totalAssignments);
        System.out.println("Unique tax ids: " + uniqueTaxIds);
        System.out.println("Total tax ids: " + totalTaxIds);
        System.out.println("Max assignments: " + maxAssignments);
        System.out.println("Min assignments: " + minAssignments);
        System.out.println("Avg assignments: " + avgAssignments);
    }

    public void printTopAssignments(int n) {
        // count for each Node how many reads are assigned to it
        HashMap<Node, Integer> nodeCounts = new HashMap<>();
        for(Read read : reads) {
            nodeCounts.computeIfPresent(read.taxon(), (k, v) -> v + 1);
            nodeCounts.computeIfAbsent(read.taxon(), k -> 1);
        }
        // sort the nodes by the number of reads assigned to them
        Node[] sortedNodes = nodeCounts.keySet().stream().sorted((n1, n2) -> Integer.compare(nodeCounts.get(n2), nodeCounts.get(n1))).toArray(Node[]::new);
        // print the top n nodes
        for(int i = 0; i < n && i < sortedNodes.length; i++) {
            Node node = sortedNodes[i];
            int count = nodeCounts.get(node);
            System.out.println("Node: " + node + " Count: " + count);
        }
    }

    public void printTopKmerAssignments(int n) {
        // count for each Node how many kmers are assigned to it
        HashMap<Node, Integer> nodeCounts = new HashMap<>();
        for(Read read : reads) {
            for(Assignment assignment : read.assignments()) {
                Node taxon = tree.idMap.get(assignment.taxId());
                nodeCounts.computeIfPresent(taxon, (k, v) -> v + assignment.count());
                nodeCounts.computeIfAbsent(taxon, k -> assignment.count());
            }
        }
        // sort the nodes by the number of kmers assigned to them
        Node[] sortedNodes = nodeCounts.keySet().stream().sorted((n1, n2) -> Integer.compare(nodeCounts.get(n2), nodeCounts.get(n1))).toArray(Node[]::new);
        // print the top n nodes
        for(int i = 0; i < n && i < sortedNodes.length; i++) {
            Node node = sortedNodes[i];
            int count = nodeCounts.get(node);
            System.out.println("Node: " + node + " Count: " + count);
        }
    }

    public record Read(String header, Assignment[] assignments, Node taxon){}

    public record Assignment(int taxId, int count) implements Comparable<Assignment> {
        @Override
        public int compareTo(Assignment o) {
            return Integer.compare(count, o.count);
        }

    }

}
