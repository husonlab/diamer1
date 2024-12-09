package org.husonlab.diamer2.io;

import org.husonlab.diamer2.alphabet.AAEncoder;
import org.husonlab.diamer2.logging.Logger;
import org.husonlab.diamer2.logging.ProgressBar;
import org.husonlab.diamer2.logging.ProgressLogger;
import org.husonlab.diamer2.seq.Sequence;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import org.husonlab.diamer2.graph.Tree;
import org.husonlab.diamer2.graph.Node;

public class NCBIReader {

    /**
     * Parses the NCBI taxonomy, including the node names and the amino acid accessions.
     * Does not set initial HashMap capacities to allow for dynamic resizing in smaller datasets.
     * @param nodesDumpfile: nodes.dmp, containing the taxonomy nodes (format: tax_id | parent_tax_id | rank)
     * @param namesDumpfile: names.dmp, containing the taxonomy names (format: tax_id | name)
     * @param accessionMappings: array of AccessionMapping objects, containing the path to the mapping file and the column indices for the accession and tax_id.
     */
    @NotNull
    public static Tree readTaxonomyWithAccessions(
            @NotNull File nodesDumpfile,
            @NotNull File namesDumpfile,
            @NotNull AccessionMapping[] accessionMappings,
            boolean debug) throws IOException {

        if (debug) {
            final Tree tree = new Tree(new HashMap<>(), new HashMap<>());
            return readTaxonomyWithAccessions(nodesDumpfile, namesDumpfile, accessionMappings, tree);
        } else {
            final Tree tree = new Tree(new HashMap<>(2700000), new HashMap<>(1400000000));
            return readTaxonomyWithAccessions(nodesDumpfile, namesDumpfile, accessionMappings, tree);
        }
    }

    @NotNull
    private static Tree readTaxonomyWithAccessions(
            @NotNull File nodesDumpfile,
            @NotNull File namesDumpfile,
            @NotNull AccessionMapping[] accessionMappings,
            @NotNull Tree tree) throws IOException {
        Logger logger = new Logger("NCBIReader", true);
        logger.logInfo("Reading nodes dumpfile...");
        readNodesDumpfile(nodesDumpfile, tree);
        logger.logInfo("Reading names dumpfile...");
        readNamesDumpfile(namesDumpfile, tree);
        for (AccessionMapping mapping : accessionMappings) {
            logger.logInfo("Reading accession mapping from " + mapping.mappingFile);
            readAccessionMap(mapping.mappingFile, mapping.accessionCol, mapping.taxIdCol, tree);
        }
        logger.logInfo("Finished reading taxonomy. Tree with %d nodes and %d accessions."
                .formatted(tree.idMap.size(), tree.accessionMap.size()));
        return tree;
    }

    /**
     * Reads the NCBI taxonomy from the nodes and names dumpfiles.
     * @param nodesDumpfile path to the nodes dumpfile (nodes.dmp)
     * @param namesDumpfile path to the names dumpfile (names.dmp)
     */
    @NotNull
    public static Tree readTaxonomy(@NotNull File nodesDumpfile, @NotNull File namesDumpfile){
        Logger logger = new Logger("NCBIReader", true);
        final HashMap<Integer, Node> idMap = new HashMap<>();
        final Tree tree = new Tree(idMap);
        logger.logInfo("Reading nodes dumpfile...");
        readNodesDumpfile(nodesDumpfile, tree);
        logger.logInfo("Reading names dumpfile...");
        readNamesDumpfile(namesDumpfile, tree);
        logger.logInfo("Finished reading taxonomy. Tree with %d nodes and %d accessions."
                .formatted(tree.idMap.size(), tree.accessionMap.size()));
        return tree;
    }

    /**
     * Preprocesses the NR database to have only the taxid of the LCA in the headers of the sequences.
     * Skips sequences that can not be found in the taxonomy.
     * Handles non-amino acid characters.
     * @param nr: input nr database
     * @param output: file to write the preprocessed database to
     * @param tree: NCBI taxonomy tree
     */
    public static void preprocessNR(File nr, File output, Tree tree) throws IOException {
        ProgressLogger progressLogger = new ProgressLogger("Fastas", "[NRPreprocessor]", 60000);
        int processedFastas = 0;
        int skippedFastas = 0;
        Sequence fasta;

        try (BufferedReader br = Files.newBufferedReader(nr.toPath());
             FASTAReader fastaReader = new FASTAReader(br);
             BufferedWriter bw = Files.newBufferedWriter(output.toPath());
             BufferedWriter bwSkipped = Files.newBufferedWriter(Path.of(output.getParent(), "skipped_sequences.fsa"))) {
            while ((fasta = fastaReader.getNextSequence()) != null) {
                processedFastas++;

                // Extract taxIds and compute LCA taxId
                String header = fasta.getHeader();
                String[] values = header.split(" ");
                ArrayList<Integer> taxIds = new ArrayList<>();
                for (String value : values) {
                    if (value.startsWith(">") && tree.accessionMap.containsKey(value.substring(1))) {
                        taxIds.add(tree.accessionMap.get(value.substring(1)));
                    }
                }
                int taxId;
                if (!taxIds.isEmpty()) {
                    taxId = taxIds.get(0);
                    for (int i = 1; i < taxIds.size(); i++) {
                        taxId = tree.findLCA(taxId, taxIds.get(i));
                    }
                } else {
                    skippedFastas++;
                    bwSkipped.write(header);
                    bwSkipped.newLine();
                    bwSkipped.write(fasta.getSequence());
                    bwSkipped.newLine();
                    continue;
                }
                header = ">%d".formatted(taxId);

                // Split the sequence by stop codons
                ArrayList<Sequence> fastas = new ArrayList<>();
                for (String sequence : fasta.getSequence().split("\\*")) {
                    fastas.add(new Sequence(header, AAEncoder.enforceAlphabet(sequence)));
                }

                // Write the sequences to the output file
                for (Sequence fasta2 : fastas) {
                    bw.write(fasta2.getHeader());
                    bw.newLine();
                    bw.write(fasta2.getSequence());
                    bw.newLine();
                }
                progressLogger.logProgress(processedFastas);
            }
        }
        System.out.printf("[NCBIReader] Skipped %d entries in the nr file, as no accession could be found in the taxonomy.\n", skippedFastas);
    }

    /**
     * Reads the NCBI nodes.dmp file and creates a map of tax_id -> Node objects.
     * @param nodesDumpfile: path to the file
     * @param tree: Tree with idMap to store the nodes
     */
    private static void readNodesDumpfile(File nodesDumpfile, Tree tree){
        ProgressBar progressBar = new ProgressBar(nodesDumpfile.length(), 20);
        Logger logger = new Logger("NCBIReader", 50, false).addElement(progressBar);
        HashMap<Integer, Integer> parentMap = new HashMap<>();
        try (CountingInputStream cis = new CountingInputStream(new FileInputStream(nodesDumpfile));
             BufferedReader br = new BufferedReader(new InputStreamReader(cis)) ) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split("\t\\|\t");
                int taxId = Integer.parseInt(values[0]);
                int parentTaxId = Integer.parseInt(values[1]);
                String rank = values[2];
                Node node = new Node(taxId, rank);
                tree.idMap.put(taxId, node);
                // parents are recorded separately since the node objects might not have been created yet
                parentMap.put(taxId, parentTaxId);
                progressBar.setProgress(cis.getReadBytes());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        progressBar.finish();
        // set the parent-child relationships after all nodes have been created
        parentMap.forEach( (nodeId, parentId) -> {
            Node node = tree.idMap.get(nodeId);
            Node parent = tree.idMap.get(parentId);
            node.setParent(parent);
            parent.addChild(node);
        });
    }

    /**
     * Reads the NCBI names.dmp file and adds the names to the corresponding Node objects.
     * @param namesDumpfile: path to the file
     * @param tree: Tree with idMap of tax_id -> Node objects
     */
    public static void readNamesDumpfile(File namesDumpfile, Tree tree) {
        try (BufferedReader br = Files.newBufferedReader(namesDumpfile.toPath())) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split("\t\\|\t");
                int taxId = Integer.parseInt(values[0]);
                String label = values[1];
                Node node = tree.idMap.get(taxId);
                node.addLabel(label);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void readAccessionMap(String accessionMapFile, int accessionCol, int taxIdCol, Tree tree) throws IOException {
        try (FileInputStream fis = new FileInputStream(accessionMapFile);
             GZIPInputStream gis = new GZIPInputStream(fis);
             BufferedReader br = new BufferedReader(new InputStreamReader(gis))) {
            String line;
            br.readLine(); // skip header
            long i = 0;
            long start2 = System.nanoTime();
            while ((line = br.readLine()) != null) {
                String[] values = line.split("\t");
                String accession = values[accessionCol];
                int taxId = Integer.parseInt(values[taxIdCol]);
                // only add accessions, if the tax_id is in the tree and the accession is not already in the tree
                if (tree.idMap.containsKey(taxId) && !tree.accessionMap.containsKey(accession)) {
                    tree.accessionMap.put(accession, taxId);
                // case where the tax_id is in the map, but the accession maps to a different taxId
                // find LCA and update the accession map
                } else if (tree.idMap.containsKey(taxId) && tree.accessionMap.get(accession) != taxId) {
                    int oldTaxId = tree.accessionMap.get(accession);
                    int newTaxId = tree.findLCA(oldTaxId, taxId);
                    tree.accessionMap.put(accession, newTaxId);
                }
                if (++i %1000000 == 0) {
                    long end2 = System.nanoTime();
                    System.out.println("[NCBIReader] accessios: " + i/1000000 + "M");
                    System.out.printf("[NCBIReader] accessions per second: %f\n", 1000000/((end2 - start2)*(10E-10)));
                    start2 = System.nanoTime();
                }
            }
        }
    }
    public record AccessionMapping(String mappingFile, int accessionCol, int taxIdCol) {}
}
