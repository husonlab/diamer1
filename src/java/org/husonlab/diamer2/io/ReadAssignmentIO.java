package org.husonlab.diamer2.io;

import org.husonlab.diamer2.readAssignment.algorithms.AssignmentAlgorithm;
import org.husonlab.diamer2.taxonomy.Node;
import org.husonlab.diamer2.util.logging.Logger;
import org.husonlab.diamer2.util.logging.OneLineLogger;
import org.husonlab.diamer2.util.logging.ProgressBar;
import org.husonlab.diamer2.util.logging.Time;
import org.husonlab.diamer2.readAssignment.*;
import org.husonlab.diamer2.taxonomy.Tree;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;

public class ReadAssignmentIO {

    /**
     * Reads a raw read assignment from a file.
     * @param tree The taxonomy tree to associate with the read assignment
     * @param readAssignmentFile The file to read the assignment from
     * @return A {@link ReadAssignment} object
     */
    public static ReadAssignment readRawAssignment(Tree tree, Path readAssignmentFile) {
        Logger logger = new Logger("ReadAssignmentIO");
        logger.addElement(new Time());
        logger.logInfo("Reading read assignments from " + readAssignmentFile);
        String[] readHeaderMapping;
        ArrayList<int[]>[] kmerMatches;
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
                        kmerMatches[lineNumber].add(new int[]{taxId, count});
                    }
                }
                readHeaderMapping[lineNumber] = read[0];
                lineNumber++;
            }
            progressBar.finish();
        } catch (IOException e) {
            throw new RuntimeException("Error parsing read assignment file: " + readAssignmentFile, e);
        }
        return new ReadAssignment(tree, readHeaderMapping, kmerMatches);
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

        readAssignment.sortKmerMatches();

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file.toString()))) {
            bw.write(readAssignment.size() + "\n");
            for (int i = 0; i < readAssignment.size(); i++) {
                progressBar.incrementProgress();
                bw.write(readAssignment.getReadHeader(i) + "\t");
                for (int[] assignment : readAssignment.getKmerMatches(i)) {
                    bw.write(assignment[0] + ":" + assignment[1] + " ");
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
    public static void writePerReadAssignments(ReadAssignment readAssignment, Path file, boolean headers, boolean taxonNames) {
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
            for (AssignmentAlgorithm algorithm : readAssignment.getAssignmentAlgorithms()) {
                bw.write("\t" + algorithm.getName());
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

        // write statistics
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file.getParent().resolve("summary.txt").toString()))) {
            bw.write("Total reads: " + nrOfReads + "\n\n");
            for (int i = 0; i < nrOfAssignmentAlgorithms; i++) {
                bw.write(readAssignment.getAssignmentAlgorithms().get(i).getName() + "\n");
                bw.write("Total assignments:\t%d (%.1f%%)\n".formatted(assignedReads[i], assignedReads[i] / (float) nrOfReads));
                for (String rank : assignmentsPerRank[i].keySet()) {
                    bw.write(rank + ": " + assignmentsPerRank[i].get(rank) + "\n");
                }
                bw.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error writing assignment summary file: " + file.getParent().resolve("summary.txt"), e);
        }
    }

    /**
     * Write the custom values associated with each taxon.
     * @param readAssignment the read assignment containing the tree to use
     * @param file the output file
     * @param threshold the minimum value that has to be contained in the custom values for a node to be written
     * @param taxonNames whether to write the taxon names or just the taxon IDs
     */
    public static void writePerTaxonAssignments(ReadAssignment readAssignment, Path file, int threshold, boolean taxonNames) {
        Tree tree = readAssignment.getTree();
        int nrOfAssignments = tree.getNodeCustomValueDescriptors().size();
        HashMap<String, Integer>[] assignmentsPerRank = new HashMap[readAssignment.getAssignmentAlgorithms().size()];

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file.toString()))) {
            bw.write(taxonNames ? "Taxon" : "TaxonID");
            for (int i = 0; i < nrOfAssignments; i++) {
                bw.write("\t" + tree.getNodeCustomValueDescriptors().get(i));
            }
            bw.newLine();
            for (Node node: tree.idMap.values()) {
                long maxAssignment = 0;
                StringBuilder sb = new StringBuilder(taxonNames ? node.toString() : Integer.toString(node.getTaxId()));
                for (int i = 0; i < nrOfAssignments; i++) {
                    maxAssignment = Math.max(maxAssignment, node.customValues.get(i));
                    sb.append("\t").append(node.customValues.get(i));
                }
                if (maxAssignment >= threshold) {
                    bw.write(sb.toString());
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Write one custom value associated with each taxon in a format that can be imported into MEGAN.
     * @param readAssignment the read assignment containing the tree to use
     * @param file the output file
     * @param threshold the minimum value for the custom value of the node to be contained in the output
     * @param customValueIndex the index of the custom value to write
     */
    public static void writeForMEGANImport(ReadAssignment readAssignment, Path file, int threshold, int customValueIndex) {
        Tree tree = readAssignment.getTree();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file.toString()))) {
            bw.write("taxid\t" + tree.getNodeCustomValueDescriptors().get(customValueIndex) + "\n");
            for (Node node: tree.idMap.values()) {
                if (node.customValues.get(customValueIndex) >= threshold) {
                    bw.write(node.getTaxId() + "\t" + node.customValues.get(customValueIndex) + "\n");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
