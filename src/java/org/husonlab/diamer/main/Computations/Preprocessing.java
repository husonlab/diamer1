package org.husonlab.diamer.main.Computations;

import org.apache.commons.cli.CommandLine;
import org.husonlab.diamer.io.NCBIReader;
import org.husonlab.diamer.io.accessionMapping.AccessionMapping;
import org.husonlab.diamer.io.accessionMapping.MeganMapping;
import org.husonlab.diamer.io.accessionMapping.NCBIMapping;
import org.husonlab.diamer.io.seq.FastaReader;
import org.husonlab.diamer.io.seq.SequenceSupplier;
import org.husonlab.diamer.main.GlobalSettings;
import org.husonlab.diamer.taxonomy.Tree;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;

import static org.husonlab.diamer.io.Utilities.getFile;
import static org.husonlab.diamer.main.CliUtils.*;

/**
 * Class to preprocess a protein sequence database to replace the headers of the sequences with the taxId of the LCA
 * of all source organisms.
 */
public class Preprocessing {
    public static void preprocess(CommandLine cli, GlobalSettings settings) {
        // input, output, mapping file(s)
        checkNumberOfPositionalArguments(cli, 3);
        settings.INPUT = getFile(cli.getArgs()[0], true);

        // create log file writer
        settings.logFileWriter.writeSettings(settings);
        settings.logFileWriter.writeTimeStamp("Preprocessing started");

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
                new FastaReader(settings.INPUT), SequenceSupplier.getEmptyConverter(), settings.KEEP_IN_MEMORY)) {

            // case: mapping file is a megan mapping file
            if (mappingFiles.getFirst().toString().endsWith(".mdb") || mappingFiles.getFirst().toString().endsWith(".db")) {
                settings.logger.logInfo("Using MEGAN mapping file: " + mappingFiles.getFirst());
                settings.logFileWriter.writeLog("Using MEGAN mapping file: " + mappingFiles.getFirst());
                // open SQLite database
                accessionMapping = new MeganMapping(mappingFiles.getFirst());
                // read over database and convert headers
                runInfo = NCBIReader.preprocessNRBuffered(settings.OUTPUT, tree, accessionMapping, sequenceSupplier);

            // case: mapping file is a NCBI mapping file
            } else {
                settings.logger.logInfo("Using NCBI mapping files: " + mappingFiles.getFirst() + " and " + mappingFiles.get(1));
                settings.logFileWriter.writeLog("Using NCBI mapping files: " + mappingFiles.getFirst() + " and " + mappingFiles.get(1));
                // collect all accessions from the database before reading the mapping file
                // to avoid reading all mappings from the file (would result in a huge hashmap)
                HashMap<String, Integer> accession2Taxid = NCBIReader.extractAccessions(sequenceSupplier);
                // read only the mappings required for the accessions in the database
                accessionMapping = new NCBIMapping(mappingFiles, tree, accession2Taxid);
                sequenceSupplier.reset();
                // read over database and convert headers
                runInfo = NCBIReader.preprocessNR(settings.OUTPUT, tree, accessionMapping, sequenceSupplier);
            }
        } catch (IOException e) {
            settings.logFileWriter.writeLog(e.toString());
            throw new RuntimeException(e);
        }

        // write final log entry
        settings.logFileWriter.writeTimeStamp("Preprocessing finished");
        settings.logFileWriter.writeLog(runInfo);
    }
}
