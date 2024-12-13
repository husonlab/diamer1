package org.husonlab.diamer2.readAssignment;

import org.husonlab.diamer2.logging.Logger;
import org.husonlab.diamer2.logging.OneLineLogger;
import org.husonlab.diamer2.logging.ProgressBar;
import org.husonlab.diamer2.taxonomy.Node;
import org.husonlab.diamer2.taxonomy.Tree;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class ReadAssignment extends ArrayList<ReadAssignment.Read> {

    private final Logger logger;
    private final Tree tree;
    private final int size;

    public ReadAssignment(Tree tree, int size, HashMap<Integer, String> readHeaderMapping) {
        super(size);
        this.size = size;
        this.logger = new Logger("ReadAssignment");
        this.tree = tree;
        readHeaderMapping.forEach((readId, header) -> this.add(readId, new Read(header)));
    }

    public ReadAssignment(Tree tree, File readAssignmentFile) {
        this.logger = new Logger("ReadAssignment");
        this.tree = tree;
        logger.logInfo("Reading read assignments from " + readAssignmentFile.getAbsolutePath());
        int lineNumber = 1;
        try(BufferedReader reader = new BufferedReader(new FileReader(readAssignmentFile))) {
            this.size = Integer.parseInt(reader.readLine());
            super.ensureCapacity(size);

            ProgressBar progressBar = new ProgressBar(size, 20);
            new OneLineLogger("ReadAssignment", 100).addElement(progressBar);

            String line;
            while((line = reader.readLine()) != null) {
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
                } else if (parts.length == 1) {
                    this.add(read);
                }
            }
            progressBar.finish();
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid file format, line: " + lineNumber, e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int size() {
        return this.size;
    }

    public void addReadAssignment(int readId, int taxId) {
        this.get(readId).addReadAssignment(tree.idMap.get(taxId));
    }

    public void writeAssignments(File file) {
        logger.logInfo("Writing read assignments to " + file.getAbsolutePath());
        ProgressBar progressBar = new ProgressBar(this.size(), 20);
        new OneLineLogger("ReadAssignment", 100).addElement(progressBar);
        try (java.io.PrintWriter writer = new java.io.PrintWriter(file)) {
            writer.println(this.size());
            for (Read read : this) {
                progressBar.incrementProgress();
                writer.println(read);
            }
            progressBar.finish();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeStatistics(Path path) {
        logger.logInfo("Calculating read statistics ...");
        // Hashmap that counts the number of found kmers that match a specific taxon
        HashMap<Node, Integer> kmerMatches = new HashMap<>();
        // Hashmap that counts the number of kmers that match a specific species
        HashMap<Node, Integer> kmerPerSpecies = new HashMap<>();
        // Hashmap that counts the number of kmers that match a specific genus
        HashMap<Node, Integer> kmerPerGenus = new HashMap<>();

        this.forEach(read -> {
            read.getAssociations().forEach(association -> {
                kmerMatches.put(
                        association.getNode(),
                        kmerMatches.getOrDefault(association.getNode(), 0) + association.getCount()
                );
                Node species = tree.getSpecies(association.getNode());
                if(species != null) {
                    kmerPerSpecies.put(
                            species,
                            kmerPerSpecies.getOrDefault(species, 0) + association.getCount()
                    );
                }
                Node genus = tree.getGenus(association.getNode());
                if(genus != null) {
                    kmerPerGenus.put(
                            genus,
                            kmerPerGenus.getOrDefault(genus, 0) + association.getCount()
                    );
                }
            });
        });

        logger.logInfo("Writing read statistics to " + path);
        try (PrintWriter writer = new PrintWriter(path.resolve("kmerMatches.tsv").toFile())) {
            writer.println("TaxId\tCount");
            kmerMatches.entrySet().stream()
                    .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                    .forEach(entry -> writer.println(entry.getKey().getTaxId() + "\t" + entry.getValue()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (PrintWriter writer = new PrintWriter(path.resolve("kmerPerSpecies.tsv").toFile())) {
            writer.println("TaxId\tCount");
            kmerPerSpecies.entrySet().stream()
                    .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                    .forEach(entry -> writer.println(entry.getKey().getTaxId() + "\t" + entry.getValue()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (PrintWriter writer = new PrintWriter(path.resolve("kmerPerGenus.tsv").toFile())) {
            writer.println("TaxId\tCount");
            kmerPerGenus.entrySet().stream()
                    .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                    .forEach(entry -> writer.println(entry.getKey().getTaxId() + "\t" + entry.getValue()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Read {

        private final String header;
        private final LinkedList<ReadTaxonAssiciation> readTaxonAssiciations;

        public Read(String header) {
            this.header = header;
            this.readTaxonAssiciations = new LinkedList<>();
        }

        /**
         * Adds a read assignment to the read. If the node is already assigned to the read, the count is incremented.
         * @param node
         */
        public void addReadAssignment(Node node) {
            synchronized (this) {
                for (ReadTaxonAssiciation readTaxonAssociation : readTaxonAssiciations) {
                    if (readTaxonAssociation.getNode() == node) {
                        readTaxonAssociation.incrementCount();
                        return;
                    }
                }
                readTaxonAssiciations.add(new ReadTaxonAssiciation(node));
            }
        }

        /**
         * Adds a read assignment to the read. If the node is already assigned to the read, the count is replaced.
         * @param node
         * @param count
         */
        public void addReadAssignment(Node node, int count) {
            readTaxonAssiciations.add(new ReadTaxonAssiciation(node, count));
        }

        public void sortAssociations() {
            readTaxonAssiciations.sort((a1, a2) -> Integer.compare(a2.getCount(), a1.getCount()));
        }

        public LinkedList<ReadTaxonAssiciation> getAssociations() {
            return readTaxonAssiciations;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(header).append("\t");
            sortAssociations();
            getAssociations().forEach(readTaxonAssiciation -> sb
                    .append(readTaxonAssiciation.getNode().getTaxId())
                    .append(":")
                    .append(readTaxonAssiciation.getCount()).append(" "));
            sb.deleteCharAt(sb.length() - 1);
            return sb.toString();
        }
    }

    public static class ReadTaxonAssiciation {
        private final Node node;
        private int count;

        public ReadTaxonAssiciation(Node node) {
            this.node = node;
            this.count = 1;
        }

        public ReadTaxonAssiciation(Node node, int count) {
            this.node = node;
            this.count = count;
        }

        public void incrementCount() {
            count++;
        }

        public Node getNode() {
            return node;
        }

        public int getCount() {
            return count;
        }

        @Override
        public String toString() {
            return node.getTaxId() + ": " + count;
        }
    }
}
