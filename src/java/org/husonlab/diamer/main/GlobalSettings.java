package org.husonlab.diamer.main;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.husonlab.diamer.readAssignment.algorithms.ClassificationAlgorithm;
import org.husonlab.diamer.seq.alphabet.ReducedAlphabet;
import org.husonlab.diamer.util.logging.LogFileWriter;
import org.husonlab.diamer.util.logging.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static org.husonlab.diamer.io.Utilities.getFile;
import static org.husonlab.diamer.io.Utilities.getFolder;
import static org.husonlab.diamer.main.CliUtils.*;
import static org.husonlab.diamer.main.CliUtils.parseAlgorithms;

public class GlobalSettings {
    public final Logger logger;
    public final LogFileWriter logFileWriter;
    /**
     * cli arguments
     */
    public final String[] args;

    public final static String VERSION = "1.0.0";
    /**
     * Maximum number of threads to use
     */
    public final int MAX_THREADS;
    /**
     * Maximum number of threads to use for IO operations
     */
    public final int MAX_IO_THREADS = 16;
    /**
     * Whether to keep sequences in memory during iterations or not (in the SequenceSupplier class)
     */
    public final boolean KEEP_IN_MEMORY;
    /**
     * How many bits of the index are reserved for the ids of the sequences
     */
    public final int BITS_FOR_IDS = 22;
    /**
     * How many sequences to process in a batch in one thread
     */
    public final int SEQUENCE_BATCH_SIZE = 1_000;
    /**
     * Buckets to process in one cycle
     */
    public int BUCKETS_PER_CYCLE;
    /**
     * How large the queues for thread pools should be
     */
    public final int QUEUE_SIZE;
    /**
     * Flag for debugging
     */
    public final boolean DEBUG;
    /**
     * Flag for collecting and writing statistics about generated index files.
     */
    public final boolean COLLECT_STATS;
    /**
     * Flag to reduce the taxonomic tree to standard ranks only.
     */
    public final boolean ONLY_STANDARD_RANKS;

    public ReducedAlphabet ALPHABET;
    public boolean[] MASK;
    public List<ClassificationAlgorithm> ALGORITHMS;

    public Path INPUT;
    public final Path OUTPUT;
    public Path DB_INDEX;
    public Path READS_INDEX;

    public GlobalSettings(String[] args, CommandLine cli, Options options, Path output, Path logFile) {
        this.logger = new Logger("DIAMER");
        getFolder(logFile.getParent().toString(), false);
        this.logFileWriter = new LogFileWriter(logFile);
        this.args = args;
        int maxThreads = Runtime.getRuntime().availableProcessors();
        if (!Objects.isNull(cli) && cli.hasOption("threads")) {
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
        }

        MAX_THREADS = maxThreads;
        KEEP_IN_MEMORY = !Objects.isNull(cli) && cli.hasOption("keep-in-memory");
        if (!Objects.isNull(cli) && cli.hasOption("b")) {
            try {
                BUCKETS_PER_CYCLE = Integer.parseInt(cli.getOptionValue("b"));
            } catch (NumberFormatException e) {
                System.err.printf("Invalid number of buckets per cycle: \"%s\"\n", cli.getOptionValue("buckets-per-cycle"));
                printHelp(options);
                System.exit(1);
            }
        }
        QUEUE_SIZE = MAX_THREADS * 2;
        DEBUG = !Objects.isNull(cli) && cli.hasOption("debug");
        COLLECT_STATS = !Objects.isNull(cli) && cli.hasOption("statistics");
        ONLY_STANDARD_RANKS = !Objects.isNull(cli) && cli.hasOption("only-standard-ranks");

        ALPHABET = getAlphabet(cli, "[L][A][GC][VWUBIZO*][SH][EMX][TY][RQ][DN][IF][PK]");
        MASK = getMask(cli, "1111111111111");
        ALGORITHMS = parseAlgorithms(cli);
        OUTPUT = output;
    }

    public GlobalSettings(String[] args, CommandLine cli, Options options, Path output) {
        this(args, cli, options, output, output.resolve("run.log"));
    }

    @Override
    public String toString() {
        StringBuilder maskString = new StringBuilder();
        for (boolean b : MASK) {
            maskString.append(b ? "1" : "0");
        }
        StringBuilder algorithmsString = new StringBuilder();
        for (ClassificationAlgorithm algorithm : ALGORITHMS) {
            algorithmsString.append(algorithm.toString()).append(", ");
        }
        if (!algorithmsString.isEmpty()) {
            algorithmsString.setLength(algorithmsString.length() - 2);
        }
        return String.join(" ", args) + "\n" +
                "VERSION:\t" + VERSION + "\n" +
                "MAX_THREADS:\t" + MAX_THREADS + "\n" +
                "MAX_IO_THREADS:\t" + MAX_IO_THREADS + "\n" +
                "KEEP_IN_MEMORY:\t" + KEEP_IN_MEMORY + "\n" +
                "BITS_FOR_IDS:\t" + BITS_FOR_IDS + "\n" +
                "SEQUENCE_BATCH_SIZE:\t" + SEQUENCE_BATCH_SIZE + "\n" +
                "BUCKETS_PER_CYCLE:\t" + BUCKETS_PER_CYCLE + "\n" +
                "QUEUE_SIZE:\t" + QUEUE_SIZE + "\n" +
                "DEBUG:\t" + DEBUG + "\n" +
                "COLLECT_STATS:\t" + COLLECT_STATS + "\n" +
                "ONLY_STANDARD_RANKS:\t" + ONLY_STANDARD_RANKS + "\n" +
                "ALPHABET:\t" + ALPHABET + "\n" +
                "MASK:\t" + maskString + "\n" +
                "ALGORITHMS:\t" + algorithmsString + "\n" +
                "INPUT:\t" + INPUT + "\n" +
                "OUTPUT:\t" + OUTPUT + "\n" +
                "DB_INDEX:\t" + DB_INDEX + "\n" +
                "READS_INDEX:\t" + READS_INDEX + "\n";
    }
}
