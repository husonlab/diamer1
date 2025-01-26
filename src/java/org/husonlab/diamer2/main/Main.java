package org.husonlab.diamer2.main;

import org.apache.commons.cli.*;
import org.husonlab.diamer2.indexing.DBIndexer;
import org.husonlab.diamer2.indexing.ReadIndexer;
import org.husonlab.diamer2.io.Utilities;
import org.husonlab.diamer2.io.accessionMapping.AccessionMapping;
import org.husonlab.diamer2.io.accessionMapping.MeganMapping;
import org.husonlab.diamer2.io.accessionMapping.NCBIMapping;
import org.husonlab.diamer2.io.seq.FastaReader;
import org.husonlab.diamer2.io.seq.SequenceSupplier;
import org.husonlab.diamer2.io.taxonomy.TreeIO;
import org.husonlab.diamer2.readAssignment.algorithms.OVO;
import org.husonlab.diamer2.readAssignment.ReadAssigner;
import org.husonlab.diamer2.io.indexing.DBIndexIO;
import org.husonlab.diamer2.io.ReadAssignmentIO;
import org.husonlab.diamer2.main.encoders.Encoder;
import org.husonlab.diamer2.main.encoders.K15Base11;
import org.husonlab.diamer2.taxonomy.Tree;
import org.husonlab.diamer2.io.NCBIReader;
import org.husonlab.diamer2.readAssignment.ReadAssignment;

import java.io.*;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;


public class Main {

    public static void main(String[] args) {

        long mask = 0b11111101101100111000100001L; // longspaced
//        long mask = 0b111111111111111L;

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

        // default values
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
        GlobalSettings globalSettings = new GlobalSettings(maxThreads, maxMemory, false);

        if (cli.hasOption("preprocess")) {
            preprocess(globalSettings, cli);
        } else if (cli.hasOption("indexdb")) {
            if (!cli.hasOption("no") || !cli.hasOption("na") || !cli.hasOption("d")) {
                System.err.println("Missing NCBI nodes and names files for indexing database task.");
                System.exit(1);
            }
            System.out.println("Indexing database");
            try {
                int bucketsPerCycle = cli.getParsedOptionValue("b");
                File nodes = cli.getParsedOptionValue("no");
                File names = cli.getParsedOptionValue("na");
                File database = cli.getParsedOptionValue("d");
                Path output = cli.getParsedOptionValue("o");

                if (!nodes.exists() || !names.exists() || !database.exists()) {
                    System.err.println("One or more required files do not exist.");
                    System.exit(1);
                }
                Tree tree = NCBIReader.readTaxonomy(nodes, names);
                Encoder encoder = new K15Base11(mask, 22);
                DBIndexer dbIndexer = new DBIndexer(database, output, tree, encoder, maxThreads, 2*maxThreads, 10000, bucketsPerCycle, false);
                dbIndexer.index();
            } catch (ParseException | NullPointerException | IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        } else if (cli.hasOption("indexreads")) {
            System.out.println("Indexing reads");
            try {
                int bucketsPerCycle = cli.getParsedOptionValue("b");
                File reads = cli.getParsedOptionValue("d");
                Path output = cli.getParsedOptionValue("o");
                Encoder encoder = new K15Base11(mask, 22);
                ReadIndexer readIndexer = new ReadIndexer(reads, output, encoder, maxThreads, 2*maxThreads, 1000, bucketsPerCycle);
                readIndexer.index();
            } catch (ParseException | NullPointerException | IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        } else if (cli.hasOption("assignreads")) {
            System.out.println("Assigning reads");
            try {
                File nodes = cli.getParsedOptionValue("no");
                File names = cli.getParsedOptionValue("na");
                Tree tree = NCBIReader.readTaxonomy(nodes, names);
//              tree.reduceToStandardRanks();
                String[] paths = cli.getOptionValues("d");
                Path dbIndex = Path.of(paths[0]);
                Path readsIndex = Path.of(paths[1]);
                Path output = Path.of(cli.getOptionValue("o"));
                ReadAssigner readAssigner = new ReadAssigner(tree, maxThreads, dbIndex, readsIndex, new K15Base11(mask, 22));
                ReadAssignment assignment = readAssigner.assignReads();
                ReadAssignmentIO.writeRawAssignments(assignment, output.resolve("raw_assignments.tsv").toFile());
                assignment.addKmerCounts();
                assignment.runAssignmentAlgorithm(new OVO(tree, 0.2f));
//                assignment.runAssignmentAlgorithm(new OVO(tree, 0.5f));
//                assignment.runAssignmentAlgorithm(new OVO(tree, 0.7f));
                assignment.runAssignmentAlgorithm(new OVO(tree, 0.8f));
                TreeIO.saveCustomValues(tree, 1, output.resolve("custom_values.tsv"), true);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        } else if (cli.hasOption("statistics")) {
            System.out.println("Statistics");
            try {
                File nodes = cli.getParsedOptionValue("no");
                File names = cli.getParsedOptionValue("na");
                File file = new File(cli.getOptionValue("d"));
                Path path = Path.of(cli.getOptionValue("o"));
                Utilities.checkFilesAndFolders(new File[]{}, new Path[]{path});
                Tree tree = NCBIReader.readTaxonomy(nodes, names);
                tree.reduceToStandardRanks();
                ReadAssignment readAssignment = ReadAssignmentIO.read(tree, file);
                readAssignment.runAssignmentAlgorithm(new OVO(tree, 0.5f));
//                AssignmentStatistics assignmentStatistics = readAssignment.calculateStatistics();
//                ReadAssignmentIO.writeAssignmentStatistics(assignmentStatistics, path);
                System.out.println();
//                ReadAssignmentIO.writeAssignments(readAssignment, path.resolve("assignments.tsv").toFile());
//                ReadAssignmentIO.writeReadStatistics(readAssignment, path);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else if (cli.hasOption("index_statistics")) {
            try {
                File nodes = cli.getParsedOptionValue("no");
                File names = cli.getParsedOptionValue("na");
                File dbIndex = new File(cli.getOptionValue("d"));
                File output = new File(cli.getOptionValue("o"));
                Tree tree = NCBIReader.readTaxonomy(nodes, names);
                DBIndexIO DBIndexIO = new DBIndexIO(dbIndex.toPath());
                org.husonlab.diamer2.indexing.Utilities.analyzeDBIndex(DBIndexIO, tree, output.toPath(), 1024, maxThreads);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else if (cli.hasOption("debug")) {
            System.out.println("Debugging");
        }
    }

    private static void preprocess(GlobalSettings globalSettings, CommandLine cli) {
        try {
            checkNodesAndNames(cli);
            Path nodes = getFile(cli.getOptionValue("no"), true);
            Path names = getFile(cli.getOptionValue("na"), true);
            checkNumberOfPositionalArguments(cli, 3);
            Path database = getFile(cli.getArgs()[0], true);
            Path output = getFile(cli.getArgs()[1], false);

            AccessionMapping accessionMapping;
            ArrayList<Path> mappingFiles = new ArrayList<>();
            for (int i = 2; i < cli.getArgs().length; i++) {
                mappingFiles.add(getFile(cli.getArgs()[i], true));
            }
            try (SequenceSupplier<String, Character> sequenceSupplier = new SequenceSupplier<>(
                    new FastaReader(database.toFile()), null, globalSettings.KEEP_IN_MEMORY)) {
                Tree tree = NCBIReader.readTaxonomy(nodes.toFile(), names.toFile());
                if (mappingFiles.getFirst().toString().endsWith(".mdb") || mappingFiles.getFirst().toString().endsWith(".db")) {
                    accessionMapping = new MeganMapping(mappingFiles.getFirst());
                } else {
                    HashSet<String> neededAccessions = NCBIReader.extractNeededAccessions(sequenceSupplier);
                    accessionMapping = new NCBIMapping(
                            mappingFiles,
                            tree,
                            neededAccessions);
                }
                NCBIReader.preprocessNRBuffered(output, tree, accessionMapping, sequenceSupplier);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void printHelp(Options options) {
        HelpFormatter helpFormatter = new HelpFormatter();
        // disable alphabetical sorting of options
        helpFormatter.setOptionComparator(null);
        helpFormatter.printHelp("diamer2 {--preprocess | --indexdb | --assignreads | --statistics} " +
                "[options] <input> <output>", options);
    }

    private static void checkNumberOfPositionalArguments(CommandLine cli, int minExpected) {
        if (cli.getArgs().length < minExpected) {
            System.err.printf("Expected %d positional arguments, got %d\n", minExpected, cli.getArgs().length);
            System.exit(1);
        }
    }

    private static void checkNodesAndNames(CommandLine cli) {
        if (!cli.hasOption("no") || !cli.hasOption("na")) {
            System.err.println("At least one of the required NCBI taxonomy files is missing: " +
                    "nodes.dmp (option -no), names.dmp (option -na)");
            System.exit(1);
        }
    }

    private static Path getFile(String path, boolean exists) {
        Path result = null;
        try {
            result = Path.of(path);
            if (exists && !result.toFile().exists()) {
                System.err.printf("File \"%s\" does not exist\n", path);
                System.exit(1);
            }
        } catch (InvalidPathException e) {
            System.err.printf("Invalid path: \"%s\"\n", path);
            System.exit(1);
        }
        return result;
    }
}