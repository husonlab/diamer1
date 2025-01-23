package org.husonlab.diamer2.main;

import org.apache.commons.cli.*;
import org.husonlab.diamer2.indexing.DBIndexer;
import org.husonlab.diamer2.indexing.ReadIndexer;
import org.husonlab.diamer2.io.Utilities;
import org.husonlab.diamer2.io.accessionMapping.AccessionMapping;
import org.husonlab.diamer2.io.accessionMapping.MeganMapping;
import org.husonlab.diamer2.io.accessionMapping.NCBIMapping;
import org.husonlab.diamer2.io.seq.SequenceSupplier;
import org.husonlab.diamer2.io.taxonomy.TreeIO;
import org.husonlab.diamer2.readAssignment.AssignmentStatistics;
import org.husonlab.diamer2.readAssignment.algorithms.OVO;
import org.husonlab.diamer2.readAssignment.ReadAssigner;
import org.husonlab.diamer2.io.indexing.DBIndexIO;
import org.husonlab.diamer2.io.ReadAssignmentIO;
import org.husonlab.diamer2.main.encodingSettings.EncodingSettings;
import org.husonlab.diamer2.main.encodingSettings.K15Base11;
import org.husonlab.diamer2.taxonomy.Tree;
import org.husonlab.diamer2.io.NCBIReader;
import org.husonlab.diamer2.readAssignment.ReadAssignment;
import org.husonlab.diamer2.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.*;
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
                        .desc("Preprocess Sequence database")
                        .build()
        );
        computationOptions.addOption(
                Option.builder()
                        .longOpt("indexdb")
                        .desc("DBIndexIO the database")
                        .build()
        );
        computationOptions.addOption(
                Option.builder()
                        .longOpt("indexreads")
                        .desc("DBIndexIO the reads")
                        .build()
        );
        computationOptions.addOption(
                Option.builder()
                        .longOpt("assignreads")
                        .desc("Assign the reads")
                        .build()
        );
        computationOptions.addOption(
                Option.builder()
                        .longOpt("statistics")
                        .desc("Compute statistics")
                        .build()
        );
        computationOptions.addOption(
                Option.builder()
                        .longOpt("index_statistics")
                        .desc("Compute statistics of an index")
                        .build()
        );
        computationOptions.addOption(
                Option.builder()
                        .longOpt("debug")
                        .build()
        );
        options.addOptionGroup(computationOptions);
        options.addOption(
                Option.builder("t")
                        .longOpt("threads")
                        .argName("Number")
                        .desc("Number of threads")
                        .hasArgs()
                        .type(Integer.class)
                        .converter((Converter<Integer, NumberFormatException>) Integer::parseInt)
                        .build()
        );
        options.addOption(
                Option.builder("m")
                        .longOpt("memory")
                        .argName("Number")
                        .desc("Memory in GB")
                        .hasArgs()
                        .type(Integer.class)
                        .converter((Converter<Integer, NumberFormatException>) Integer::parseInt)
                        .build()
        );
        options.addOption(
                Option.builder("b")
                        .longOpt("buckets")
                        .argName("Number")
                        .desc("Number of buckets to process in one cycle.")
                        .hasArg()
                        .type(Integer.class)
                        .build()
        );
        options.addOption(
                Option.builder("no")
                        .longOpt("nodes")
                        .argName("File")
                        .desc("NCBI taxonomy nodes.dmp file")
                        .hasArg()
                        .type(File.class)
                        .build()
        );
        options.addOption(
                Option.builder("na")
                        .longOpt("names")
                        .argName("File")
                        .desc("NCBI taxonomy names.dmp file")
                        .hasArg()
                        .type(File.class)
                        .build()
        );
        options.addOption(
                Option.builder("ac")
                        .longOpt("accessions")
                        .argName("File")
                        .desc("NCBI accession mapping file(s)")
                        .hasArg()
                        .numberOfArgs(Option.UNLIMITED_VALUES)
                        .type(File.class)
                        .build()
        );
        options.addOption(
                Option.builder("d")
                        .longOpt("database")
                        .argName("File")
                        .desc("Database file in fastA format")
                        .hasArgs()
                        .type(File.class)
                        .build()
        );
        options.addOption(
                Option.builder("o")
                        .longOpt("output")
                        .argName("Path")
                        .desc("Output path")
                        .hasArg()
                        .type(Path.class)
                        .build()
        );
        options.addOption(
                Option.builder()
                        .longOpt("mappings")
                        .argName("File,Integer,Integer;File,Integer,Integer...")
                        .desc("Mapping file(s) and columns of accession and taxid.")
                        .hasArg()
                        .build()
        );
        HelpFormatter helpFormatter = new HelpFormatter();

        if (args.length == 0 || args[0].equals("-h") || args[0].equals("--help")) {
            helpFormatter.printHelp("diamer2", options);
            System.exit(0);
        }

        CommandLine cli = null;
        try {
            CommandLineParser parser = new DefaultParser();
            cli = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            helpFormatter.printHelp("diamer2", options);
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
            e.printStackTrace();
        }

        int maxMemory = (int) (Runtime.getRuntime().maxMemory() * 1e-9);
        try {
            Integer parsedMemory = cli.getParsedOptionValue("m");
            if (parsedMemory != null) {
                maxMemory = parsedMemory;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if (cli.hasOption("preprocess")) {
            System.out.println("Preprocessing");
            try {
                File nodes = cli.getParsedOptionValue("no");
                File names = cli.getParsedOptionValue("na");
                File database = cli.getParsedOptionValue("d");
                File output = new File(cli.getOptionValue("o"));
                String mappings = cli.getOptionValue("mappings");
                Tree tree = NCBIReader.readTaxonomy(nodes, names);
                AccessionMapping accessionMapping;
                Pair<HashSet<String>, SequenceSupplier<String, Character>> result = NCBIReader.extractNeededAccessions(database);
                if (mappings.contains(",")) {
                    ArrayList<NCBIMapping.NCBIMappingFile> accessionMappings = getNcbiMappingFiles(mappings);
                    accessionMapping = new NCBIMapping(accessionMappings.toArray(new NCBIMapping.NCBIMappingFile[0]), tree, result.first());
                } else {
                    accessionMapping = new MeganMapping(new File(mappings));
                }
                NCBIReader.preprocessNR(output, tree, accessionMapping, result.last());
            } catch (IOException | ParseException e) {
                e.printStackTrace();
                System.exit(1);
            }
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
                EncodingSettings encodingSettings = new K15Base11(mask, 22);
                DBIndexer dbIndexer = new DBIndexer(database, output, tree, encodingSettings, maxThreads, 2*maxThreads, 10000, bucketsPerCycle, false);
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
                EncodingSettings encodingSettings = new K15Base11(mask, 22);
                ReadIndexer readIndexer = new ReadIndexer(reads, output, encodingSettings, maxThreads, 2*maxThreads, 1000, bucketsPerCycle);
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

    @NotNull
    private static ArrayList<NCBIMapping.NCBIMappingFile> getNcbiMappingFiles(String mappings) {
        ArrayList<NCBIMapping.NCBIMappingFile> accessionMappings = new ArrayList<>();
        for (String mapping : mappings.split(";")) {
            String[] parts = mapping.split(",");
            String mappingFile = parts[0];
            int accessionColumn = Integer.parseInt(parts[1]);
            int taxidColumn = Integer.parseInt(parts[2]);
            accessionMappings.add(new NCBIMapping.NCBIMappingFile(new File(mappingFile), accessionColumn, taxidColumn));
        }
        return accessionMappings;
    }
}