package org.husonlab.diamer2.main;

public class GlobalSettings {
    public final static String VERSION = "2.0.0";
    /**
     * Maximum number of threads to use
     */
    public final int MAX_THREADS;
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

    public final boolean DEBUG;

    public GlobalSettings(int MAX_THREADS, int MAX_MEMORY, boolean KEEP_IN_MEMORY, boolean DEBUG) {
        this.MAX_THREADS = MAX_THREADS;
        this.MAX_MEMORY = MAX_MEMORY;
        this.KEEP_IN_MEMORY = KEEP_IN_MEMORY;
        this.DEBUG = DEBUG;
    }
}
