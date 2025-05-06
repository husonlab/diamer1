package org.husonlab.diamer2.io;

import org.husonlab.diamer2.main.GlobalSettings;
import org.husonlab.diamer2.taxonomy.Node;
import org.husonlab.diamer2.util.logging.Logger;
import org.husonlab.diamer2.util.logging.OneLineLogger;
import org.husonlab.diamer2.util.logging.ProgressBar;
import org.husonlab.diamer2.util.logging.Time;
import org.husonlab.diamer2.readAssignment.*;
import org.husonlab.diamer2.taxonomy.Tree;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class ReadAssignmentIO {

    /**
     * Reads a raw read assignment from a file.
     * @param tree The taxonomy tree to associate with the read assignment
     * @param readAssignmentFile The file to read the assignment from
     * @return A {@link ReadAssignment} object
     */
    public static ReadAssignment readRawAssignment(Tree tree, Path readAssignmentFile, GlobalSettings settings) {
        Logger logger = new Logger("ReadAssignmentIO");
        logger.addElement(new Time());
        logger.logInfo("Reading read assignments from " + readAssignmentFile);
        String[] readHeaderMapping;
        ArrayList<ReadAssignment.KmerCount<Integer>>[] kmerMatches;
        HashSet<Integer> missingTaxIds = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(readAssignmentFile.toString()))) {
            int size;
            try {
                size = Integer.parseInt(reader.readLine());
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid Assignment file format: missing file length. " + readAssignmentFile);
            }
            readHeaderMapping = new String[size];
            kmerMatches = new ArrayList[size];
            for (int i = 0; i < size; i++) {
                kmerMatches[i] = new ArrayList<>();
            }

            ProgressBar progressBar = new ProgressBar(size, 20);
            new OneLineLogger("ReadAssignmentIO", 500).addElement(progressBar);

            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                progressBar.incrementProgress();
                String[] read = line.split("\t");
                if (read.length == 2) {
                    String[] assignmentStrings = read[1].split(" ");
                    for (String assignmentString : assignmentStrings) {
                        String[] assignmentParts = assignmentString.split(":");
                        int taxId = Integer.parseInt(assignmentParts[0]);
                        int count = Integer.parseInt(assignmentParts[1]);
                        if (tree.hasNode(taxId)) {
                            kmerMatches[lineNumber].add(new ReadAssignment.KmerCount<>(taxId, count));
                        } else {
                            missingTaxIds.add(taxId);
                        }
                    }
                }
                readHeaderMapping[lineNumber] = read[0];
                lineNumber++;
            }
            progressBar.finish();
        } catch (IOException e) {
            throw new RuntimeException("Error parsing read assignment file: " + readAssignmentFile, e);
        }
        if (!missingTaxIds.isEmpty()) logger.logWarning("Could not find " + missingTaxIds.size() + " tax IDs in the tree.");
        return new ReadAssignment(tree, readHeaderMapping, kmerMatches, settings);
    }

    public static ReadAssignment readRawKrakenAssignment(Tree tree, Path readAssignmentFile, GlobalSettings settings) {
        Logger logger = new Logger("ReadAssignmentIO");
        logger.addElement(new Time());
        logger.logInfo("Reading read assignments from " + readAssignmentFile);
        ArrayList<String> readHeaderMapping = new ArrayList<>();
        ArrayList<ArrayList<ReadAssignment.KmerCount<Integer>>> kmerCounts = new ArrayList<>();
        ArrayList<ArrayList<Integer>> taxonAssignments = new ArrayList<>();
        HashSet<Integer> missingTaxIds = new HashSet<>();
        try (CountingInputStream cis = new CountingInputStream(new FileInputStream(readAssignmentFile.toString()));
                BufferedReader reader = new BufferedReader(new InputStreamReader(cis))) {
            ProgressBar progressBar = new ProgressBar(readAssignmentFile.toFile().length(), 20);
            new OneLineLogger("ReadAssignmentIO", 500).addElement(progressBar);

            String line;
            while ((line = reader.readLine()) != null) {
                progressBar.setProgress(cis.getBytesRead());
                String[] read = line.split("\t");
                readHeaderMapping.add(read[1]);
                ArrayList<ReadAssignment.KmerCount<Integer>> kmerMatch = new ArrayList<>();
                ArrayList<Integer> taxonAssignment = new ArrayList<>();
                if (read.length == 5) {
                    String[] assignmentStrings = read[4].split(" ");
                    int assignment = Integer.parseInt(read[2]);
                    taxonAssignment.add(assignment == 0 || !tree.hasNode(assignment) ? -1 : assignment);
                    for (String assignmentString : assignmentStrings) {
                        String[] assignmentParts = assignmentString.split(":");
                        try {
                            int taxId = Integer.parseInt(assignmentParts[0]);
                            int count = Integer.parseInt(assignmentParts[1]);
                            if (tree.hasNode(taxId)) {
                                kmerMatch.add(new ReadAssignment.KmerCount<>(taxId, count));
                            } else {
                                missingTaxIds.add(taxId);
                            }
                        } catch (NumberFormatException _) {}
                    }
                    kmerCounts.add(kmerMatch);
                    taxonAssignments.add(taxonAssignment);
                }
            }
            if (!missingTaxIds.isEmpty()) logger.logWarning("Could not find " + missingTaxIds.size() + " tax IDs in the tree.");
            progressBar.finish();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new ReadAssignment(tree, readHeaderMapping.toArray(new String[0]), kmerCounts.toArray(new ArrayList[0]), new ArrayList<>(List.of("kraken2")), taxonAssignments.toArray(new ArrayList[0]), settings);
    }

    /**
     * Writes the raw read assignment to a file.
     * @param readAssignment The read assignment to write
     * @param file The file to write the assignment to
     */
    public static void writeRawAssignment(ReadAssignment readAssignment, Path file) {
        Logger logger = new Logger("ReadAssignmentIO");
        logger.addElement(new Time());
        logger.logInfo("Writing raw read assignments to " + file);
        ProgressBar progressBar = new ProgressBar(readAssignment.size(), 20);
        new OneLineLogger("ReadAssignmentIO", 100).addElement(progressBar);

        readAssignment.sortKmerCounts();

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file.toString()))) {
            bw.write(readAssignment.size() + "\n");
            for (int i = 0; i < readAssignment.size(); i++) {
                progressBar.incrementProgress();
                bw.write(readAssignment.getReadHeader(i) + "\t");
                for (ReadAssignment.KmerCount<Integer> kmerCount : readAssignment.getKmerCounts(i)) {
                    bw.write(kmerCount.getTaxId() + ":" + kmerCount.getCount() + " ");
                }
                bw.newLine();
            }
            progressBar.finish();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Writes the final assignment(s) per read.
     * <p>Format:</p>
     * <pre>
     *     Header
     *     ReadID/ReadHeader \t Assignment1 \t Assignment2  ...
     * </pre>
     * @param readAssignment The read assignment to write
     * @param file The name of the result assignment
     * @param headers Whether to write the read headers or just the read IDs
     * @param taxonNames Whether to write the taxon names or just the taxon IDs
     */
    public static String writePerReadAssignments(ReadAssignment readAssignment, Path file, boolean headers, boolean taxonNames, GlobalSettings settings) {
        Tree tree = readAssignment.getTree();
        int nrOfAssignmentAlgorithms = readAssignment.getAssignmentAlgorithms().size();
        int nrOfReads = readAssignment.size();

        Logger logger = new Logger("ReadAssignmentIO").addElement(new Time());
        logger.logInfo("Writing read assignments to " + file);
        ProgressBar progressBar = new ProgressBar(nrOfReads, 20);
        new OneLineLogger("ReadAssignmentIO", 500).addElement(progressBar);

        // Initialize variables to collect statistics
        HashMap<String, Integer>[] assignmentsPerRank = new HashMap[nrOfAssignmentAlgorithms];
        int[] assignedReads = new int[nrOfAssignmentAlgorithms];
        for (int i = 0; i < nrOfAssignmentAlgorithms; i++) {
            assignmentsPerRank[i] = new HashMap<>();
        }

        // write assignments
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file.toString()))) {
            bw.write(nrOfReads + "\n");
            bw.write(headers ? "Read" : "ReadID");
            for (String algorithm : readAssignment.getAssignmentAlgorithms()) {
                bw.write("\t" + algorithm);
            }
            bw.newLine();
            for (int i = 0; i < nrOfReads; i++) {
                progressBar.incrementProgress();
                bw.write(headers ? readAssignment.getReadHeader(i) : Integer.toString(i));
                for (int j = 0; j < nrOfAssignmentAlgorithms; j++) {
                    int taxId = readAssignment.getTaxonAssignments(i).get(j);
                    if (taxId != -1) {
                        assignedReads[j]++;
                        String taxName = Integer.toString(taxId);
                        if (tree.idMap.containsKey(taxId)) {
                            Node node = tree.idMap.get(taxId);
                            String rank = node.getRank();
                            assignmentsPerRank[j].put(rank, assignmentsPerRank[j].getOrDefault(rank, 0) + 1);
                            if (taxonNames) {
                                taxName = node.toString();
                            }
                        }
                        bw.write("\t" + taxName);
                    } else {
                        bw.write("\t");
                    }
                }
                bw.newLine();
            }
            progressBar.finish();
        } catch (IOException e) {
            throw new RuntimeException("Error writing read assignment to: " + file, e);
        }

        StringBuilder info = new StringBuilder();

        for (int i = 0; i < nrOfAssignmentAlgorithms; i++) {
            info.append(readAssignment.getAssignmentAlgorithms().get(i)).append("\n");
            info.append("Total assignments: ").append(assignedReads[i])
                    .append(" (").append("%.2f".formatted(assignedReads[i] / (float) nrOfReads * 100)).append("%)\n");
            for (String rank : assignmentsPerRank[i].keySet()) {
                info.append(rank).append(": ").append(assignmentsPerRank[i].get(rank)).append("\n");
            }
            info.append("\n");
        }
        return info.toString();
    }
}
