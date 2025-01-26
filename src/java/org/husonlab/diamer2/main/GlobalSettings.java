package org.husonlab.diamer2.main;

public class GlobalSettings {
    public final static String VERSION = "2.0.0";
    public final int MAX_THREADS;
    public final int MAX_MEMORY;
    public final boolean KEEP_IN_MEMORY;

    public GlobalSettings(int MAX_THREADS, int MAX_MEMORY, boolean KEEP_IN_MEMORY) {
        this.MAX_THREADS = MAX_THREADS;
        this.MAX_MEMORY = MAX_MEMORY;
        this.KEEP_IN_MEMORY = KEEP_IN_MEMORY;
    }
}
