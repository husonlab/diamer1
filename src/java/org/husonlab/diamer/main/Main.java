package org.husonlab.diamer.main;

import org.apache.commons.cli.*;
import org.husonlab.diamer.main.Computations.*;

import java.nio.file.Path;

import static org.husonlab.diamer.io.Utilities.getFile;
import static org.husonlab.diamer.io.Utilities.getFolder;


public class Main {

    public static void main(String[] args) {

        Options options = new Options();
        OptionGroup computationOptions = new OptionGroup();
        computationOptions.setRequired(true);
        computationOptions.addOption(
                Option.builder()
                        .longOpt("preprocess")
                        .desc("""
                                Preprocess protein sequence database.
                                
                                Required options: -no -na <input> <output> <mapping>
                                
                                <input>: protein database as multi-FASTA
                                
                                <output>: output file
                                
                                <mapping>: accession -> taxid mapping file(s), can be either a MEGAN mapping file
                                 (.db or .mdb) or NCBI accession2taxid mapping file(s).""")
                        .build()
        );
        computationOptions.addOption(
                Option.builder()
                        .longOpt("indexdb")
                        .desc("""
                                Index a preprocessed sequence database.
                                
                                Required options: -no -na <input> <output>
                                
                                <input>: preprocessed database
                                
                                <output>: output path""")
                        .build()
        );
        computationOptions.addOption(
                Option.builder()
                        .longOpt("indexreads")
                        .desc("""
                                Index DNA reads.
                                
                                Required options: <input> <output>
                                
                                <input>: reads in fastQ format
                                
                                <output>: output path""")
                        .build()
        );
        computationOptions.addOption(
                Option.builder()
                        .longOpt("assignreads")
                        .desc("""
                                Assign reads.
                                
                                Required options: <input> <output>
                                
                                <input>: database index folder and reads index folder
                                
                                <output>: output path""")
                        .build()
        );
        computationOptions.addOption(
                Option.builder()
                        .longOpt("analyze-db-index")
                        .desc("""
                                Generate statistics for a DB Index.
                                
                                Required options: <input> <output>
                                
                                <input>: database index folder
                                
                                <output>: output path""")
                        .build()
        );
        options.addOptionGroup(computationOptions);
        options.addOption(
                Option.builder("t")
                        .longOpt("threads")
                        .argName("number")
                        .desc("Number of threads")
                        .hasArg()
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
                        .desc("Mask to use for kmer extraction.\nDefault: 1111111111111")
                        .hasArg()
                        .type(String.class)
                        .build()
        );
        options.addOption(
                Option.builder()
                        .longOpt("debug")
                        .desc("Run in debug mode")
                        .build()
        );
        options.addOption(
                Option.builder()
                        .longOpt("statistics")
                        .desc("Collect statistics.")
                        .build()
        );
        options.addOption(
                Option.builder()
                        .longOpt("only-standard-ranks")
                        .desc("Reduce the taxonomic tree to only include standard ranks" +
                                "(superkingdom, kingdom, phylum, class, order, family, genus, species).")
                        .build()
        );
        options.addOption(
                Option.builder()
                        .longOpt("alphabet")
                        .desc("""
                                Supply a custom reduced amino acid alphabet for the encoding of the sequences.
                                
                                Every letter that is not part of the alphabet will be treated as a stop character
                                (case sensitive!).
                                
                                default: base11uniform
                                
                                a base 11 alphabet in which the likelihood of each amino acid is about the same
                                
                                [L][A][GC][VWUBIZO*][SH][EMX][TY][RQ][DN][IF][PK]
                                """)
                        .hasArg()
                        .type(Path.class)
                        .build()
        );
        options.addOption(
                Option.builder()
                        .longOpt("filtering")
                        .desc("""
                                Choose one of the follwing filtering options for the reads:
                                
                                -
                                
                                COMPLEXITY
                                
                                keep k-mers with more than n unique characters
                                
                                syntax: --filtering c <n>
                                
                                keep complexity maximizer in window of size w
                                
                                syntax: --filtering cm <w>
                                
                                -
                                
                                PROBABILITY
                                
                                keep k-mers below certain probability p (e.g. 1e-12)
                                
                                syntax: --filtering p <p>
                                
                                keep probability minimizer in window of size w
                                
                                syntax: --filtering pm <w>
                                
                                -
                                
                                default --filtering c 3
                                """)
                        .numberOfArgs(2)
                        .type(Path.class)
                        .build()
        );
        options.addOption(
                Option.builder()
                        .longOpt("ovo")
                        .desc("""
                                One Versus One assignment algorithm. Use syntax --ovo 0.7,0.8,0.9,... to run the
                                algorithm with (multiple) different thresholds. (default is 0.7)
                                """)
                        .hasArg()
                        .type(Path.class)
                        .build()
        );
        options.addOption(
                Option.builder()
                        .longOpt("ova")
                        .desc("""
                                One Versus All assignment algorithm. Use syntax --ova 0.7,0.8,0.9,... to run the
                                algorithm with (multiple) different thresholds. (default is 0.7)
                                """)
                        .hasArg()
                        .type(Path.class)
                        .build()
        );

        // print help message
        if (args.length == 0 || args[0].equals("-h") || args[0].equals("--help")) {
            CliUtils.printHelp(options);
            System.exit(0);
        }

        // parse command line arguments
        CommandLine cli = null;
        try {
            CommandLineParser parser = new DefaultParser();
            cli = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        // prepare additional options for the different tasks and start them
        if (cli.hasOption("preprocess")) {
            CliUtils.checkNumberOfPositionalArguments(cli, 3);
            Path output = getFile(cli.getArgs()[1], false);
            // ensure writing a gzipped output file
            if (!output.toString().endsWith(".gz")) {
                output = output.getParent().resolve(output.getFileName() + ".gz");
            }
            GlobalSettings globalSettings = new GlobalSettings(args, cli, options, output, output.getParent().resolve("run.log"));
            Preprocessing.preprocess(cli, globalSettings);
        } else if (cli.hasOption("indexdb")) {
            CliUtils.checkNumberOfPositionalArguments(cli, 2);
            Path output = getFolder(cli.getArgs()[1], false);
            GlobalSettings globalSettings = new GlobalSettings(args, cli, options, output);
            DBIndexing.indexDB(cli, globalSettings);
        } else if (cli.hasOption("indexreads")) {
            CliUtils.checkNumberOfPositionalArguments(cli, 2);
            Path output = getFolder(cli.getArgs()[1], false);
            GlobalSettings globalSettings = new GlobalSettings(args, cli, options, output);
            ReadIndexing.indexReads(cli, globalSettings);
        } else if (cli.hasOption("assignreads")) {
            CliUtils.checkNumberOfPositionalArguments(cli, 3);
            Path output = getFolder(cli.getArgs()[2], false);
            GlobalSettings globalSettings = new GlobalSettings(args, cli, options, output);
            ReadAssigning.assignReads(cli, globalSettings);
        } else if (cli.hasOption("analyze-db-index")) {
            CliUtils.checkNumberOfPositionalArguments(cli, 2);
            Path output = getFolder(cli.getArgs()[1], false);
            GlobalSettings globalSettings = new GlobalSettings(args, cli, options, output, output.resolve("db-analyze-run.log"));
            DBIndexAnalyzing.analyzeDBIndex(cli, globalSettings);
        } else {
            System.err.println("No computation option selected");
            CliUtils.printHelp(options);
            System.exit(1);
        }
    }
}