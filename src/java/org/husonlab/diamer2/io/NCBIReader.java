package org.husonlab.diamer2.io;

import org.husonlab.diamer2.logging.*;
import org.husonlab.diamer2.seq.Sequence;
import org.husonlab.diamer2.seq.alphabet.Utilities;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import org.husonlab.diamer2.taxonomy.Tree;
import org.husonlab.diamer2.taxonomy.Node;

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
//            final Tree tree = new Tree(new HashMap<>(), new HashMap<>());
            return readTaxonomyWithAccessions(nodesDumpfile, namesDumpfile, accessionMappings, tree);
        }
    }

    @NotNull
    private static Tree readTaxonomyWithAccessions(
            @NotNull File nodesDumpfile,
            @NotNull File namesDumpfile,
            @NotNull AccessionMapping[] accessionMappings,
            @NotNull Tree tree) throws IOException {
        Logger logger = new Logger("NCBIReader").addElement(new Time());
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
     * ReadAssignment the NCBI taxonomy from the nodes and names dumpfiles.
     * @param nodesDumpfile path to the nodes dumpfile (nodes.dmp)
     * @param namesDumpfile path to the names dumpfile (names.dmp)
     */
    @NotNull
    public static Tree readTaxonomy(@NotNull File nodesDumpfile, @NotNull File namesDumpfile){
        Logger logger = new Logger("NCBIReader").addElement(new Time());
        final Tree tree = new Tree();
        logger.logInfo("Reading nodes dumpfile...");
        readNodesDumpfile(nodesDumpfile, tree);
        logger.logInfo("Reading names dumpfile...");
        readNamesDumpfile(namesDumpfile, tree);
        logger.logInfo("Finished reading taxonomy. Tree with %d nodes."
                .formatted(tree.idMap.size()));
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
        Logger logger = new Logger("NCBIReader").addElement(new Time());
        logger.logInfo("Preprocessing NR database...");
        int processedFastas = 0;
        int skippedNoTaxId = 0;
        int skippedRank = 0;
        HashMap<String, Integer> rankMapping = new HashMap<>();
        Sequence fasta;

        try (SequenceSupplier sup = SequenceSupplier.getFastaSupplier(nr, false);
             BufferedWriter bw = Files.newBufferedWriter(output.toPath());
             BufferedWriter bwSkipped = Files.newBufferedWriter(Path.of(output.getParent(), "skipped_sequences.fsa"))) {

            ProgressBar progressBar = new ProgressBar(sup.getFileSize(), 20);
            ProgressLogger progressLogger = new ProgressLogger("Fastas");
            new OneLineLogger("NCBIReader", 1000)
                    .addElement(new RunningTime())
                    .addElement(progressBar)
                    .addElement(progressLogger);

            while ((fasta = sup.next()) != null) {
                processedFastas++;
                progressBar.setProgress(sup.getBytesRead());
                progressLogger.setProgress(processedFastas);

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
                    skippedNoTaxId++;
                    bwSkipped.write(header + " (no accession found in taxonomy)");
                    bwSkipped.newLine();
                    bwSkipped.write(fasta.getSequence());
                    bwSkipped.newLine();
                    continue;
                }
                String rank = tree.idMap.get(taxId).getRank();
                rankMapping.computeIfPresent(rank, (k, v) -> v + 1);
                rankMapping.putIfAbsent(rank, 1);
                if (rank.equals("no rank") || rank.equals("superkingdom")) {
                    skippedRank++;
                    bwSkipped.write(header + " (rank to low: %s)".formatted(rank));
                    bwSkipped.newLine();
                    bwSkipped.write(fasta.getSequence());
                    bwSkipped.newLine();
                    continue;
                }
                header = ">%d".formatted(taxId);

                // Split the sequence by stop codons
                ArrayList<Sequence> fastas = new ArrayList<>();
                for (String sequence : fasta.getSequence().split("\\*")) {
                    fastas.add(new Sequence(header, Utilities.enforceAlphabet(sequence)));
                }

                // Write the sequences to the output file
                for (Sequence fasta2 : fastas) {
                    bw.write(fasta2.getHeader());
                    bw.newLine();
                    bw.write(fasta2.getSequence());
                    bw.newLine();
                }
            }
            progressBar.finish();
        }
        String report = """
                \tdate \t %s
                \tinput file \t %s
                \toutput file \t %s
                \tprocessed sequences \t %d
                \tkept sequences \t %d
                \tskipped sequences without accession \t %d
                \tskipped sequences with rank 'no rank' or 'superkingdom' \t %d
                """.formatted(
                java.time.LocalDateTime.now(),
                nr,
                output,
                processedFastas,
                processedFastas - skippedNoTaxId - skippedRank,
                skippedNoTaxId,
                skippedRank);

        logger.logInfo("Finished preprocessing NR database.\n" + report);

        try (BufferedWriter bw = Files.newBufferedWriter(Path.of(output.getParent(), "preprocessing_report.txt"))) {
            bw.write(report);
            bw.newLine();
            rankMapping.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .forEach(entry -> {
                try {
                    bw.write("%s\t%d".formatted(entry.getKey(), entry.getValue()));
                    bw.newLine();
                } catch (IOException e) {
                    System.err.println("Could not write preprocessing report.");
                }
            });
        }
    }

    /**
     * ReadAssignment the NCBI nodes.dmp file and creates a map of tax_id -> Node objects.
     * @param nodesDumpfile: path to the file
     * @param tree: Tree with idMap to store the nodes
     */
    private static void readNodesDumpfile(File nodesDumpfile, Tree tree){
        ProgressBar progressBar = new ProgressBar(nodesDumpfile.length(), 20);
        new OneLineLogger("NCBIReader", 500)
                .addElement(progressBar);
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
                progressBar.setProgress(cis.getBytesRead());
            }
            progressBar.finish();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // set the parent-child relationships after all nodes have been created
        parentMap.forEach( (nodeId, parentId) -> {
            if (!Objects.equals(nodeId, parentId)) {
                Node node = tree.idMap.get(nodeId);
                Node parent = tree.idMap.get(parentId);
                node.setParent(parent);
                parent.addChild(node);
            }
        });
        tree.setRoot();
    }

    /**
     * ReadAssignment the NCBI names.dmp file and adds the names to the corresponding Node objects.
     * @param namesDumpfile: path to the file
     * @param tree: Tree with idMap of tax_id -> Node objects
     */
    public static void readNamesDumpfile(File namesDumpfile, Tree tree) {
        ProgressBar progressBar = new ProgressBar(namesDumpfile.length(), 20);
        new OneLineLogger("NCBIReader", 500)
                .addElement(progressBar);
        try (CountingInputStream cis = new CountingInputStream(new FileInputStream(namesDumpfile));
             BufferedReader br = new BufferedReader(new InputStreamReader(cis)) ) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split("\t\\|\t");
                int taxId = Integer.parseInt(values[0]);
                String label = values[1];
                Node node = tree.idMap.get(taxId);
                node.addLabel(label);
                if (values.length > 3 && values[3].equals("scientific name\t|")) {
                    node.setScientificName(label);
                }
                progressBar.setProgress(cis.getBytesRead());
            }
            progressBar.finish();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void readAccessionMap(String accessionMapFile, int accessionCol, int taxIdCol, Tree tree) throws IOException {

        ProgressBar progressBar = new ProgressBar(new File(accessionMapFile).length(), 20);
        ProgressLogger progressLogger = new ProgressLogger("Accessions");
        new OneLineLogger("NCBIReader", 500)
                .addElement(new RunningTime())
                .addElement(progressBar)
                .addElement(progressLogger);

        try (FileInputStream fis = new FileInputStream(accessionMapFile);
             GZIPInputStream gis = new GZIPInputStream(fis);
             CountingInputStream cis = new CountingInputStream(gis);
             BufferedReader br = new BufferedReader(new InputStreamReader(cis))) {
            String line;
            br.readLine(); // skip header
            long i = 0;
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
                progressBar.setProgress(cis.getBytesRead()/6);
                progressLogger.setProgress(++i);
            }
            progressBar.finish();
        }
    }
    public record AccessionMapping(String mappingFile, int accessionCol, int taxIdCol) {}
}
