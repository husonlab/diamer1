package org.husonlab.diamer2.main;

import org.apache.commons.cli.*;
import org.husonlab.diamer2.alphabet.DNAEncoder;
import org.husonlab.diamer2.alphabet.DNAKmerEncoder;
import org.husonlab.diamer2.graph.Tree;
import org.husonlab.diamer2.indexing.Indexer;
import org.husonlab.diamer2.io.NCBIReader;

import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;


public class Main {
    public static void main(String[] args) {
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
                        .desc("Index the database")
                        .build()
        );
        computationOptions.addOption(
                Option.builder()
                        .longOpt("indexreads")
                        .desc("Index the reads")
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
                    .hasArg()
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

        int maxMemory = (int)(Runtime.getRuntime().maxMemory() * 1e-9);
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
                Indexer indexer = new Indexer(tree, maxThreads, 1000, 100, bucketsPerCycle);
                indexer.indexDB(database, output);
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

                Indexer indexer = new Indexer(null, maxThreads, 1000, 100, bucketsPerCycle);
                indexer.indexReads(reads, output);
            } catch (ParseException | NullPointerException | IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        } else if (cli.hasOption("debug")) {
            System.out.println("Debugging");
            DNAKmerEncoder kmerEncoder = new DNAKmerEncoder(4, "TT");
            String dnaSequence = "TCAATGTCTT";
            for (int i = 0; i < dnaSequence.length(); i++) {
                System.out.println(kmerEncoder.addNucleotide(dnaSequence.charAt(i)).getEncodedKmers());
            }
            System.out.println(kmerEncoder.getEncodedKmers());
        }
    }
}