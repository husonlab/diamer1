package org.husonlab.diamer2.io;

import org.husonlab.diamer2.io.accessionMapping.AccessionMapping;
import org.husonlab.diamer2.io.seq.FASTAReader;
import org.husonlab.diamer2.io.seq.SequenceSupplier;
import org.husonlab.diamer2.seq.HeaderSequenceRecord;
import org.husonlab.diamer2.seq.alphabet.Utilities;
import org.husonlab.diamer2.seq.alphabet.converter.AAtoBase11;
import org.husonlab.diamer2.util.Pair;
import org.husonlab.diamer2.util.logging.*;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.husonlab.diamer2.taxonomy.Tree;
import org.husonlab.diamer2.taxonomy.Node;

public class NCBIReader {

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

    public static Pair<HashSet<String>, SequenceSupplier> extractNeededAccessions(File fasta) throws IOException {

        Logger logger = new Logger("NCBIReader").addElement(new Time());

        logger.logInfo("Estimating number of sequenceRecords in database...");
        int numberOfSequencesEst = org.husonlab.diamer2.io.Utilities.approximateNumberOfSequences(fasta, "\n>");
        HashSet<String> neededAccessions = new HashSet<>(numberOfSequencesEst);
        logger.logInfo("Extracting accessions from database...");
        SequenceSupplier<Short> sequenceSupplier;
        try(SequenceSupplier<Short> sup = new SequenceSupplier<Short>(new FASTAReader(fasta), new AAtoBase11(), false)) {
            sequenceSupplier = sup;
            ProgressBar progressBar = new ProgressBar(sup.getFileSize(), 20);
            new OneLineLogger("NCBIReader", 1000)
                    .addElement(new RunningTime())
                    .addElement(progressBar);

            HeaderSequenceRecord<Short> seq;
            while ((seq = sup.next()) != null) {
                progressBar.setProgress(sup.getBytesRead());
                neededAccessions.addAll(extractIdsFromHeader(seq.getHeader()));
            }
            progressBar.finish();
        }
        return new Pair<>(neededAccessions, sequenceSupplier);
    }

    /**
     * Preprocesses the NR database to have only the taxid of the LCA in the headers of the sequenceRecords.
     * Skips sequenceRecords that can not be found in the taxonomy.
     * Handles non-amino acid characters.
     * @param output: file to write the preprocessed database to
     * @param tree: NCBI taxonomy tree
     */
    public static void preprocessNR(File output, Tree tree, AccessionMapping accessionMapping, SequenceSupplier sequenceSupplier) throws IOException {

        HashSet<String> highRanks = new HashSet<>(
                Arrays.asList("superkingdom", "kingdom", "phylum", "class", "order", "family"));

        Logger logger = new Logger("NCBIReader").addElement(new Time());

        logger.logInfo("Preprocessing NR database...");
        int processedFastas = 0;
        int skippedNoTaxId = 0;
        int skippedRank = 0;
        HashMap<String, Integer> rankMapping = new HashMap<>();
        HeaderSequenceRecord fasta;

        try (SequenceSupplier sup = sequenceSupplier.open();
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
                ArrayList<Integer> taxIds = new ArrayList<>();
                for (String id: extractIdsFromHeader(header)) {
                    int taxId = accessionMapping.getTaxId(id);
                    if (taxId != -1) {
                        taxIds.add(taxId);
                    }
                }
                int taxId;
                if (!taxIds.isEmpty()) {
                    taxId = taxIds.getFirst();
                    for (int i = 1; i < taxIds.size(); i++) {
                        taxId = tree.findLCA(taxId, taxIds.get(i));
                    }
                } else {
                    skippedNoTaxId++;
                    bwSkipped.write(header + " (Accession(s) not found in mapping)");
                    bwSkipped.newLine();
                    bwSkipped.write(fasta.getSequenceString());
                    bwSkipped.newLine();
                    continue;
                }
                if (!tree.idMap.containsKey(taxId)) {
                    skippedNoTaxId++;
                    bwSkipped.write(header + " (taxId not found in taxonomy %d)".formatted(taxId));
                    bwSkipped.newLine();
                    bwSkipped.write(fasta.getSequenceString());
                    bwSkipped.newLine();
                    continue;
                }
                String rank = tree.idMap.get(taxId).getRank();
                rankMapping.computeIfPresent(rank, (k, v) -> v + 1);
                rankMapping.putIfAbsent(rank, 1);
                if (highRanks.contains(rank)) {
                    skippedRank++;
                    bwSkipped.write(header + " (rank to high: %s)".formatted(rank));
                    bwSkipped.newLine();
                    bwSkipped.write(fasta.getSequenceString());
                    bwSkipped.newLine();
                    continue;
                }
                header = ">%d".formatted(taxId);

                // Split the sequence by stop codons
                ArrayList<HeaderSequenceRecord> fastas = new ArrayList<>();
                for (String sequence : fasta.getSequenceString().split("\\*")) {
                    fastas.add(HeaderSequenceRecord.AA(header, Utilities.enforceAlphabet(sequence)));
                }

                // Write the sequenceRecords to the output file
                for (HeaderSequenceRecord fasta2 : fastas) {
                    bw.write(fasta2.getHeader());
                    bw.newLine();
                    bw.write(fasta2.getSequenceString());
                    bw.newLine();
                }
            }
            progressBar.finish();
        }
        String report = """
                \tdate \t %s
                \tinput file \t %s
                \toutput file \t %s
                \tprocessed sequenceRecords \t %d
                \tkept sequenceRecords \t %d
                \tskipped sequenceRecords without accession \t %d
                \tskipped sequenceRecords with rank too high (%s) \t %d
                """.formatted(
                java.time.LocalDateTime.now(),
                sequenceSupplier.getFile(),
                output,
                processedFastas,
                processedFastas - skippedNoTaxId - skippedRank,
                skippedNoTaxId,
                String.join(", ", highRanks),
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
        tree.autoFindRoot();
    }

    public static ArrayList<String> extractIdsFromHeader(String header) {
        String[] values = header.split(" ");
        ArrayList<String> ids = new ArrayList<>();
        for (String value : values) {
            if (value.startsWith(">")) {
                ids.add(AccessionMapping.removeVersion(value.substring(1)));
            }
        }
        return ids;
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
}
