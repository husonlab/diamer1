package org.husonlab.diamer2.io;

import org.husonlab.diamer2.io.accessionMapping.AccessionMapping;
import org.husonlab.diamer2.io.seq.SequenceRecordContainer;
import org.husonlab.diamer2.io.seq.SequenceSupplier;
import org.husonlab.diamer2.seq.SequenceRecord;
import org.husonlab.diamer2.seq.alphabet.Utilities;
import org.husonlab.diamer2.util.Pair;
import org.husonlab.diamer2.util.logging.*;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.GZIPOutputStream;

import org.husonlab.diamer2.taxonomy.Tree;
import org.husonlab.diamer2.taxonomy.Node;

public class NCBIReader {

    /**
     * Read the NCBI taxonomy from the nodes and names dumpfile.
     * <p>Can usually be downloaded under
     * <a href=https://ftp.ncbi.nlm.nih.gov/pub/taxonomy/>https://ftp.ncbi.nlm.nih.gov/pub/taxonomy/</a>.</p>
     * @param nodesDumpfile path to the nodes dumpfile (nodes.dmp)
     * @param namesDumpfile path to the names dumpfile (names.dmp)
     */
    @NotNull
    public static Tree readTaxonomy(@NotNull Path nodesDumpfile, @NotNull Path namesDumpfile){
        Logger logger = new Logger("NCBIReader").addElement(new Time());
        logger.logInfo("Reading nodes dumpfile...");
        Tree tree = readNodesDumpfile(nodesDumpfile, logger);
        logger.logInfo("Reading names dumpfile...");
        readNamesDumpfile(namesDumpfile, tree);
        logger.logInfo("Finished reading taxonomy. Tree with %d nodes."
                .formatted(tree.idMap.size()));
        return tree;
    }

    /**
     * Generates a taxonomic tree from the NCBI nodes dumpfile.
     * @param nodesDumpfile: path to the file
     * @return A taxonomic trees with the nodes from the dumpfile
     */
    private static Tree readNodesDumpfile(Path nodesDumpfile, Logger logger) {
        Tree tree = new Tree();
        ProgressBar progressBar = new ProgressBar(nodesDumpfile.toFile().length(), 20);
        new OneLineLogger("NCBIReader", 500)
                .addElement(progressBar);
        HashMap<Integer, Integer> parentMap = new HashMap<>();
        try (CountingInputStream cis = new CountingInputStream(new FileInputStream(nodesDumpfile.toString()));
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
        logger.logInfo("Connecting nodes...");
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
        return tree;
    }

    /**
     * Extracts the sequence accessions from the header of a fasta file.
     * <p>
     *     Each accession has to start with ">".
     * </p>
     * @param header the header of a fasta file
     * @return a list of accessions
     */
    public static ArrayList<String> extractAccessionsFromHeader(String header) {
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
    public static void readNamesDumpfile(Path namesDumpfile, Tree tree) {
        ProgressBar progressBar = new ProgressBar(namesDumpfile.toFile().length(), 20);
        new OneLineLogger("NCBIReader", 500)
                .addElement(progressBar);
        try (CountingInputStream cis = new CountingInputStream(new FileInputStream(namesDumpfile.toString()));
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

    /**
     * Extracts the accessions from the headers of input sequences
     * @param sequenceSupplier supplier of the sequences
     * @return a HashMap with the accessions as keys and -1 as values
     */
    public static HashMap<String, Integer> extractAccessions(
            SequenceSupplier<String, Character> sequenceSupplier) throws IOException {
        Logger logger = new Logger("NCBIReader").addElement(new Time());
        logger.logInfo("Estimating number of sequenceRecords in database...");
        int numberOfSequencesEst = sequenceSupplier.approximateNumberOfSequences();
        HashMap<String, Integer> neededAccessions = new HashMap<>(numberOfSequencesEst);
        logger.logInfo("Extracting accessions from database...");
        ProgressBar progressBar = new ProgressBar(sequenceSupplier.getFileSize(), 20);
        ProgressLogger progressLogger = new ProgressLogger("Fastas");
        new OneLineLogger("NCBIReader", 1000)
                .addElement(new RunningTime())
                .addElement(progressBar)
                .addElement(progressLogger);
        SequenceRecordContainer<String, Character> container;
        sequenceSupplier.reset();
        while ((container = sequenceSupplier.next()) != null) {
            progressBar.setProgress(sequenceSupplier.getBytesRead());
            progressLogger.incrementProgress();
            for (SequenceRecord<String, Character> record: container.getSequenceRecords()) {
                for (String accession : extractAccessionsFromHeader(record.id())) {
                    neededAccessions.put(accession, -1);
                }
            }
        }
        progressBar.finish();
        return neededAccessions;
    }

    /**
     * Preprocesses the NR database to have only the taxid of the LCA in the headers of the sequenceRecords.
     * Skips sequenceRecords that can not be found in the taxonomy.
     * Handles non-amino acid characters.
     * @param output: file to write the preprocessed database to
     * @param tree: NCBI taxonomy tree
     */
    public static void preprocessNRBuffered(Path output, Tree tree, AccessionMapping accessionMapping, SequenceSupplier<String, Character> sup) throws IOException {

        HashSet<String> highRanks = new HashSet<>(
                Arrays.asList("superkingdom", "kingdom", "phylum", "class", "order", "family"));

        Logger logger = new Logger("NCBIReader").addElement(new Time());

        logger.logInfo("Preprocessing NR database...");
        int fastaIndex = 0;
        Counts counts = new Counts();
        HashMap<String, Integer> rankMapping = new HashMap<>();
        SequenceRecordContainer<String, Character> container;

        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(
                     Files.newOutputStream(output))));
             BufferedWriter bwSkipped = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(
                     Files.newOutputStream(output.getParent().resolve("skipped_sequences.fsa.gz")))))) {

            ProgressBar progressBar = new ProgressBar(sup.getFileSize(), 20);
            ProgressLogger progressLogger = new ProgressLogger("Fastas");
            new OneLineLogger("NCBIReader", 1000)
                    .addElement(new RunningTime())
                    .addElement(progressBar)
                    .addElement(progressLogger);

            int bufferSize = 100000; // 100000
            Pair<SequenceRecord<String, Character>, Integer>[] sequenceBuffer = new Pair[bufferSize];
            ArrayList<String> accessionBuffer = new ArrayList<>();

            while ((container = sup.next()) != null) {
                progressBar.setProgress(sup.getBytesRead());
                progressLogger.setProgress(fastaIndex + 1);

                for (SequenceRecord<String, Character> record: container.getSequenceRecords()) {
                    // Extract taxIds and compute LCA taxId
                    String header = record.id();
                    ArrayList<String> accessions = extractAccessionsFromHeader(header);
                    sequenceBuffer[fastaIndex % bufferSize] = new Pair<>(record, accessions.size());
                    accessionBuffer.addAll(accessions);

                    if (fastaIndex > 0 && (fastaIndex + 1) % bufferSize == 0) {
                        ArrayList<Integer> taxIdBuffer = accessionMapping.getTaxIds(accessionBuffer);
                        emptyBuffer(bufferSize, sequenceBuffer, accessionBuffer, taxIdBuffer, bw, bwSkipped, tree,
                                highRanks, rankMapping, counts);
                    }
                }
                fastaIndex++;
            }
            ArrayList<Integer> taxIdBuffer = accessionMapping.getTaxIds(accessionBuffer);
            emptyBuffer(fastaIndex % bufferSize, sequenceBuffer, accessionBuffer, taxIdBuffer, bw, bwSkipped, tree,
                    highRanks, rankMapping, counts);
            progressBar.finish();
        }
        String report = """
                \tdate \t %s
                \tinput file \t %s
                \toutput file \t %s
                \tprocessed sequenceRecords \t %d
                \tkept sequenceRecords \t %d
                \tskipped sequenceRecords without (valid) accession \t %d
                \t[DISABLED] skipped sequenceRecords with rank too high (%s) \t %d
                """.formatted(
                java.time.LocalDateTime.now(),
                sup.getFile(),
                output,
                fastaIndex + 1,
                fastaIndex - counts.skippedNoTaxId - counts.skippedRank + 1,
                counts.skippedNoTaxId,
                String.join(", ", highRanks),
                counts.skippedRank
        );

        logger.logInfo("Finished preprocessing NR database.\n" + report);

        try (BufferedWriter bw = Files.newBufferedWriter(output.getParent().resolve("preprocessing_report.txt"))) {
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

    private static void emptyBuffer(int bufferSize, Pair<SequenceRecord<String, Character>, Integer>[] sequenceBuffer,
                                    ArrayList<String> accessionBuffer, ArrayList<Integer> taxIdBuffer,
                                    BufferedWriter bw, BufferedWriter bwSkipped, Tree tree,
                                    HashSet<String> highRanks, HashMap<String, Integer> rankMapping,
                                    Counts counts) throws IOException {
        int taxIdIndex = 0;
        for (Pair<SequenceRecord<String, Character>, Integer> bufferEntry: sequenceBuffer) {
            if (bufferEntry == null) {    // buffer is not full
                break;
            }
            SequenceRecord<String, Character> record = bufferEntry.first();
            String header = record.id();
            String sequence = record.getSequenceString();
            int nrOfAccessions = bufferEntry.last();
            if (nrOfAccessions == 0) {
                counts.skippedNoTaxId++;
                bwSkipped.write(record.id() + " (No accession found in header)");
                bwSkipped.newLine();
                bwSkipped.write(sequence);
                bwSkipped.newLine();
                continue;
            }
            ArrayList<Integer> taxIdsNode = new ArrayList<>();
            for (int j = 0; j < nrOfAccessions; j++) {
                int taxId = taxIdBuffer.get(taxIdIndex++);
                if (taxId != -1) {
                    taxIdsNode.add(taxId);
                }
            }
            if (taxIdsNode.isEmpty()) {
                counts.skippedNoTaxId++;
                bwSkipped.write(header + " (Accession(s) not found in mapping)");
                bwSkipped.newLine();
                bwSkipped.write(sequence);
                bwSkipped.newLine();
                continue;
            }
            // Compute LCA of all taxIds
            int taxId = taxIdsNode.getFirst();
            for (int j = 1; j < taxIdsNode.size(); j++) {
                taxId = tree.findLCA(taxId, taxIdsNode.get(j));
            }
            if (!tree.idMap.containsKey(taxId)) {
                counts.skippedNoTaxId++;
                bwSkipped.write(header + " (taxId(s) not found in taxonomy %d)".formatted(taxId));
                bwSkipped.newLine();
                bwSkipped.write(sequence);
                bwSkipped.newLine();
                continue;
            }
            String rank = tree.idMap.get(taxId).getRank();
            rankMapping.computeIfPresent(rank, (k, v) -> v + 1);
            rankMapping.putIfAbsent(rank, 1);
//            if (highRanks.contains(rank)) {
//                counts.skippedRank++;
//                bwSkipped.write(header + " (rank to high: %s)".formatted(rank));
//                bwSkipped.newLine();
//                bwSkipped.write(sequence);
//                bwSkipped.newLine();
//                continue;
//            }
            header = ">%d".formatted(taxId);

            // Split the sequence by stop codons
            ArrayList<SequenceRecord<String, Character>> newRecords = new ArrayList<>();
            for (String sequencePart : sequence.split("\\*")) {
                newRecords.add(SequenceRecord.AA(header, Utilities.enforceAlphabet(sequencePart)));
            }

            // Write the sequenceRecords to the output file
            for (SequenceRecord<String, Character> newRecord : newRecords) {
                bw.write(newRecord.id());
                bw.newLine();
                bw.write(newRecord.getSequenceString());
                bw.newLine();
            }
        }
        accessionBuffer.clear();
        Arrays.fill(sequenceBuffer, null);
    }

    /**
     * Preprocesses the NR database to have only the taxid of the LCA in the headers of the sequenceRecords.
     * Skips sequenceRecords that can not be found in the taxonomy.
     * Handles non-amino acid characters.
     * @param output: file to write the preprocessed database to
     * @param tree: NCBI taxonomy tree
     */
    public static void preprocessNR(Path output, Tree tree, AccessionMapping accessionMapping, SequenceSupplier<String, Character> sup) throws IOException {

        HashSet<String> highRanks = new HashSet<>(
                Arrays.asList("superkingdom", "kingdom", "phylum", "class", "order", "family"));

        Logger logger = new Logger("NCBIReader").addElement(new Time());

        logger.logInfo("Preprocessing NR database...");
        int processedFastas = 0;
        int skippedNoTaxId = 0;
        int skippedRank = 0;
        HashMap<String, Integer> rankMapping = new HashMap<>();
        SequenceRecordContainer<String, Character> container;

        if (!output.toString().endsWith(".gz")) {
            output = output.resolveSibling(output.getFileName() + ".gz");
        }

        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(
                     new FileOutputStream(output.toString())))) ;
             BufferedWriter bwSkipped = Files.newBufferedWriter(output.getParent().resolve("skipped_sequences.fsa"))) {

            ProgressBar progressBar = new ProgressBar(sup.getFileSize(), 20);
            ProgressLogger progressLogger = new ProgressLogger("Fastas");
            new OneLineLogger("NCBIReader", 1000)
                    .addElement(new RunningTime())
                    .addElement(progressBar)
                    .addElement(progressLogger);

            while ((container = sup.next()) != null) {
                processedFastas++;
                progressBar.setProgress(sup.getBytesRead());
                progressLogger.setProgress(processedFastas);

                for (SequenceRecord<String, Character> record: container.getSequenceRecords()) {
                    // Extract taxIds and compute LCA taxId
                    String header = record.id();
                    ArrayList<Integer> taxIds = new ArrayList<>();
                    for (String id: extractAccessionsFromHeader(header)) {
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
                        bwSkipped.write(record.getSequenceString());
                        bwSkipped.newLine();
                        continue;
                    }
                    if (!tree.idMap.containsKey(taxId)) {
                        skippedNoTaxId++;
                        bwSkipped.write(header + " (taxId not found in taxonomy %d)".formatted(taxId));
                        bwSkipped.newLine();
                        bwSkipped.write(record.getSequenceString());
                        bwSkipped.newLine();
                        continue;
                    }
                    String rank = tree.idMap.get(taxId).getRank();
                    rankMapping.computeIfPresent(rank, (k, v) -> v + 1);
                    rankMapping.putIfAbsent(rank, 1);
//                if (highRanks.contains(rank)) {
//                    skippedRank++;
//                    bwSkipped.write(header + " (rank to high: %s)".formatted(rank));
//                    bwSkipped.newLine();
//                    bwSkipped.write(fasta.getSequenceString());
//                    bwSkipped.newLine();
//                    continue;
//                }
                    header = ">%d".formatted(taxId);

                    // Split the sequence by stop codons
                    ArrayList<SequenceRecord<String, Character>> fastas = new ArrayList<>();
                    for (String sequence : record.getSequenceString().split("\\*")) {
                        fastas.add(SequenceRecord.AA(header, Utilities.enforceAlphabet(sequence)));
                    }

                    // Write the sequenceRecords to the output file
                    for (SequenceRecord<String, Character> fasta2 : fastas) {
                        bw.write(fasta2.id());
                        bw.newLine();
                        bw.write(fasta2.getSequenceString());
                        bw.newLine();
                    }
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
                sup.getFile(),
                output,
                processedFastas,
                processedFastas - skippedNoTaxId - skippedRank,
                skippedNoTaxId,
                String.join(", ", highRanks),
                skippedRank);

        logger.logInfo("Finished preprocessing NR database.\n" + report);

        try (BufferedWriter bw = Files.newBufferedWriter(output.getParent().resolve("preprocessing_report.txt"))) {
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

    private static class Counts {
        public int skippedNoTaxId = 0;
        public int skippedRank = 0;
    }
}
