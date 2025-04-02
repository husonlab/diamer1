package org.husonlab.diamer2.main;

import org.apache.commons.cli.CommandLine;

public class GlobalSettings {
    /**
     * cli arguments
     */
    public final String[] args;

    public final static String VERSION = "2.0.0";
    /**
     * Maximum number of threads to use
     */
    public final int MAX_THREADS;
    public final int MAX_WRITE_THREADS = 16;
    /**
     * Maximum memory to use
     */
    public final int MAX_MEMORY;
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
    public final int SEQUENCE_BATCH_SIZE = 1000;
    /**
     * Buckets to process in one cycle
     */
    public final int BUCKETS_PER_CYCLE;
    /**
     * How large the queues for thread pools should be
     */
    public final int QUEUE_SIZE;

    public final boolean DEBUG;

    public final boolean COLLECT_STATS;

    public final boolean ONLY_STANDARD_RANKS;

    public GlobalSettings(String[] args, int MAX_THREADS, int BUCKETS_PER_CYCLE, int MAX_MEMORY, boolean KEEP_IN_MEMORY, boolean DEBUG, boolean COLLECT_STATS, boolean onlyStandardRanks) {
        this.args = args;
        this.MAX_THREADS = MAX_THREADS;
        this.BUCKETS_PER_CYCLE = BUCKETS_PER_CYCLE;
        this.MAX_MEMORY = MAX_MEMORY;
        this.KEEP_IN_MEMORY = KEEP_IN_MEMORY;
        this.QUEUE_SIZE = MAX_THREADS * 2;
        this.DEBUG = DEBUG;
        this.COLLECT_STATS = COLLECT_STATS;
        ONLY_STANDARD_RANKS = onlyStandardRanks;
    }

    @Override
    public String toString() {
        return String.join(" ", args) + "\n" +
                "MAX_THREADS:\t" + MAX_THREADS + "\n" +
                "MAX_MEMORY:\t" + MAX_MEMORY + "\n" +
                "KEEP_IN_MEMORY:\t" + KEEP_IN_MEMORY + "\n" +
                "BITS_FOR_IDS:\t" + BITS_FOR_IDS + "\n" +
                "SEQUENCE_BATCH_SIZE:\t" + SEQUENCE_BATCH_SIZE + "\n" +
                "BUCKETS_PER_CYCLE:\t" + BUCKETS_PER_CYCLE + "\n" +
                "QUEUE_SIZE:\t" + QUEUE_SIZE + "\n" +
                "DEBUG:\t" + DEBUG + "\n";
    }
}
