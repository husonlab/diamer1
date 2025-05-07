package org.husonlab.diamer2.main;

import org.apache.commons.cli.*;
import org.husonlab.diamer2.io.taxonomy.TreeIO;
import org.husonlab.diamer2.readAssignment.algorithms.AssignmentAlgorithm;
import org.husonlab.diamer2.readAssignment.algorithms.OVA;
import org.husonlab.diamer2.readAssignment.algorithms.OVO;
import org.husonlab.diamer2.readAssignment.ReadAssigner;
import org.husonlab.diamer2.io.ReadAssignmentIO;
import org.husonlab.diamer2.main.encoders.Encoder;
import org.husonlab.diamer2.seq.alphabet.*;
import org.husonlab.diamer2.readAssignment.ReadAssignment;
import org.husonlab.diamer2.util.DBIndexAnalyzer;

import java.io.*;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
                                diamond (default) 
                                
                                Encoding: DIAMOND's base 11 alphabet [BDEKNOQRXZ][AST][IJLV][G][P][F][Y][CU][H][M][W]
                                
                                -
                                
                                base11uniform
                                
                                Encoding: a base 11 alphabet in which the likelihood of each amino acid is about the
                                same [L][A][GC][VWUBIZO][SH][EMX][TY][RQ][DN][IF][PK]
                                
                                -
                                
                                base11nuc
                                
                                For nucleotide databases
                                
                                Encoding: DIAMOND's base 11 alphabet but with stop codons encoded as 0.
                                [BDEKNOQRXZ*][AST][IJLV][G][P][F][Y][CU][H][M][W]
                                
                                -
                                
                                custom
                                
                                Encoding: user-defined alphabet""")
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

        // parse arguments or set default values
        GlobalSettings globalSettings = new GlobalSettings(args, cli, options);

        // prepare additional options for the different tasks and start them
        if (cli.hasOption("preprocess")) {
            Preprocessing.preprocess(cli, globalSettings);
        } else if (cli.hasOption("indexdb")) {
            DBIndexing.indexDB(cli, globalSettings);
        } else if (cli.hasOption("indexreads")) {
            ReadIndexing.indexReads(cli, globalSettings);
        } else if (cli.hasOption("assignreads")) {
            assignreads(globalSettings, cli);
        } else if (cli.hasOption("analyze-db-index")) {
            analyzeDBIndex(globalSettings, cli);
        } else {
            System.err.println("No computation option selected");
            CliUtils.printHelp(options);
            System.exit(1);
        }
    }

    private static void assignreads(GlobalSettings globalSettings, CommandLine cli) {
        CliUtils.checkNumberOfPositionalArguments(cli, 3);
        boolean[] mask = CliUtils.getMask(cli);
        Path dbIndex = getFolder(cli.getArgs()[0], true);
        Path readsIndex = getFolder(cli.getArgs()[1], true);
        Path output = getFolder(cli.getArgs()[2], false);
        List<AssignmentAlgorithm> algorithms = new ArrayList<>();
        if (cli.hasOption("ovo")) {
            String[] thresholds = cli.getOptionValue("ovo").split(",");
            for (String threshold : thresholds) {
                algorithms.add(new OVO(Float.parseFloat(threshold)));
            }
        } else {
            algorithms.add(new OVO(0.7f));
        }
        if (cli.hasOption("ova")) {
            String[] thresholds = cli.getOptionValue("ova").split(",");
            for (String threshold : thresholds) {
                algorithms.add(new OVA(Float.parseFloat(threshold)));
            }
        } else {
            algorithms.add(new OVA(0.7f));
        }
        writeLogBegin(globalSettings, output.resolve("run.log"));

        ReadAssignment readAssignment = null;

        String runInfo = "";
        ReducedAlphabet alphabet = CliUtils.getAlphabet(cli);
        Encoder encoder = new Encoder(alphabet,
                dbIndex, readsIndex, mask, null, globalSettings.BITS_FOR_IDS);
        ReadAssigner readAssigner = new ReadAssigner(encoder, globalSettings);

        try {
            runInfo = readAssigner.assignReads();
            readAssignment = readAssigner.getReadAssignment();
        } catch (Exception e) {
            writeLogEnd(e.toString(), output.resolve("run.log"));
            throw new RuntimeException(e);
        }

        ReadAssignmentIO.writeRawAssignment(readAssignment, output.resolve("raw_assignments.tsv"));
        readAssignment.addKmerCountsToTree();
        readAssignment.normalizeKmerCounts();

        for (AssignmentAlgorithm algorithm : algorithms) {
            readAssignment.runAssignmentAlgorithmOnKmerCounts(algorithm);
            readAssignment.runAssignmentAlgorithmOnNormalizedKmerCounts(algorithm);
        }
        readAssignment.addReadCountsToTree();

        runInfo = runInfo + "\n\n" + ReadAssignmentIO.writePerReadAssignments(readAssignment, output.resolve("per_read_assignments.tsv"), false, true, globalSettings);
        TreeIO.savePerTaxonAssignment(readAssignment.getTree(), output.resolve("per_taxon_assignments.tsv"));
        TreeIO.saveForMegan(readAssignment.getTree(), output.resolve("megan.tsv"), List.of(new String[]{"kmer count"}), List.of(new String[0]));
        writeLogEnd(runInfo, output.resolve("run.log"));
    }
    
    private static void analyzeDBIndex(GlobalSettings globalSettings, CommandLine cli) {
        CliUtils.checkNumberOfPositionalArguments(cli, 2);
        Path dbIndex = getFolder(cli.getArgs()[0], true);
        Path output = getFolder(cli.getArgs()[1], false);
        writeLogBegin(globalSettings, output.resolve("db-analyze-run.log"));

        boolean[] mask = CliUtils.getMask(cli);
        ReducedAlphabet alphabet = CliUtils.getAlphabet(cli);
        Encoder encoder = new Encoder(alphabet, dbIndex, null, mask, null, globalSettings.BITS_FOR_IDS);
        DBIndexAnalyzer dbIndexAnalyzer = new DBIndexAnalyzer(output, encoder, globalSettings);
        String runInfo = dbIndexAnalyzer.analyze();
        writeLogEnd(runInfo, output.resolve("db-analyze-run.log"));
    }

    /**
     * Write a logfile with the command line arguments, version and date.
     */
    private static void writeLogBegin(GlobalSettings settings, Path out) {
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(out.toFile())))) {
            writer.print(settings.toString());
            writer.println("Start: " + LocalDateTime.now());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Append the end time to a logfile.
     */
    private static void writeLogEnd(String runInfo, Path out) {
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(out.toFile(), true)))) {
            writer.println("End: " + LocalDateTime.now());
            writer.println();
            writer.print(runInfo);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}