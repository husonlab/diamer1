package org.husonlab.diamer2.main;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import static org.husonlab.diamer2.main.CliUtils.printHelp;

public class GlobalSettings {
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

    public GlobalSettings(String[] args, CommandLine cli, Options options) {
        this.args = args;
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
        MAX_THREADS = maxThreads;
        KEEP_IN_MEMORY = cli.hasOption("keep-in-memory");
        if (cli.hasOption("b")) {
            try {
                BUCKETS_PER_CYCLE = Integer.parseInt(cli.getOptionValue("b"));
            } catch (NumberFormatException e) {
                System.err.printf("Invalid number of buckets per cycle: \"%s\"\n", cli.getOptionValue("buckets-per-cycle"));
                printHelp(options);
                System.exit(1);
            }
        }
        QUEUE_SIZE = MAX_THREADS * 2;
        DEBUG = cli.hasOption("debug");
        COLLECT_STATS = cli.hasOption("statistics");
        ONLY_STANDARD_RANKS = cli.hasOption("only-standard-ranks");
    }

    public GlobalSettings(String[] args, int MAX_THREADS, int BUCKETS_PER_CYCLE, boolean KEEP_IN_MEMORY, boolean DEBUG, boolean COLLECT_STATS, boolean onlyStandardRanks) {
        this.args = args;
        this.MAX_THREADS = MAX_THREADS;
        this.BUCKETS_PER_CYCLE = BUCKETS_PER_CYCLE;
        this.KEEP_IN_MEMORY = KEEP_IN_MEMORY;
        this.QUEUE_SIZE = MAX_THREADS * 2;
        this.DEBUG = DEBUG;
        this.COLLECT_STATS = COLLECT_STATS;
        ONLY_STANDARD_RANKS = onlyStandardRanks;
    }

    @Override
    public String toString() {
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
                "ONLY_STANDARD_RANKS:\t" + ONLY_STANDARD_RANKS + "\n";
    }

    public void setBUCKETS_PER_CYCLE(int BUCKETS_PER_CYCLE) {
        this.BUCKETS_PER_CYCLE = BUCKETS_PER_CYCLE;
    }
}
