package org.husonlab.diamer2.readAssignment;

import org.husonlab.diamer2.logging.Logger;
import org.husonlab.diamer2.taxonomy.Node;
import org.husonlab.diamer2.taxonomy.Tree;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

public class ReadAssignment implements Iterable<Read> {

    private final Logger logger;
    private final Tree tree;
    private final int size;
    private final Read[] readAssignments;

    public ReadAssignment(Tree tree, HashMap<Integer, String> readHeaderMapping) {
        this.logger = new Logger("ReadAssignment");
        this.size = readHeaderMapping.size();
        this.readAssignments = new Read[size];
        this.tree = tree;
        readHeaderMapping.forEach((readId, header) -> readAssignments[readId] = new Read(header));
    }

    public ReadAssignment(Tree tree, Read[] readAssignments) {
        this.logger = new Logger("ReadAssignment");
        this.tree = tree;
        this.size = readAssignments.length;
        this.readAssignments = readAssignments;
    }

    public int size() {
        return this.size;
    }

    public void addReadAssignment(int readId, int taxId) {
        readAssignments[readId].addReadAssignment(tree.idMap.get(taxId));
    }

    public ReadStatistics getStatistics() {
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
        return new ReadStatistics(kmerMatches, kmerPerSpecies, kmerPerGenus);
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


    /**
     * Represents the statistics of a read assignment.
     * @param kmerMatches Hashmap that counts the number of found kmers that match a specific taxon
     * @param kmerPerSpecies Hashmap that counts the number of kmers that match a specific species
     * @param kmerPerGenus Hashmap that counts the number of kmers that match a specific genus
     */
    public record ReadStatistics(
            HashMap<Node, Integer> kmerMatches,
            HashMap<Node, Integer> kmerPerSpecies,
            HashMap<Node, Integer> kmerPerGenus){}
}
