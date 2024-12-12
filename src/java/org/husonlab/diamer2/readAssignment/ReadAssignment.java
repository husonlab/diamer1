package org.husonlab.diamer2.readAssignment;

import org.husonlab.diamer2.taxonomy.Node;
import org.husonlab.diamer2.taxonomy.Tree;

import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

public class ReadAssignment {

    private Read[] reads;
    Tree tree;

    public ReadAssignment(Tree tree, File file) {
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
                        assignments[j] = new Assignment(tree.idMap.get(taxId), count);
                    }
                    assignments = Arrays.stream(assignments).sorted().toArray(Assignment[]::new);
                    reads[i] = new Read(header, assignments, assignments[assignments.length - 1].taxon);
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

    public HashMap<String, Integer> kmerRanks() {
        HashMap<String, Integer> rankCounts = new HashMap<>();
        for(Read read : reads) {
            for(Assignment assignment : read.assignments()) {
                Node taxon = assignment.taxon();
                String rank = taxon.getRank();
                rankCounts.computeIfPresent(rank, (k, v) -> v + assignment.count());
                rankCounts.computeIfAbsent(rank, k -> assignment.count());
            }
        }
        return rankCounts;
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

    @Override
    public String toString() {
        HashMap<String, Integer> kmerRanks = kmerRanks();
        return kmerRanks.entrySet().stream().map(entry -> entry.getKey() + ":\t" + entry.getValue()).collect(Collectors.joining("\n"));
    }

    public record Read(String header, Assignment[] assignments, Node taxon){}

    public record Assignment(Node taxon, int count) implements Comparable<Assignment> {
        @Override
        public int compareTo(Assignment o) {
            return Integer.compare(count, o.count);
        }

    }

    public static void statistics(Tree tree, File file, Path outpath) throws IOException {

        int unclassifiedReads = 0;
        int classifiedReads = 0;
        HashMap<Integer, Integer> taxonKmerCounts = new HashMap<>();    // taxon id -> kmer count
        HashMap<String, Integer> kmerRanks = new HashMap<>();           // rank -> kmer count
        HashMap<String, Integer> rankOfBestMatch = new HashMap<>();     // rank -> read count
        HashMap<String, Integer> bestSpeciesMatches = new HashMap<>();  // species string (assignment with the highest count) -> read count
        HashMap<Integer, Integer> kmersPerGenus = new HashMap<>();      // genus tax id -> kmer count
        HashMap<Integer, Integer> kmersPerSpecies = new HashMap<>();    // species tax id -> kmer count

        try(BufferedReader reader = new BufferedReader(new FileReader(file))) {
            reader.readLine(); // skip the first line with the number of reads
            String line;
            while((line = reader.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length == 2) {
                    classifiedReads++;
                    String header = parts[0];
                    String[] assignmentStrings = parts[1].split(" ");
                    Assignment[] assignments = new Assignment[assignmentStrings.length];
                    for(int i = 0; i < assignmentStrings.length; i++) {
                        String[] assignmentParts = assignmentStrings[i].split(":");
                        int taxId = Integer.parseInt(assignmentParts[0]);
                        int count = Integer.parseInt(assignmentParts[1]);
                        Assignment assignment = new Assignment(tree.idMap.get(taxId), count);
                        assignments[i] = assignment;

                        Node assignmentSpecies = tree.getSpecies(assignment.taxon);
                        Node assignmentGenus = tree.getGenus(assignment.taxon);

                        kmerRanks.computeIfPresent(assignments[i].taxon.getRank(), (k, v) -> v + count);
                        kmerRanks.computeIfAbsent(assignments[i].taxon.getRank(), k -> count);

                        taxonKmerCounts.computeIfPresent(taxId, (k, v) -> v + count);
                        taxonKmerCounts.computeIfAbsent(taxId, k -> count);

                        if (assignmentSpecies != null) {
                            kmersPerSpecies.computeIfPresent(assignmentSpecies.getTaxId(), (k, v) -> v + count);
                            kmersPerSpecies.computeIfAbsent(assignmentSpecies.getTaxId(), k -> count);
                        }
                        if (assignmentGenus != null) {
                            kmersPerGenus.computeIfPresent(assignmentGenus.getTaxId(), (k, v) -> v + count);
                            kmersPerGenus.computeIfAbsent(assignmentGenus.getTaxId(), k -> count);
                        }
                    }
                    assignments = Arrays.stream(assignments).sorted().toArray(Assignment[]::new);

                    rankOfBestMatch.computeIfPresent(assignments[assignments.length - 1].taxon.getRank(), (k, v) -> v + 1);
                    rankOfBestMatch.computeIfAbsent(assignments[assignments.length - 1].taxon.getRank(), k -> 1);

                    for (int i = 0; i < assignmentStrings.length; i++) {
                        if (assignments[i].taxon.getRank().equals("species")) {
                            bestSpeciesMatches.computeIfPresent(assignments[i].taxon.toString(), (k, v) -> v + 1);
                            bestSpeciesMatches.computeIfAbsent(assignments[i].taxon.toString(), k -> 1);
                            break;
                        }
                    }
                } else if (parts.length == 1) {
                    unclassifiedReads++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outpath.resolve("overview.tsv").toFile()))) {
            bw.write("Total reads: " + (unclassifiedReads + classifiedReads) + "\n");
            bw.write("Classified reads: " + classifiedReads + "\n");
            bw.write("Unclassified reads: " + unclassifiedReads + "\n");
            bw.write("Rank Counts\n");
            kmerRanks
                    .entrySet()
                    .stream()
                    .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                    .forEach(e -> {
                        try {
                            bw.write(e.getKey() + ":\t" + e.getValue() + "\n");
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
            bw.write("Rank of best match\n");
            rankOfBestMatch
                    .entrySet()
                    .stream()
                    .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                    .forEach(e -> {
                        try {
                            bw.write(e.getKey() + ":\t" + e.getValue() + "\n");
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    });
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outpath.resolve("taxonKmerCounts.tsv").toFile()))) {
            bw.write("Taxon\tCount\n");
            taxonKmerCounts
                    .entrySet()
                    .stream()
                    .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                    .forEach(e -> {
                        try {
                            bw.write(tree.idMap.get(e.getKey()) + "\t" + e.getValue() + "\n");
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outpath.resolve("bestSpeciesMatches.tsv").toFile()))) {
            bw.write("Species\tCount\n");
            bestSpeciesMatches
                    .entrySet()
                    .stream()
                    .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                    .forEach(e -> {
                        try {
                            bw.write(e.getKey() + "\t" + e.getValue() + "\n");
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outpath.resolve("kmersPerGenus.tsv").toFile()))){
            bw.write("Genus\tCount\n");
            kmersPerGenus
                    .entrySet()
                    .stream()
                    .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                    .forEach(e -> {
                        try {
                            bw.write(tree.idMap.get(e.getKey()) + "\t" + e.getValue() + "\n");
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outpath.resolve("kmersPerSpecies.tsv").toFile()))){
            bw.write("Species\tCount\n");
            kmersPerSpecies
                    .entrySet()
                    .stream()
                    .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                    .forEach(e -> {
                        try {
                            bw.write(tree.idMap.get(e.getKey()) + "\t" + e.getValue() + "\n");
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
        }
    }

}
