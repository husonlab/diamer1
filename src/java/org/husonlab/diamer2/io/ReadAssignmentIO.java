package org.husonlab.diamer2.io;

import org.husonlab.diamer2.logging.Logger;
import org.husonlab.diamer2.logging.OneLineLogger;
import org.husonlab.diamer2.logging.ProgressBar;
import org.husonlab.diamer2.logging.Time;
import org.husonlab.diamer2.readAssignment.AssignmentAlgorithm;
import org.husonlab.diamer2.readAssignment.OVO;
import org.husonlab.diamer2.readAssignment.Read;
import org.husonlab.diamer2.readAssignment.ReadAssignment;
import org.husonlab.diamer2.taxonomy.Tree;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;

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

//    public static void writeRawAssignments(ReadAssignment readAssignment, File file) {
//        Logger logger = new Logger("ReadAssignmentIO");
//        logger.logInfo("Writing raw read assignments to " + file.getAbsolutePath());
//        ProgressBar progressBar = new ProgressBar(readAssignment.size(), 20);
//        new OneLineLogger("ReadAssignmentIO", 100).addElement(progressBar);
//        try (java.io.PrintWriter writer = new java.io.PrintWriter(file)) {
//            writer.println(readAssignment.size());
//            for (Read read : readAssignment) {
//                progressBar.incrementProgress();
//                writer.println(read);
//            }
//            progressBar.finish();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }

//    public static void writeAssignments(ReadAssignment readAssignment, File file) {
//        Logger logger = new Logger("ReadAssignmentIO");
//        logger.logInfo("Writing read assignments to " + file.getAbsolutePath());
//        ProgressBar progressBar = new ProgressBar(readAssignment.size(), 20);
//        new OneLineLogger("ReadAssignmentIO", 100).addElement(progressBar);
//
//        OVO ovo = new OVO(readAssignment.getTree(), 0.5f);
//
//        try (PrintWriter writer = new PrintWriter(file)) {
//            writer.println(readAssignment.size());
//            for (Read read : readAssignment) {
//                ovo.assignRead(read.getAssociations());
//                progressBar.incrementProgress();
//                writer.print(read.getHeader());
//                writer.print("\t");
//                writer.println(read.getAssignedNode());
//            }
//            progressBar.finish();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }

//    public static void writeReadStatistics(ReadAssignment readAssignment, Path path) {
//        writeReadStatistics(readAssignment.getStatistics(), path);
//    }

//    public static void writeReadStatistics(ReadAssignment.ReadStatistics readStatistics, Path path) {
//        try (PrintWriter writer = new PrintWriter(path.resolve("taxonWeight.tsv").toFile())) {
//            writer.println("TaxId\tCount");
//            readStatistics.kmerMatches().entrySet().stream()
//                    .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
//                    .forEach(entry -> writer.println(entry.getKey().toString() + "\t" + entry.getValue()));
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        try (PrintWriter writer = new PrintWriter(path.resolve("kmerPerSpecies.tsv").toFile())) {
//            writer.println("TaxId\tCount");
//            readStatistics.kmerPerSpecies().entrySet().stream()
//                    .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
//                    .forEach(entry -> writer.println(entry.getKey().toString() + "\t" + entry.getValue()));
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        try (PrintWriter writer = new PrintWriter(path.resolve("kmerPerGenus.tsv").toFile())) {
//            writer.println("TaxId\tCount");
//            readStatistics.kmerPerGenus().entrySet().stream()
//                    .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
//                    .forEach(entry -> writer.println(entry.getKey().toString() + "\t" + entry.getValue()));
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
}
