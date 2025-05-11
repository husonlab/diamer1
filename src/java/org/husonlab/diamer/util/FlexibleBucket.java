package org.husonlab.diamer.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class FlexibleBucket {
    private final int INITIAL_CAPACITY;
    private final int CHUNK_SIZE;
    private final int CONTINGENT_SIZE;
    private final AtomicInteger capacity;
    private final AtomicInteger size;
    public final ArrayList<long[]> chunks;

    public FlexibleBucket(int INITIAL_CAPACITY, int CHUNK_SIZE, int CONTINGENT_SIZE) {
        this.INITIAL_CAPACITY = INITIAL_CAPACITY;
        this.CHUNK_SIZE = CHUNK_SIZE;
        this.CONTINGENT_SIZE = CONTINGENT_SIZE;
        this.capacity = new AtomicInteger(INITIAL_CAPACITY);
        this.size = new AtomicInteger(0);
        this.chunks = new ArrayList<>();
        chunks.add(new long[INITIAL_CAPACITY]);
    }

    public FlexibleBucket(int INITIAL_CAPACITY) {
        this(INITIAL_CAPACITY, 131_072, 4096);
    }

    public void fill(long value) {
        for (long[] chunk : chunks) {
            Arrays.fill(chunk, value);
        }
    }

    public void set(int index, long value) {
        int chunkIndex = 0;
        int chunkOffset = index;
        if (index >= INITIAL_CAPACITY) {
            chunkIndex = (index - INITIAL_CAPACITY + CHUNK_SIZE) / CHUNK_SIZE;
            chunkOffset = (index - INITIAL_CAPACITY) % CHUNK_SIZE;
        }
        chunks.get(chunkIndex)[chunkOffset] = value;
    }

    public long getValue(int index) {
        int chunkIndex = 0;
        int chunkOffset = index;
        if (index >= INITIAL_CAPACITY) {
            chunkIndex = (index - INITIAL_CAPACITY + CHUNK_SIZE) / CHUNK_SIZE;
            chunkOffset = (index - INITIAL_CAPACITY) % CHUNK_SIZE;
        }
        return chunks.get(chunkIndex)[chunkOffset];
    }

    public Pair<Integer, Integer> getContingent() {
        synchronized (size) {
            int size = this.size.get();
            int capacity = this.capacity.get();
            if (size + CONTINGENT_SIZE <= capacity) {
                this.size.addAndGet(CONTINGENT_SIZE);
                return new Pair<>(size, size + CONTINGENT_SIZE);
            } else {
                long[] newLongChunk = new long[CHUNK_SIZE];
                chunks.add(newLongChunk);
                this.capacity.set(capacity + CHUNK_SIZE);
                this.size.addAndGet(CONTINGENT_SIZE);
                return new Pair<>(size, size + CONTINGENT_SIZE);
            }
        }
    }

    public int size() {
        return size.get();
    }

    public void clear() {
        size.set(0);
    }
}
