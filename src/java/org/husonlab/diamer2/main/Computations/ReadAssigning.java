package org.husonlab.diamer2.main.Computations;

import org.apache.commons.cli.CommandLine;
import org.husonlab.diamer2.indexing.kmers.KmerExtractor;
import org.husonlab.diamer2.io.ReadAssignmentIO;
import org.husonlab.diamer2.io.taxonomy.TreeIO;
import org.husonlab.diamer2.main.CliUtils;
import org.husonlab.diamer2.main.GlobalSettings;
import org.husonlab.diamer2.main.encoders.Encoder;
import org.husonlab.diamer2.main.encoders.EncoderWithoutKmerExtractor;
import org.husonlab.diamer2.readAssignment.ReadAssigner;
import org.husonlab.diamer2.readAssignment.ReadAssignment;
import org.husonlab.diamer2.readAssignment.algorithms.AssignmentAlgorithm;

import java.util.List;

import static org.husonlab.diamer2.io.Utilities.getFolder;

public class ReadAssigning {
    public static void assignReads(CommandLine cli, GlobalSettings settings) {
        // database index, reads index, output folder
        CliUtils.checkNumberOfPositionalArguments(cli, 3);
        settings.DB_INDEX = getFolder(cli.getArgs()[0], true);
        settings.READS_INDEX = getFolder(cli.getArgs()[1], true);

        settings.logFileWriter.writeSettings(settings);
        settings.logFileWriter.writeTimeStamp("Read assignment started");

        // setup read assigner
        Encoder encoder = new EncoderWithoutKmerExtractor(settings);
        ReadAssigner readAssigner = new ReadAssigner(encoder, settings);

        // run assignment
        ReadAssignment readAssignment;
        try {
            String runInfo = readAssigner.assignReads();
            settings.logFileWriter.writeTimeStamp("Read assignment finished");
            settings.logFileWriter.writeLog(runInfo);
            readAssignment = readAssigner.getReadAssignment();
        } catch (Exception e) {
            settings.logFileWriter.writeLog("Read assignment failed: " + e.getMessage());
            throw new RuntimeException(e);
        }

        // write raw kmer matches per read
        ReadAssignmentIO.writeRawAssignment(readAssignment, settings.OUTPUT.resolve("raw_assignments.tsv"));
        // add k-mer counts to the taxonomic nodes to use them for the assignment algorithms
        readAssignment.addKmerCountsToTree();
        // normalize the kmer counts by the number of kmers in the database for each taxon
        readAssignment.normalizeKmerCounts();

        // run assignment algorithms
        for (AssignmentAlgorithm algorithm : settings.ALGORITHMS) {
            readAssignment.runAssignmentAlgorithmOnKmerCounts(algorithm);
            readAssignment.runAssignmentAlgorithmOnNormalizedKmerCounts(algorithm);
        }

        // write assignments per read
        String runInfo = ReadAssignmentIO.writePerReadAssignments(readAssignment, settings.OUTPUT.resolve("per_read_assignments.tsv"), false, true, settings);
        settings.logFileWriter.writeLog(runInfo);

        // add read counts to the taxonomic nodes
        readAssignment.addReadCountsToTree();

        // save the numbers assigned to each taxon by each algorithm
        TreeIO.savePerTaxonAssignment(readAssignment.getTree(), settings.OUTPUT.resolve("per_taxon_assignments.tsv"));
        TreeIO.saveForMegan(readAssignment.getTree(), settings.OUTPUT.resolve("megan.tsv"), List.of(new String[]{"kmer count"}), List.of(new String[0]));
    }

}
