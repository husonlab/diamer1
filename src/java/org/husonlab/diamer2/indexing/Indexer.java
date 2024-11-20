package org.husonlab.diamer2.indexing;

import org.husonlab.diamer2.graph.Tree;
import org.husonlab.diamer2.seq.FASTA;

import java.io.*;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.concurrent.*;

import static org.husonlab.diamer2.alphabet.AAEncoder.toBase11AndNumber;

public class Indexer {
    private final Tree tree;
    private final int MAX_THREADS;
    private final int MAX_QUEUE_SIZE;
    private final int FASTA_BATCH_SIZE;
    private final int bucketsPerCycle;
    private final LinkedList<int[]> bucketRangesToProcess;
    private final ThreadPoolExecutor threadPoolExecutor;
    private int[] currentBucketRange = new int[2];

    /**
     * Indexes a FASTA database (multifasta file) of AA sequences.
     * @param MAX_THREADS Maximum number of threads to use for processing FASTA sequences.
     *                    Each thread processes a batch of FASTA sequences and sorts the kmers into the respective bucket.
     * @param MAX_QUEUE_SIZE Maximum number of batches to queue for processing in the ThreadPoolExecutor queue.
     * @param FASTA_BATCH_SIZE Number of FASTA sequences to process in each batch (thread).
     */
    public Indexer(Tree tree, int MAX_THREADS, int MAX_QUEUE_SIZE, int FASTA_BATCH_SIZE, int bucketsPerCycle) {
        // Initialize the parameters of the Indexer
        this.tree = tree;
        this.MAX_THREADS = MAX_THREADS;
        this.MAX_QUEUE_SIZE = MAX_QUEUE_SIZE;
        this.FASTA_BATCH_SIZE = FASTA_BATCH_SIZE;
        this.bucketsPerCycle = bucketsPerCycle;
        this.bucketRangesToProcess = new LinkedList<>();
        // Divide the 1024 buckets into ranges to process in each cycle
        int i = 0;
        while (i < 1024) {
            int[] bucketRange = new int[2];
            bucketRange[0] = i;
            bucketRange[1] = Math.min(i + bucketsPerCycle, 1024);
            bucketRangesToProcess.add(bucketRange);
            i += bucketsPerCycle;
        }
        this.threadPoolExecutor = new ThreadPoolExecutor(
                1,
                MAX_THREADS,
                500L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(MAX_QUEUE_SIZE),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    public void index(File fastaFile, Path outPath) {
        System.out.println("[Indexer] Indexing " + fastaFile + " into " + outPath + " with " + MAX_THREADS + " threads.");
        for (int[] rangeToProcess : bucketRangesToProcess) {
            currentBucketRange = rangeToProcess;
            System.out.println("[Indexer] Indexing buckets " + currentBucketRange[0] + " - " + currentBucketRange[1]);
            indexCurrentRange(fastaFile, outPath);
        }
    }

    /**
     * Run indexing on a FASTA database and add kmers to the buckets.
     * @param fastaFile Path to the FASTA database.
     */
    private void indexCurrentRange(File fastaFile, Path outPath) {
        System.out.println("[Indexer] Indexing buckets " + currentBucketRange[0] + " - " + currentBucketRange[1]);

        // Initialize Concurrent HashMaps to store the kmers and their taxIds during indexing
        final ConcurrentHashMap<Long, Integer>[] bucketMaps = new ConcurrentHashMap[bucketsPerCycle];
        for (int i = 0; i < currentBucketRange[1] - currentBucketRange[0]; i++) {
            bucketMaps[i] = new ConcurrentHashMap<Long, Integer>(21000000);
        }

        // Initialize variable for FASTA reading
        String header = null;
        StringBuilder sequence = new StringBuilder();
        FASTA[] batch = new FASTA[FASTA_BATCH_SIZE];
        long timerStart = System.currentTimeMillis();
        int processedFastasLastPrint = 0;
        int processedFastas = 0;

        // Read the FASTA database and process the sequences in batches
        try (BufferedReader br = new BufferedReader(new FileReader(fastaFile))) {
            String line;
            boolean newData = false; // Flag to indicate if new data was added to the batch to avoid processing empty batches.
            while ((line = br.readLine()) != null) {
                line = line.strip();
                if (line.startsWith(">")) {
                    if (header != null) {
                        batch[processedFastas % FASTA_BATCH_SIZE] = new FASTA(header, sequence.toString());
                        processedFastas++;
                        newData = true;
                        sequence = new StringBuilder();
                    }
                    header = line.substring(1);
                } else if (header != null) {
                    sequence.append(line);
                }
                if (processedFastas % FASTA_BATCH_SIZE == 0 && newData) {
                    // Submit a new FastaBatchProcessor thread to process the batch
                    threadPoolExecutor.submit(new FastaBatchProcessor(batch, bucketMaps, tree, currentBucketRange));
                    newData = false;

                    // Print progress every minute
                    if (System.currentTimeMillis() - timerStart > 60000) {
                        double mSequencesPerSecond = (processedFastas - processedFastasLastPrint) / 60f;
                        System.out.printf("[Indexer] (buckets %d - %d) Processed %dM sequences (%.2f seq/s)%n",
                                currentBucketRange[0], currentBucketRange[1], (int) (processedFastas * 1e-6), mSequencesPerSecond);
                        timerStart = System.currentTimeMillis();
                        processedFastasLastPrint = processedFastas;
                    }
                }
            }
            batch[processedFastas % FASTA_BATCH_SIZE] = new FASTA(header, sequence.toString());
            if (newData) {
                // Submit a new FastaBatchProcessor thread to process the batch
                threadPoolExecutor.submit(new FastaBatchProcessor(batch, bucketMaps, tree, currentBucketRange));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        threadPoolExecutor.shutdown();
        try {
            System.out.println("[Indexer] Waiting for threads to finish...");
            if (!threadPoolExecutor.awaitTermination(1, TimeUnit.MINUTES)) {
                System.out.println("[Indexer] Timeout reached. Forcing shutdown.");
                threadPoolExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("[Indexer] Finished indexing.");
        for (int j = 0; j < bucketMaps.length; j++) {
            System.out.println("[Indexer] Size of bucket " + j + ": " + bucketMaps[j].size());
        }
        writeBuckets(bucketMaps, outPath);
    }

    /**
     * Converts the bucketMaps (kmer -> taxId) to an array of sorted long arrays
     * with the kmer and taxId both encoded in each long.
     * @return Array of long arrays (buckets) with sorted longs that encode kmer and taxId.
     */
    public long[][] getBuckets(ConcurrentHashMap<Long, Integer>[] bucketMaps) {
        long[][] buckets = new long[bucketMaps.length][];
        for (int i = 0; i < bucketMaps.length; i++) {
            buckets[i] = new long[bucketMaps[i].size()];
            int j = 0;
            for (ConcurrentHashMap.Entry<Long, Integer> entry : bucketMaps[i].entrySet()) {
                buckets[i][j] = (entry.getKey() << 22) | entry.getValue();
                j++;
            }
        }
        return buckets;
    }

    public void writeBuckets(ConcurrentHashMap<Long, Integer>[] bucketMaps, Path path) {
        System.out.println("[Indexer] Converting buckets to arrays...");
        long[][] buckets = getBuckets(bucketMaps);
        System.out.println("[Indexer] Sorting buckets...");
        final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                Math.min(MAX_THREADS, buckets.length),
                Math.min(MAX_THREADS, buckets.length),
                500L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(buckets.length),
                new ThreadPoolExecutor.CallerRunsPolicy());
        for (int i = 0; i < buckets.length; i++) {
            buckets[i] = Sorting.radixSort44bits(buckets[i]);
        }
        threadPoolExecutor.shutdown();
        try {
            System.out.println("[Indexer] Waiting for sorting threads to finish...");
            if (!threadPoolExecutor.awaitTermination(20, TimeUnit.MINUTES)) {
                System.out.println("[Indexer] 20 min timeout reached. Forcing shutdown.");
                threadPoolExecutor.shutdownNow();
                System.err.println("[Indexer] Could not finish sorting.");
                System.exit(1);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("[Indexer] Finished sorting.");
        System.out.println("[Indexer] Writing buckets to " + path);
        for (int i = 0; i < currentBucketRange[1] - currentBucketRange[0]; i++) {
            System.out.println("[Indexer] Writing bucket " + (i + currentBucketRange[0]));
            try (FileOutputStream fos = new FileOutputStream(path.resolve((i + currentBucketRange[0]) + ".bin").toString());
                 DataOutputStream dos = new DataOutputStream(fos)) {
                dos.writeInt(buckets[i].length);
                for (long kmer : buckets[i]) {
                    dos.writeLong(kmer);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
