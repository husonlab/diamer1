package org.husonlab.diamer2.main;

import org.apache.commons.cli.*;
import org.husonlab.diamer2.indexing.DBIndexer;
import org.husonlab.diamer2.indexing.ReadIndexer;
import org.husonlab.diamer2.io.accessionMapping.AccessionMapping;
import org.husonlab.diamer2.io.accessionMapping.MeganMapping;
import org.husonlab.diamer2.io.accessionMapping.NCBIMapping;
import org.husonlab.diamer2.io.seq.FastaReader;
import org.husonlab.diamer2.io.seq.SequenceSupplier;
import org.husonlab.diamer2.readAssignment.algorithms.OVO;
import org.husonlab.diamer2.readAssignment.ReadAssigner;
import org.husonlab.diamer2.io.ReadAssignmentIO;
import org.husonlab.diamer2.main.encoders.Encoder;
import org.husonlab.diamer2.main.encoders.K15Base11;
import org.husonlab.diamer2.taxonomy.Tree;
import org.husonlab.diamer2.io.NCBIReader;
import org.husonlab.diamer2.readAssignment.ReadAssignment;
import org.husonlab.diamer2.util.Pair;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;

import static org.husonlab.diamer2.io.Utilities.getFile;
import static org.husonlab.diamer2.io.Utilities.getFolder;


public class Main {

    public static void main(String[] args) {

        Options options = new Options();
        OptionGroup computationOptions = new OptionGroup();
        computationOptions.setRequired(true);
        computationOptions.addOption(
                Option.builder()
                        .longOpt("preprocess")
                        .desc("""
                                Preprocess protein sequence database.\
                                
                                Required options: -no -na <input> <output> <mapping>\
                                
                                <input>: protein database as multi-FASTA\
                                
                                <output>: output file\
                                
                                <mapping>: accession -> taxid mapping file(s), can be either a MEGAN mapping file \
                                (.db or .mdb) or NCBI accession2taxid mapping file(s).""")
                        .build()
        );
        computationOptions.addOption(
                Option.builder()
                        .longOpt("indexdb")
                        .desc("""
                                Index a preprocessed sequence database.\
                                
                                Required options: -no -na <input> <output>\
                                
                                <input>: preprocessed database\
                                
                                <output>: output path""")
                        .build()
        );
        computationOptions.addOption(
                Option.builder()
                        .longOpt("indexreads")
                        .desc("""
                                Index DNA reads.\
                                
                                Required options: <input> <output>\
                                
                                <input>: reads in fastQ format\
                                
                                <output>: output path""")
                        .build()
        );
        computationOptions.addOption(
                Option.builder()
                        .longOpt("assignreads")
                        .desc("""
                                Assign reads.\
                                
                                Required options: -no -na <input> <output>\
                                
                                <input>: database index folder and reads index folder\
                                
                                <output>: output path""")
                        .build()
        );
        options.addOptionGroup(computationOptions);
        options.addOption(
                Option.builder("t")
                        .longOpt("threads")
                        .argName("number")
                        .desc("Number of threads")
                        .hasArgs()
                        .type(Integer.class)
                        .converter((Converter<Integer, NumberFormatException>) Integer::parseInt)
                        .build()
        );
        options.addOption(
                Option.builder("m")
                        .longOpt("memory")
                        .argName("number")
                        .desc("Memory in GB")
                        .hasArgs()
                        .type(Integer.class)
                        .converter((Converter<Integer, NumberFormatException>) Integer::parseInt)
                        .build()
        );
        options.addOption(
                Option.builder("b")
                        .longOpt("buckets")
                        .argName("number")
                        .desc("Number of buckets to process in one cycle.")
                        .hasArg()
                        .type(Integer.class)
                        .build()
        );
        options.addOption(
                Option.builder()
                        .longOpt("keep-in-memory")
                        .desc("If set, keeps the sequence files" +
                                "(database or reads) in memory during indexing and preprocessing.")
                        .build()
        );
        options.addOption(
                Option.builder("no")
                        .longOpt("nodes")
                        .argName("file")
                        .desc("NCBI taxonomy nodes.dmp file")
                        .hasArg()
                        .type(Path.class)
                        .build()
        );
        options.addOption(
                Option.builder("na")
                        .longOpt("names")
                        .argName("file")
                        .desc("NCBI taxonomy names.dmp file")
                        .hasArg()
                        .type(Path.class)
                        .build()
        );
        options.addOption(
                Option.builder()
                        .longOpt("mask")
                        .argName("bitmask")
                        .desc("Mask to use for kmer extraction.\nDefault: 11111101101100111000100001")
                        .hasArg()
                        .type(String.class)
                        .build()
        );

        if (args.length == 0 || args[0].equals("-h") || args[0].equals("--help")) {
            printHelp(options);
            System.exit(0);
        }
        CommandLine cli = null;
        try {
            CommandLineParser parser = new DefaultParser();
            cli = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        // parse arguments or set default values
        int maxThreads = Runtime.getRuntime().availableProcessors();
        try {
            Integer parsedThreads = cli.getParsedOptionValue("t");
            if (parsedThreads != null) {
                maxThreads = parsedThreads;
            }
        } catch (ParseException e) {
            System.err.printf("Invalid number of threads: \"%s\"\n", cli.getOptionValue("t"));
            printHelp(options);
            System.exit(1);
        }
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() * 1e-9);
        try {
            Integer parsedMemory = cli.getParsedOptionValue("m");
            if (parsedMemory != null) {
                maxMemory = parsedMemory;
            }
        } catch (ParseException e) {
            System.err.printf("Invalid amount of memory: \"%s\"\n", cli.getOptionValue("m"));
            printHelp(options);
            System.exit(1);
        }
        GlobalSettings globalSettings = new GlobalSettings(maxThreads, maxMemory, cli.hasOption("keep-in-memory"));

        if (cli.hasOption("preprocess")) {
            preprocess(globalSettings, cli);
        } else if (cli.hasOption("indexdb")) {
            indexdb(globalSettings, cli);
        } else if (cli.hasOption("indexreads")) {
            indexreads(globalSettings, cli);
        } else if (cli.hasOption("assignreads")) {
            assignreads(globalSettings, cli);
        } else {
            System.err.println("No computation option selected");
            printHelp(options);
            System.exit(1);
        }
    }

    private static void preprocess(GlobalSettings globalSettings, CommandLine cli) {
        Pair<Path, Path> nodesAndNames = getNodesAndNames(cli);
        checkNumberOfPositionalArguments(cli, 3);
        Path database = getFile(cli.getArgs()[0], true);
        Path output = getFile(cli.getArgs()[1], false);

        AccessionMapping accessionMapping;
        ArrayList<Path> mappingFiles = new ArrayList<>();
        for (int i = 2; i < cli.getArgs().length; i++) {
            mappingFiles.add(getFile(cli.getArgs()[i], true));
        }
        try (SequenceSupplier<String, Character> sequenceSupplier = new SequenceSupplier<>(
                new FastaReader(database), null, globalSettings.KEEP_IN_MEMORY)) {
            Tree tree = NCBIReader.readTaxonomy(nodesAndNames.first(), nodesAndNames.last());
            if (mappingFiles.getFirst().toString().endsWith(".mdb") || mappingFiles.getFirst().toString().endsWith(".db")) {
                accessionMapping = new MeganMapping(mappingFiles.getFirst());
                NCBIReader.preprocessNRBuffered(output, tree, accessionMapping, sequenceSupplier);
            } else {
                HashSet<String> neededAccessions = NCBIReader.extractNeededAccessions(sequenceSupplier);
                accessionMapping = new NCBIMapping(
                        mappingFiles,
                        tree,
                        neededAccessions);
                NCBIReader.preprocessNR(output, tree, accessionMapping, sequenceSupplier);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void indexdb(GlobalSettings globalSettings, CommandLine cli) {
        Pair<Path, Path> nodesAndNames = getNodesAndNames(cli);
        checkNumberOfPositionalArguments(cli, 2);
        int bucketsPerCycle = cli.hasOption("b") ? Integer.parseInt(cli.getOptionValue("b")) : globalSettings.MAX_THREADS;
        boolean[] mask = getMask(cli);
        Path database = getFile(cli.getArgs()[0], true);
        Path output = getFolder(cli.getArgs()[1], false);

        Tree tree = NCBIReader.readTaxonomy(nodesAndNames.first(), nodesAndNames.last());
        Encoder encoder = new K15Base11(mask, globalSettings.BITS_FOR_IDS);
        DBIndexer dbIndexer = new DBIndexer(
                database, output, tree, encoder, globalSettings.MAX_THREADS, 2 * globalSettings.MAX_THREADS,
                globalSettings.SEQUENCE_BATCH_SIZE, bucketsPerCycle, globalSettings.KEEP_IN_MEMORY, globalSettings.DEBUG);
        try {
            dbIndexer.index();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void indexreads(GlobalSettings globalSettings, CommandLine cli) {
        checkNumberOfPositionalArguments(cli, 2);
        int bucketsPerCycle = cli.hasOption("b") ? Integer.parseInt(cli.getOptionValue("b")) : globalSettings.MAX_THREADS;
        boolean[] mask = getMask(cli);
        Path reads = getFile(cli.getArgs()[0], true);
        Path output = getFolder(cli.getArgs()[1], false);
        Encoder encoder = new K15Base11(mask, 22);
        ReadIndexer readIndexer = new ReadIndexer(
                reads, output, encoder, bucketsPerCycle, globalSettings.MAX_THREADS, 2 * globalSettings.MAX_THREADS,
                globalSettings.SEQUENCE_BATCH_SIZE, globalSettings.KEEP_IN_MEMORY);
        try {
            readIndexer.index();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void assignreads(GlobalSettings globalSettings, CommandLine cli) {
        Pair<Path, Path> nodesAndNames = getNodesAndNames(cli);
        checkNumberOfPositionalArguments(cli, 3);
        boolean[] mask = getMask(cli);
        Path dbIndex = getFolder(cli.getArgs()[0], true);
        Path readsIndex = getFolder(cli.getArgs()[1], true);
        Path output = getFolder(cli.getArgs()[2], false);

        Tree tree = NCBIReader.readTaxonomy(nodesAndNames.first(), nodesAndNames.last());
//              tree.reduceToStandardRanks();
        ReadAssigner readAssigner = new ReadAssigner(
                tree, globalSettings.MAX_THREADS, dbIndex, readsIndex, new K15Base11(mask, 22));
        ReadAssignment assignment = readAssigner.assignReads();
        ReadAssignmentIO.writeRawAssignment(assignment, output.resolve("raw_assignments.tsv"));
        assignment.addKmerCounts();
        assignment.runAssignmentAlgorithm(new OVO(tree, 0.2f));
//                assignment.runAssignmentAlgorithm(new OVO(tree, 0.5f));
//                assignment.runAssignmentAlgorithm(new OVO(tree, 0.7f));
        assignment.runAssignmentAlgorithm(new OVO(tree, 0.8f));
        ReadAssignmentIO.writePerReadAssignments(assignment, output.resolve("per_read_assignments.tsv"), false, true);
        ReadAssignmentIO.writePerTaxonAssignments(assignment, output.resolve("per_taxon_assignments.tsv"), 1, true);
        ReadAssignmentIO.writeForMEGANImport(assignment, output.resolve("megan.tsv"), 1, 0);
    }

    /**
     * Print help message.
     * @param options command line options
     */
    private static void printHelp(Options options) {
        HelpFormatter helpFormatter = new HelpFormatter();
        // disable alphabetical sorting of options
        helpFormatter.setOptionComparator(null);
        helpFormatter.printHelp("diamer2 {--preprocess | --indexdb | --assignreads | --statistics} " +
                "[options] <input> <output>", options);
    }

    /**
     * Check if the number of positional arguments is at least minExpected.
     * @param cli command line arguments
     * @param minExpected minimum number of positional arguments
     */
    private static void checkNumberOfPositionalArguments(CommandLine cli, int minExpected) {
        if (cli.getArgs().length < minExpected) {
            System.err.printf("Expected %d positional arguments, got %d\n", minExpected, cli.getArgs().length);
            System.exit(1);
        }
    }

    /**
     * @param cli command line arguments
     * @return pair of NCBI nodes and names dump files
     */
    private static Pair<Path, Path> getNodesAndNames(CommandLine cli) {
        if (!cli.hasOption("no") || !cli.hasOption("na")) {
            System.err.println("At least one of the required NCBI taxonomy files is missing: " +
                    "nodes.dmp (option -no), names.dmp (option -na)");
            System.exit(1);
        }
        return new Pair<>(getFile(cli.getOptionValue("no"), true), getFile(cli.getOptionValue("na"), true));
    }

    /**
     * @param cli command line arguments
     * @return mask from cli or default mask
     */
    private static boolean[] getMask(CommandLine cli) {
        return cli.hasOption("mask") ? parseMask(cli.getOptionValue("mask")) : parseMask("11111101101100111000100001");
    }

    /**
     * Parse a mask string with 1 and 0 into a boolean array.
     * @param mask mask string
     * @return boolean array
     */
    private static boolean[] parseMask(String mask) {
        mask = mask.replaceAll("^0+", "").replaceAll("0+$", "");
        boolean[] result = new boolean[mask.length()];
        for (int i = 0; i < mask.length(); i++) {
            result[i] = mask.charAt(i) == '1';
        }
        return result;
    }
}