package org.husonlab.diamer2.io;

import org.husonlab.diamer2.logging.Logger;
import org.husonlab.diamer2.logging.OneLineLogger;
import org.husonlab.diamer2.logging.ProgressBar;
import org.husonlab.diamer2.readAssignment.AssignmentAlgorithms;
import org.husonlab.diamer2.readAssignment.Read;
import org.husonlab.diamer2.readAssignment.ReadAssignment;
import org.husonlab.diamer2.taxonomy.Tree;

import java.io.*;
import java.nio.file.Path;

public class ReadAssignmentIO {
    public static ReadAssignment read(Tree tree, File readAssignmentFile) {
        Logger logger = new Logger("ReadAssignmentIO");
        logger.logInfo("Reading read assignments from " + readAssignmentFile.getAbsolutePath());
        int lineNumber = 1;
        Read[] readAssignments;
        try (BufferedReader reader = new BufferedReader(new FileReader(readAssignmentFile))) {
            int size = Integer.parseInt(reader.readLine());
            readAssignments = new Read[size];

            ProgressBar progressBar = new ProgressBar(size, 20);
            new OneLineLogger("ReadAssignmentIO", 100).addElement(progressBar);

            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                progressBar.incrementProgress();
                String[] parts = line.split("\t");
                Read read = new Read(parts[0]);
                if (parts.length == 2) {
                    String[] assignmentStrings = parts[1].split(" ");
                    for (String assignmentString : assignmentStrings) {
                        String[] assignmentParts = assignmentString.split(":");
                        int taxId = Integer.parseInt(assignmentParts[0]);
                        int count = Integer.parseInt(assignmentParts[1]);
                        read.addReadAssignment(tree.idMap.get(taxId), count);
                    }
                    readAssignments[lineNumber - 2] = read;
                } else if (parts.length == 1) {
                    readAssignments[lineNumber - 2] = read;
                }
            }
            progressBar.finish();
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid file format, line: " + lineNumber, e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new ReadAssignment(tree, readAssignments);
    }

    public static void writeRawAssignments(ReadAssignment readAssignment, File file) {
        Logger logger = new Logger("ReadAssignmentIO");
        logger.logInfo("Writing raw read assignments to " + file.getAbsolutePath());
        ProgressBar progressBar = new ProgressBar(readAssignment.size(), 20);
        new OneLineLogger("ReadAssignmentIO", 100).addElement(progressBar);
        try (java.io.PrintWriter writer = new java.io.PrintWriter(file)) {
            writer.println(readAssignment.size());
            for (Read read : readAssignment) {
                progressBar.incrementProgress();
                writer.println(read);
            }
            progressBar.finish();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeAssignments(ReadAssignment readAssignment, File file) {
        Logger logger = new Logger("ReadAssignmentIO");
        logger.logInfo("Writing read assignments to " + file.getAbsolutePath());
        ProgressBar progressBar = new ProgressBar(readAssignment.size(), 20);
        new OneLineLogger("ReadAssignmentIO", 100).addElement(progressBar);

        AssignmentAlgorithms assignmentAlgorithms = new AssignmentAlgorithms(readAssignment.getTree());
        try (PrintWriter writer = new PrintWriter(file)) {
            writer.println(readAssignment.size());
            for (Read read : readAssignment) {
                assignmentAlgorithms.OVO(read, 0.5f);
                progressBar.incrementProgress();
                writer.print(read.getHeader());
                writer.print("\t");
                writer.println(read.getAssignedNode());
            }
            progressBar.finish();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeReadStatistics(ReadAssignment readAssignment, Path path) {
        writeReadStatistics(readAssignment.getStatistics(), path);
    }

    public static void writeReadStatistics(ReadAssignment.ReadStatistics readStatistics, Path path) {
        try (PrintWriter writer = new PrintWriter(path.resolve("kmerMatches.tsv").toFile())) {
            writer.println("TaxId\tCount");
            readStatistics.kmerMatches().entrySet().stream()
                    .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                    .forEach(entry -> writer.println(entry.getKey().toString() + "\t" + entry.getValue()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (PrintWriter writer = new PrintWriter(path.resolve("kmerPerSpecies.tsv").toFile())) {
            writer.println("TaxId\tCount");
            readStatistics.kmerPerSpecies().entrySet().stream()
                    .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                    .forEach(entry -> writer.println(entry.getKey().toString() + "\t" + entry.getValue()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (PrintWriter writer = new PrintWriter(path.resolve("kmerPerGenus.tsv").toFile())) {
            writer.println("TaxId\tCount");
            readStatistics.kmerPerGenus().entrySet().stream()
                    .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                    .forEach(entry -> writer.println(entry.getKey().toString() + "\t" + entry.getValue()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
