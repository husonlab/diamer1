package org.husonlab.diamer2.main;

import org.apache.commons.cli.CommandLine;
import org.husonlab.diamer2.io.NCBIReader;
import org.husonlab.diamer2.io.accessionMapping.AccessionMapping;
import org.husonlab.diamer2.io.accessionMapping.MeganMapping;
import org.husonlab.diamer2.io.accessionMapping.NCBIMapping;
import org.husonlab.diamer2.io.seq.FastaReader;
import org.husonlab.diamer2.io.seq.SequenceSupplier;
import org.husonlab.diamer2.taxonomy.Tree;
import org.husonlab.diamer2.util.logging.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;

import static org.husonlab.diamer2.io.Utilities.getFile;
import static org.husonlab.diamer2.main.CliUtils.*;

/**
 * Class to preprocess a protein sequence database to replace the headers of the sequences with the taxId of the LCA
 * of all source organisms.
 */
public class Preprocessing {
    public static void preprocess(CommandLine cli, GlobalSettings globalSettings) {
        Logger logger = new Logger("Preprocessing");

        // input, output, mapping file(s)
        checkNumberOfPositionalArguments(cli, 3);
        Path database = getFile(cli.getArgs()[0], true);
        Path output = getFile(cli.getArgs()[1], false);
        // ensure writing a gzipped output file
        if (!output.toString().endsWith(".gz")) {
            output = output.getParent().resolve(output.getFileName() + ".gz");
        }

        // create log file writer
        LogFileWriter logFileWriter = new LogFileWriter(output.getParent().resolve("run.log"));
        logFileWriter.writeSettings(globalSettings);
        logFileWriter.writeTimeStamp("Preprocessing started");

        // reading the taxonomic tree
        Tree tree = readTree(cli);

        // parse all mapping files
        AccessionMapping accessionMapping;
        ArrayList<Path> mappingFiles = new ArrayList<>();
        for (int i = 2; i < cli.getArgs().length; i++) {
            mappingFiles.add(getFile(cli.getArgs()[i], true));
        }

        String runInfo;
        try (SequenceSupplier<String, String> sequenceSupplier = new SequenceSupplier<>(
                new FastaReader(database), SequenceSupplier.getEmptyConverter(), globalSettings.KEEP_IN_MEMORY)) {

            // case: mapping file is a megan mapping file
            if (mappingFiles.getFirst().toString().endsWith(".mdb") || mappingFiles.getFirst().toString().endsWith(".db")) {
                logger.logInfo("Using MEGAN mapping file: " + mappingFiles.getFirst());
                logFileWriter.writeLog("Using MEGAN mapping file: " + mappingFiles.getFirst());
                // open SQLite database
                accessionMapping = new MeganMapping(mappingFiles.getFirst());
                // read over database and convert headers
                runInfo = NCBIReader.preprocessNRBuffered(output, tree, accessionMapping, sequenceSupplier);

            // case: mapping file is a NCBI mapping file
            } else {
                logger.logInfo("Using NCBI mapping files: " + mappingFiles.getFirst() + " and " + mappingFiles.get(1));
                logFileWriter.writeLog("Using NCBI mapping files: " + mappingFiles.getFirst() + " and " + mappingFiles.get(1));
                // collect all accessions from the database before reading the mapping file
                // to avoid reading all mappings from the file (would result in a huge hashmap)
                HashMap<String, Integer> accession2Taxid = NCBIReader.extractAccessions(sequenceSupplier);
                // read only the mappings required for the accessions in the database
                accessionMapping = new NCBIMapping(mappingFiles, tree, accession2Taxid);
                sequenceSupplier.reset();
                // read over database and convert headers
                runInfo = NCBIReader.preprocessNR(output, tree, accessionMapping, sequenceSupplier);
            }
        } catch (IOException e) {
            logFileWriter.writeLog(e.toString());
            throw new RuntimeException(e);
        }

        // write final log entry
        logFileWriter.writeTimeStamp("Preprocessing finished");
        logFileWriter.writeLog(runInfo);
    }
}
