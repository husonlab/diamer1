package org.husonlab.diamer2.io;

import org.husonlab.diamer2.util.logging.Logger;
import org.husonlab.diamer2.util.logging.OneLineLogger;
import org.husonlab.diamer2.util.logging.ProgressBar;
import org.husonlab.diamer2.util.logging.Time;
import org.husonlab.diamer2.readAssignment.*;
import org.husonlab.diamer2.taxonomy.Tree;
import org.husonlab.diamer2.taxonomy.Node;
import static org.husonlab.diamer2.io.Utilities.createPath;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class ReadAssignmentIO {
    public static ReadAssignment read(Tree tree, File readAssignmentFile) {
        Logger logger = new Logger("ReadAssignmentIO");
        logger.addElement(new Time());
        logger.logInfo("Reading read assignments from " + readAssignmentFile.getAbsolutePath());
        String[] readHeaderMapping;
        ArrayList<int[]>[] kmerMatches;
        ReadAssignment readAssignment;
        try (BufferedReader reader = new BufferedReader(new FileReader(readAssignmentFile))) {
            int size;
            try {
                size = Integer.parseInt(reader.readLine());
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid Assignment file format " + readAssignmentFile);
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
            throw new RuntimeException(e);
        }
        return new ReadAssignment(tree, readHeaderMapping, kmerMatches);
    }

    public static void writeAssignmentStatistics(AssignmentStatistics assignmentStatistics, Path path) {
        Logger logger = new Logger("ReadAssignmentIO");
        logger.addElement(new Time());

        Tree tree = assignmentStatistics.tree();

        logger.logInfo("Writing kmer assignment statistics to " + path.toAbsolutePath());
        for (Tree.AccumulatedWeightsPerRank rankStatistics : assignmentStatistics.kmerStatistics()) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(createPath(path.resolve("kmerStatistics")).resolve(rankStatistics.rank().replace(" ", "_").replace(":", "") + ".tsv").toFile()))) {
                int[][] taxonWeights = rankStatistics.taxonWeights();
                Node node;
                for (int i = 0; i < taxonWeights.length; i++) {
                    if ((node = tree.idMap.get(taxonWeights[i][0])) != null) {
                        bw.write(node + "\t" + taxonWeights[i][1] + "\n");
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


        try (BufferedWriter bw = new BufferedWriter(new FileWriter(createPath(path.resolve("algorithmStatistics")).resolve("overview.txt").toFile()))) {
            for (AssignmentStatistics.PerAlgorithmStatistics perAlgorithmStatistics : assignmentStatistics.perAlgorithmStatistics()) {
                bw.write("Algorithm: " + perAlgorithmStatistics.algorithmName() + "\n");
                bw.write("Assigned reads: " + perAlgorithmStatistics.assignedReads() + "\n");
                bw.write("Unassigned reads: " + perAlgorithmStatistics.unassignedReads() + "\n");
                bw.write("Assignments per rank:\n");
                Arrays.stream(perAlgorithmStatistics.kumulativeAssignmentsPerRank()).sorted(Comparator.comparingInt(Tree.AccumulatedWeightsPerRank::totalWeight).reversed()).forEach(rankStatistics -> {
                    try {
                        bw.write(rankStatistics.rank() + "\t" + rankStatistics.totalWeight() + "\n");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                bw.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (AssignmentStatistics.PerAlgorithmStatistics algorithmStatistics : assignmentStatistics.perAlgorithmStatistics()) {
            for (Tree.AccumulatedWeightsPerRank rankStatistics : algorithmStatistics.kumulativeAssignmentsPerRank()) {
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(createPath(path.resolve("algorithmStatistics", algorithmStatistics.algorithmName().replace(":", ""))).resolve(rankStatistics.rank() + ".tsv").toFile()))) {
                    final int[][] taxonWeights = rankStatistics.taxonWeights();
                    Node node;
                    for (int[] taxonWeight : taxonWeights) {
                        if ((node = tree.idMap.get(taxonWeight[0])) != null) {
                            bw.write(node + "\t" + taxonWeight[1] + "\n");
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static void writeRawAssignments(ReadAssignment readAssignment, File file) {
        Logger logger = new Logger("ReadAssignmentIO");
        logger.addElement(new Time());
        logger.logInfo("Writing raw read assignments to " + file.getAbsolutePath());
        ProgressBar progressBar = new ProgressBar(readAssignment.size(), 20);
        new OneLineLogger("ReadAssignmentIO", 100).addElement(progressBar);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
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
}
