package org.husonlab.diamer2.readAssignment;

import org.husonlab.diamer2.logging.Logger;
import org.husonlab.diamer2.logging.OneLineLogger;
import org.husonlab.diamer2.logging.ProgressBar;
import org.husonlab.diamer2.taxonomy.Node;
import org.husonlab.diamer2.taxonomy.Tree;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

public class ReadAssignment implements Iterable<ReadAssignment.Read> {

    private final Logger logger;
    private final Tree tree;
    private final int size;
    private final Read[] readAssignments;

    public ReadAssignment(Tree tree, int size, HashMap<Integer, String> readHeaderMapping) {
        this.readAssignments = new ReadAssignment.Read[size];
        this.size = size;
        this.logger = new Logger("ReadAssignment");
        this.tree = tree;
        readHeaderMapping.forEach((readId, header) -> readAssignments[readId] = new Read(header));
    }

    public ReadAssignment(Tree tree, File readAssignmentFile) {
        this.logger = new Logger("ReadAssignment");
        this.tree = tree;
        logger.logInfo("Reading read assignments from " + readAssignmentFile.getAbsolutePath());
        int lineNumber = 1;
        try(BufferedReader reader = new BufferedReader(new FileReader(readAssignmentFile))) {
            this.size = Integer.parseInt(reader.readLine());
            this.readAssignments = new ReadAssignment.Read[size];

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
    }

    public int size() {
        return this.size;
    }

    public void addReadAssignment(int readId, int taxId) {
        readAssignments[readId].addReadAssignment(tree.idMap.get(taxId));
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
                Node node = tree.idMap.get(association[0]);
                kmerMatches.put(
                        node,
                        kmerMatches.getOrDefault(node, 0) + association[1]
                );
                Node species = tree.getSpecies(node);
                if(species != null) {
                    kmerPerSpecies.put(
                            species,
                            kmerPerSpecies.getOrDefault(species, 0) + association[1]
                    );
                }
                Node genus = tree.getGenus(node);
                if(genus != null) {
                    kmerPerGenus.put(
                            genus,
                            kmerPerGenus.getOrDefault(genus, 0) + association[1]
                    );
                }
            });
        });

        logger.logInfo("Writing read statistics to " + path);
        try (PrintWriter writer = new PrintWriter(path.resolve("kmerMatches.tsv").toFile())) {
            writer.println("TaxId\tCount");
            kmerMatches.entrySet().stream()
                    .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                    .forEach(entry -> writer.println(entry.getKey().toString() + "\t" + entry.getValue()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (PrintWriter writer = new PrintWriter(path.resolve("kmerPerSpecies.tsv").toFile())) {
            writer.println("TaxId\tCount");
            kmerPerSpecies.entrySet().stream()
                    .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                    .forEach(entry -> writer.println(entry.getKey().toString() + "\t" + entry.getValue()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (PrintWriter writer = new PrintWriter(path.resolve("kmerPerGenus.tsv").toFile())) {
            writer.println("TaxId\tCount");
            kmerPerGenus.entrySet().stream()
                    .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                    .forEach(entry -> writer.println(entry.getKey().toString() + "\t" + entry.getValue()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    @Override
    public Iterator<Read> iterator() {
        return new Iterator<Read>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < size;
            }

            @Override
            public Read next() {
                return readAssignments[index++];
            }
        };
    }

    @Override
    public void forEach(Consumer<? super Read> action) {
        for (Read read : this) {
            action.accept(read);
        }
    }

    @Override
    public Spliterator<Read> spliterator() {
        return null;
    }

    public static class Read {

        private final String header;
        private final LinkedList<int[]> readTaxonAssiciations;

        public Read(String header) {
            this.header = header;
            this.readTaxonAssiciations = new LinkedList<>();
        }

        /**
         * Adds a read assignment to the read. If the node is already assigned to the read, the count is incremented.
         * @param node the node to assign
         */
        public void addReadAssignment(Node node) {
            synchronized (this) {
                for (int[] readAssociation : readTaxonAssiciations) {
                    int taxId = node.getTaxId();
                    if (readAssociation[0] == taxId) {
                        readAssociation[1]++;
                        return;
                    }
                }
                readTaxonAssiciations.add(new int[]{node.getTaxId(), 1});
            }
        }

        /**
         * Adds a read assignment to the read. If the node is already assigned to the read, the count is replaced.
         * @param node the node to assign
         * @param count the count of the assignment
         */
        public void addReadAssignment(Node node, int count) {
            for (int[] readAssociation : readTaxonAssiciations) {
                int taxId = node.getTaxId();
                if (readAssociation[0] == taxId) {
                    readAssociation[1] = count;
                    return;
                }
            }
            readTaxonAssiciations.add(new int[]{node.getTaxId(), count});
        }

        public void addReadAssignment(int taxId) {
            for (int[] readAssociation : readTaxonAssiciations) {
                if (readAssociation[0] == taxId) {
                    readAssociation[1]++;
                    return;
                }
            }
            readTaxonAssiciations.add(new int[]{taxId, 1});
        }

        public void addReadAssignment(int taxId, int count) {
            for (int[] readAssociation : readTaxonAssiciations) {
                if (readAssociation[0] == taxId) {
                    readAssociation[1] = count;
                    return;
                }
            }
            readTaxonAssiciations.add(new int[]{taxId, count});
        }

        public void sortAssociations() {
            readTaxonAssiciations.sort((a1, a2) -> Integer.compare(a2[1], a1[1]));
        }

        public LinkedList<int[]> getAssociations() {
            return readTaxonAssiciations;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(header).append("\t");
            sortAssociations();
            getAssociations().forEach(readTaxonAssiciation -> sb
                    .append(readTaxonAssiciation[0])
                    .append(":")
                    .append(readTaxonAssiciation[1]).append(" "));
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
