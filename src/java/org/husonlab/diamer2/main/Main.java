package org.husonlab.diamer2.main;

import org.apache.commons.cli.*;
import org.husonlab.diamer2.indexing.DBIndexer;
import org.husonlab.diamer2.indexing.ReadIndexer;
import org.husonlab.diamer2.io.accessionMapping.AccessionMapping;
import org.husonlab.diamer2.io.accessionMapping.MeganMapping;
import org.husonlab.diamer2.io.accessionMapping.NCBIMapping;
import org.husonlab.diamer2.io.seq.FastaReader;
import org.husonlab.diamer2.io.seq.SequenceSupplier;
import org.husonlab.diamer2.io.taxonomy.TreeIO;
import org.husonlab.diamer2.main.encoders.K15Base11Nuc;
import org.husonlab.diamer2.main.encoders.K15Base11Uniform;
import org.husonlab.diamer2.readAssignment.algorithms.AssignmentAlgorithm;
import org.husonlab.diamer2.readAssignment.algorithms.OVA;
import org.husonlab.diamer2.readAssignment.algorithms.OVO;
import org.husonlab.diamer2.readAssignment.ReadAssigner;
import org.husonlab.diamer2.io.ReadAssignmentIO;
import org.husonlab.diamer2.main.encoders.Encoder;
import org.husonlab.diamer2.main.encoders.K15Base11;
import org.husonlab.diamer2.seq.alphabet.*;
import org.husonlab.diamer2.seq.converter.EnforceAA;
import org.husonlab.diamer2.taxonomy.Tree;
import org.husonlab.diamer2.io.NCBIReader;
import org.husonlab.diamer2.readAssignment.ReadAssignment;
import org.husonlab.diamer2.util.Pair;

import java.io.*;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
                                
                                Required options: <input> <output>\
                                
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
                        .hasArg()
                        .type(Integer.class)
                        .converter((Converter<Integer, NumberFormatException>) Integer::parseInt)
                        .build()
        );
        options.addOption(
                Option.builder("m")
                        .longOpt("memory")
                        .argName("number")
                        .desc("Memory in GB")
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
                        .desc("Mask to use for kmer extraction.\nDefault: 11111101101100111000100001")
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
                        .longOpt("encoder")
                        .argName("encoder")
                        .desc("""
                                base11 (default) \
                                
                                DB: protein\
                                
                                Encoding: DIAMOND's base 11 alphabet with a maximum kmer weight of 15\
                                
                                -
                                
                                base11uniform\
                                
                                DB: protein\
                                
                                Encoding: a base 11 alphabet in which the likelihood of each amino acid is about the same\
                                
                                -
                                
                                base11nuc\
                                
                                DB: nucleotide\
                                
                                Encoding: DIAMOND's base 11 alphabet but with stop codons encoded as 0.""")
                        .hasArg()
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
        GlobalSettings globalSettings = parseGlobalSettings(args, cli, options);

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

    private static GlobalSettings parseGlobalSettings(String[] args, CommandLine cli, Options options) {
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
        int bucketsPerCycle = cli.hasOption("b") ? Integer.parseInt(cli.getOptionValue("b")) : maxThreads;
        return new GlobalSettings(
                args, maxThreads, bucketsPerCycle, maxMemory, cli.hasOption("keep-in-memory"), cli.hasOption("debug"), cli.hasOption("statistics"));
    }

    private static void preprocess(GlobalSettings globalSettings, CommandLine cli) {
        Pair<Path, Path> nodesAndNames = getNodesAndNames(cli);
        checkNumberOfPositionalArguments(cli, 3);
        Path database = getFile(cli.getArgs()[0], true);
        Path output = getFile(cli.getArgs()[1], false);
        if (!output.toString().endsWith(".gz")) {
            output = output.getParent().resolve(output.getFileName() + ".gz");
        }
        writeLogBegin(globalSettings, output.getParent().resolve("run.log"));
        String runInfo;

        AccessionMapping accessionMapping;
        ArrayList<Path> mappingFiles = new ArrayList<>();
        for (int i = 2; i < cli.getArgs().length; i++) {
            mappingFiles.add(getFile(cli.getArgs()[i], true));
        }
        try (SequenceSupplier<String, Character, AAWithLowerAndStop, Character, AA> sequenceSupplier = new SequenceSupplier<>(
                new FastaReader<>(database, new AAWithLowerAndStop()), new EnforceAA(), globalSettings.KEEP_IN_MEMORY)) {
            Tree tree = NCBIReader.readTaxonomy(nodesAndNames.first(), nodesAndNames.last(), true);
            if (mappingFiles.getFirst().toString().endsWith(".mdb") || mappingFiles.getFirst().toString().endsWith(".db")) {
                accessionMapping = new MeganMapping(mappingFiles.getFirst());
                runInfo = NCBIReader.preprocessNRBuffered(output, tree, accessionMapping, sequenceSupplier);
            } else {
                HashMap<String, Integer> accession2Taxid = NCBIReader.extractAccessions(sequenceSupplier);
                accessionMapping = new NCBIMapping(
                        mappingFiles,
                        tree,
                        accession2Taxid);
                sequenceSupplier.reset();
                runInfo = NCBIReader.preprocessNR(output, tree, accessionMapping, sequenceSupplier);
            }
        } catch (IOException e) {
            writeLogEnd(e.toString(), output.getParent().resolve("run.log"));
            throw new RuntimeException(e);
        }
        writeLogEnd(runInfo, output.getParent().resolve("run.log"));
    }

    private static void indexdb(GlobalSettings globalSettings, CommandLine cli) {
        Pair<Path, Path> nodesAndNames = getNodesAndNames(cli);
        checkNumberOfPositionalArguments(cli, 2);
        boolean[] mask = getMask(cli);
        Path database = getFile(cli.getArgs()[0], true);
        Path output = getFolder(cli.getArgs()[1], false);
        writeLogBegin(globalSettings, output.resolve("run.log"));

        // parse tree
        Tree tree;
        try {
            tree = NCBIReader.readTaxonomy(nodesAndNames.first(), nodesAndNames.last(), true);
        } catch (Exception e) {
            writeLogEnd(e.toString(), output.resolve("run.log"));
            throw new RuntimeException(e);
        }
        // run indexing with specified encoder
        if (!cli.hasOption("encoder") || cli.getOptionValue("encoder").equals("base11")) {
            Encoder<Character, AA, Character, DNA, Base11Alphabet> encoder = new K15Base11(
                    database, null, null, null, mask, globalSettings.BITS_FOR_IDS);
            try (SequenceSupplier<Integer, Character, AA, Byte, Base11Alphabet> sup = new SequenceSupplier<>(
                    encoder.getDBReader(), encoder.getDBConverter(), globalSettings.KEEP_IN_MEMORY)) {
                DBIndexer<Base11Alphabet> dbIndexer = new DBIndexer<>(sup, output, tree, encoder, globalSettings);
                String runInfo = dbIndexer.index();
                writeLogEnd(runInfo, output.resolve("run.log"));
            } catch (Exception e) {
                writeLogEnd(e.toString(), output.resolve("run.log"));
                throw new RuntimeException(e);
            }
        } else if (cli.getOptionValue("encoder").equals("base11uniform")) {
            Encoder<Character, AA, Character, DNA, Base11Uniform> encoder = new K15Base11Uniform(
                    database, null, null, null, mask, globalSettings.BITS_FOR_IDS);
            try (SequenceSupplier<Integer, Character, AA, Byte, Base11Uniform> sup = new SequenceSupplier<>(
                    encoder.getDBReader(), encoder.getDBConverter(), globalSettings.KEEP_IN_MEMORY)) {
                DBIndexer<Base11Uniform> dbIndexer = new DBIndexer<>(sup, output, tree, encoder, globalSettings);
                String runInfo = dbIndexer.index();
                writeLogEnd(runInfo, output.resolve("run.log"));
            } catch (Exception e) {
                writeLogEnd(e.toString(), output.resolve("run.log"));
                throw new RuntimeException(e);
            }
        } else if (cli.getOptionValue("encoder").equals("base11nuc")) {
            Encoder<Character, DNA, Character, DNA, Base11WithStop> encoder = new K15Base11Nuc(
                    database, null, null, null, mask, globalSettings.BITS_FOR_IDS);
            try (SequenceSupplier<Integer, Character, DNA, Byte, Base11WithStop> sup = new SequenceSupplier<>(
                    encoder.getDBReader(), encoder.getDBConverter(), globalSettings.KEEP_IN_MEMORY)) {
                DBIndexer<Base11WithStop> dbIndexer = new DBIndexer<>(sup, output, tree, encoder, globalSettings);
                String runInfo = dbIndexer.index();
                writeLogEnd(runInfo, output.resolve("run.log"));
            } catch (Exception e) {
                writeLogEnd(e.toString(), output.resolve("run.log"));
                throw new RuntimeException(e);
            }

        } else {
            System.err.println("Invalid encoder: " + cli.getOptionValue("encoder"));
            System.exit(1);
        }
    }

    private static void indexreads(GlobalSettings globalSettings, CommandLine cli) {
        checkNumberOfPositionalArguments(cli, 2);
        boolean[] mask = getMask(cli);
        Path reads = getFile(cli.getArgs()[0], true);
        Path output = getFolder(cli.getArgs()[1], false);
        writeLogBegin(globalSettings, output.resolve("run.log"));

        // todo: use encoder.getReadsIndexIO instead of output directly
        if (!cli.hasOption("encoder") || cli.getOptionValue("encoder").equals("base11")) {
            Encoder<Character, AA, Character, DNA, Base11Alphabet> encoder = new K15Base11(
                    null, reads, null, output, mask, globalSettings.BITS_FOR_IDS);
            try (SequenceSupplier<Integer, Character, DNA, Byte, Base11Alphabet> sup = new SequenceSupplier<>(
                    encoder.getReadReader(), encoder.getReadConverter(), globalSettings.KEEP_IN_MEMORY)) {
                ReadIndexer<Base11Alphabet> readIndexer = new ReadIndexer<>(sup, output, encoder, globalSettings);
                String runInfo = readIndexer.index();
                writeLogEnd(runInfo, output.resolve("run.log"));
            } catch (Exception e) {
                writeLogEnd(e.toString(), output.resolve("run.log"));
                throw new RuntimeException(e);
            }
        } else if (cli.getOptionValue("encoder").equals("base11uniform")) {
            Encoder<Character, AA, Character, DNA, Base11Uniform> encoder = new K15Base11Uniform(
                    null, reads, null, output, mask, globalSettings.BITS_FOR_IDS);
            try (SequenceSupplier<Integer, Character, DNA, Byte, Base11Uniform> sup = new SequenceSupplier<>(
                    encoder.getReadReader(), encoder.getReadConverter(), globalSettings.KEEP_IN_MEMORY)) {
                ReadIndexer<Base11Uniform> readIndexer = new ReadIndexer<>(sup, output, encoder, globalSettings);
                String runInfo = readIndexer.index();
                writeLogEnd(runInfo, output.resolve("run.log"));
            } catch (Exception e) {
                writeLogEnd(e.toString(), output.resolve("run.log"));
                throw new RuntimeException(e);
            }
        } else if (cli.getOptionValue("encoder").equals("base11nuc")) {
            Encoder<Character, DNA, Character, DNA, Base11WithStop> encoder = new K15Base11Nuc(
                    null, reads, null, output, mask, globalSettings.BITS_FOR_IDS);
            try (SequenceSupplier<Integer, Character, DNA, Byte, Base11WithStop> sup = new SequenceSupplier<>(
                    encoder.getReadReader(), encoder.getReadConverter(), globalSettings.KEEP_IN_MEMORY)) {
                ReadIndexer<Base11WithStop> readIndexer = new ReadIndexer<>(sup, output, encoder, globalSettings);
                String runInfo = readIndexer.index();
                writeLogEnd(runInfo, output.resolve("run.log"));
            } catch (Exception e) {
                writeLogEnd(e.toString(), output.resolve("run.log"));
                throw new RuntimeException(e);
            }
        } else {
            System.err.println("Invalid encoder: " + cli.getOptionValue("encoder"));
            System.exit(1);
        }
    }

    private static void assignreads(GlobalSettings globalSettings, CommandLine cli) {
        checkNumberOfPositionalArguments(cli, 3);
        boolean[] mask = getMask(cli);
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
        if (!cli.hasOption("encoder") || cli.getOptionValue("encoder").equals("base11")) {
            Encoder<Character, AA, Character, DNA, Base11Alphabet> encoder = new K15Base11(
                    null, null, dbIndex, readsIndex, mask, globalSettings.BITS_FOR_IDS);
            ReadAssigner<Character, AA, Character, DNA, Base11Alphabet> readAssigner = new ReadAssigner<>(encoder, globalSettings);
            try {
                runInfo = readAssigner.assignReads();
                readAssignment = readAssigner.getReadAssignment();
            } catch (Exception e) {
                writeLogEnd(e.toString(), output.resolve("run.log"));
                throw new RuntimeException(e);
            }
        } else if (cli.getOptionValue("encoder").equals("base11uniform")) {
            Encoder<Character, AA, Character, DNA, Base11Uniform> encoder = new K15Base11Uniform(
                    null, null, dbIndex, readsIndex, mask, globalSettings.BITS_FOR_IDS);
            ReadAssigner<Character, AA, Character, DNA, Base11Uniform> readAssigner = new ReadAssigner<>(encoder, globalSettings);
            try {
                runInfo = readAssigner.assignReads();
                readAssignment = readAssigner.getReadAssignment();
            } catch (Exception e) {
                writeLogEnd(e.toString(), output.resolve("run.log"));
                throw new RuntimeException(e);
            }
        } else if (cli.getOptionValue("encoder").equals("base11nuc")) {
            Encoder<Character, DNA, Character, DNA, Base11WithStop> encoder = new K15Base11Nuc(
                    null, null, dbIndex, readsIndex, mask, globalSettings.BITS_FOR_IDS);
            ReadAssigner<Character, DNA, Character, DNA, Base11WithStop> readAssigner = new ReadAssigner<>(encoder, globalSettings);
            try {
                runInfo = readAssigner.assignReads();
                readAssignment = readAssigner.getReadAssignment();
            } catch (Exception e) {
                writeLogEnd(e.toString(), output.resolve("run.log"));
                throw new RuntimeException(e);
            }
        } else {
            System.err.println("Invalid encoder: " + cli.getOptionValue("encoder"));
            System.exit(1);
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
    public static boolean[] parseMask(String mask) {
        mask = mask.replaceAll("^0+", "").replaceAll("0+$", "");
        boolean[] result = new boolean[mask.length()];
        for (int i = 0; i < mask.length(); i++) {
            result[i] = mask.charAt(i) == '1';
        }
        return result;
    }
}