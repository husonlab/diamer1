package org.husonlab.diamer2.indexing;

import org.husonlab.diamer2.graph.Tree;
import org.husonlab.diamer2.logging.ProgressLogger;
import org.husonlab.diamer2.seq.Sequence;

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.*;

import static org.husonlab.diamer2.alphabet.AAEncoder.toBase11;

public class Indexer {
    private final Tree tree;
    private final int MAX_THREADS;
    private final int MAX_QUEUE_SIZE;
    private final int BATCH_SIZE;
    private final int bucketsPerCycle;
    private final LinkedList<int[]> bucketRangesToProcess;
    private int[] currentBucketRange = new int[2];

    /**
     * Indexes a Sequence database (multifasta file) of AA sequences.
     * @param tree The taxonomy tree to use for finding the LCA of the kmers
     * @param MAX_THREADS Maximum number of threads to use.
     * @param MAX_QUEUE_SIZE Maximum number of batches to queue for processing in the ThreadPoolExecutor queue.
     * @param BATCH_SIZE Number of Sequence sequences to process in each batch (thread).
     * @param bucketsPerCycle Number of buckets to process in each indexing cycle (one run over the database).
     */
    public Indexer(Tree tree, int MAX_THREADS, int MAX_QUEUE_SIZE, int BATCH_SIZE, int bucketsPerCycle) {
        // Initialize the parameters of the Indexer
        this.tree = tree;
        this.MAX_THREADS = MAX_THREADS;
        this.MAX_QUEUE_SIZE = MAX_QUEUE_SIZE;
        this.BATCH_SIZE = BATCH_SIZE;
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
    }

    /**
     * DBIndexIO a Sequence database and write the indexed buckets to files.
     * @param fastaFile Path to the Sequence database.
     * @param outPath Path to write the index buckets to.
     * @throws IOException If an error occurs during reading the database or writing the buckets.
     */
    public void indexDB(File fastaFile, Path outPath) throws IOException {
        System.out.println("[Indexer] Indexing " + fastaFile + " into " + outPath + " with " + MAX_THREADS + " threads.");
        for (int[] rangeToProcess : bucketRangesToProcess) {
            currentBucketRange = rangeToProcess;
            System.out.println("[Indexer] Indexing buckets " + currentBucketRange[0] + " - " + currentBucketRange[1]);
            ConcurrentHashMap<Long, Integer>[] bucketMaps = indexDBCurrentRange(fastaFile);
            Bucket[] buckets = convertBucketMaps(bucketMaps);
            writeBuckets(buckets, outPath);
        }
    }

    public void indexReads(File readsFile, Path outPath) throws IOException {
        System.out.println("[Indexer] Indexing " + readsFile + " into " + outPath + " with " + MAX_THREADS + " threads.");
        for (int[] rangeToProcess : bucketRangesToProcess) {
            currentBucketRange = rangeToProcess;
            System.out.println("[Indexer] Indexing buckets " + currentBucketRange[0] + " - " + currentBucketRange[1]);
            ConcurrentLinkedQueue<Long>[] bucketLists = indexReadsCurrentRange(readsFile, outPath);
            Bucket[] buckets = convertBucketLists(bucketLists);
            writeBuckets(buckets, outPath);
        }
    }

    /**
     * DBIndexIO the selected range (currentBucketRange) of buckets from the Sequence database.
     * @param fastaFile Path to the Sequence database.
     * @return Array of ConcurrentHashMaps with the kmers and their taxIds for each bucket.
     * @throws IOException If an error occurs during reading the database.
     */
    private ConcurrentHashMap<Long, Integer>[] indexDBCurrentRange(File fastaFile) throws IOException {

        ThreadPoolExecutor threadPoolExecutor = new CustomThreadPoolExecutor(
                MAX_THREADS,
                MAX_THREADS,
                500L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(MAX_QUEUE_SIZE),
                new ThreadPoolExecutor.CallerRunsPolicy());

        // Initialize Concurrent HashMaps to store the kmers and their taxIds during indexing
        final ConcurrentHashMap<Long, Integer>[] bucketMaps = new ConcurrentHashMap[bucketsPerCycle];
        for (int i = 0; i < currentBucketRange[1] - currentBucketRange[0]; i++) {
            bucketMaps[i] = new ConcurrentHashMap<Long, Integer>(57000000); // initial capacity 57000000
        }

        // Read the Sequence database and process the sequences in batches
        try (BufferedReader br = new BufferedReader(new FileReader(fastaFile))) {

            // Initialize variable for Sequence reading
            String header = null;
            StringBuilder sequence = new StringBuilder();
            Sequence[] batch = new Sequence[BATCH_SIZE];
            ProgressLogger progressLogger = new ProgressLogger("Fastas", "[Indexer]", 60000);
            int processedFastas = 0;
            String line;
            // Flag to indicate if new data was added to the batch to avoid processing empty batches.
            boolean newDataInBatch = false;

            // #########################################################################################################
            // Read the Sequence file line by line
            while ((line = br.readLine()) != null) {
                line = line.strip();

                // CASE: line == Sequence header : store the header and process the previous sequence
                if (line.startsWith(">")) {

                    // CASE: line != first header -> process the previous sequence (all headers except the first one)
                    if (header != null) {
                        batch[processedFastas % BATCH_SIZE] = new Sequence(header, sequence.toString());
                        processedFastas++;
                        newDataInBatch = true;
                        sequence = new StringBuilder();
                    }
                    // always store the header
                    header = line.substring(1);

                // CASE: line == Sequence sequence -> append the sequence to the current sequence
                } else if (header != null) {
                    sequence.append(line);
                }

                // CASE: batch is full -> process the batch
                if (processedFastas % BATCH_SIZE == 0 && newDataInBatch) {

                    // Submit a new FastaBatchProcessor thread to process the batch
                    threadPoolExecutor.submit(new FastaBatchProcessor(batch, bucketMaps, tree, currentBucketRange));
                    batch = new Sequence[BATCH_SIZE];
                    newDataInBatch = false;

                    // Print progress every minute
                    progressLogger.logProgress(processedFastas);
                }
            }
            // Append last sequence to the batch and submit the last batch for processing
            batch[processedFastas % BATCH_SIZE] = new Sequence(header, sequence.toString());
            threadPoolExecutor.submit(new FastaBatchProcessor(batch, bucketMaps, tree, currentBucketRange));
        }
        // #############################################################################################################

        // Shutdown the ThreadPoolExecutor and print size of each bucket
        shutdownThreadPoolExecutor(threadPoolExecutor);
        System.out.println("[Indexer] Finished indexing.");
        for (int j = 0; j < bucketMaps.length; j++) {
            System.out.println("[Indexer] Size of bucket " + j + ": " + bucketMaps[j].size());
        }
        return bucketMaps;
    }

    private ConcurrentLinkedQueue<Long>[] indexReadsCurrentRange(File fastqFile, Path outPath) throws IOException {

        HashMap<Integer, String> readHeaderMap = new HashMap<>();

        ThreadPoolExecutor threadPoolExecutor = new CustomThreadPoolExecutor(
                MAX_THREADS,
                MAX_THREADS,
                500L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(MAX_QUEUE_SIZE),
                new ThreadPoolExecutor.CallerRunsPolicy());

        // Initialize LinkedLists to store the kmers and their taxIds during indexing
        final ConcurrentLinkedQueue<Long>[] buckets = new ConcurrentLinkedQueue[currentBucketRange[1] - currentBucketRange[0]];
        for (int i = 0; i < currentBucketRange[1] - currentBucketRange[0]; i++) {
            buckets[i] = new ConcurrentLinkedQueue<Long>();
        }

        // Read the FastQ file and process the sequences in batches
        try (BufferedReader br = new BufferedReader(new FileReader(fastqFile))) {

            // Initialize variable for Sequence reading
            Sequence[] batch = new Sequence[BATCH_SIZE];
            ProgressLogger progressLogger = new ProgressLogger("Reads", "[Indexer]", 60000);
            int processedReads = 0;
            String line;

            // #########################################################################################################
            // Read the Sequence file line by line
            while ((line = br.readLine()) != null) {
                String header = null;
                String sequence = null;
                try {
                    header = line.strip().substring(1);
                    sequence = br.readLine().strip();
                    br.readLine(); // skip the "+" line
                    br.readLine(); // skip the quality line
                } catch (NullPointerException e) {
                    System.err.println("[Indexer] Error reading FastQ file. FastQ format violated?");
                    e.printStackTrace();
                }
                Sequence fastQ = new Sequence(header, sequence);

                // Only store the header index during the first batch
                if (currentBucketRange[0] == 0) {
                    readHeaderMap.put(processedReads, header);
                }

                batch[processedReads % BATCH_SIZE] = fastQ;
                processedReads++;

                // CASE: batch is full -> process the batch
                if (processedReads % BATCH_SIZE == 0) {

                    // Submit a new FastQBatchProcessor thread to process the batch
                    threadPoolExecutor.submit(new FastQBatchProcessor(batch, buckets, currentBucketRange, processedReads - BATCH_SIZE));
                    batch = new Sequence[BATCH_SIZE];

                    // Print progress every minute
                    progressLogger.logProgress(processedReads);
                }
            }
            // submit the last batch for processing if not empty
            if (batch[0] != null) {
                threadPoolExecutor.submit(new FastQBatchProcessor(batch, buckets, currentBucketRange, processedReads - processedReads % BATCH_SIZE));
            }

            // Write the read headers to a file in a separate thread
            if(currentBucketRange[0] == 0) {
                Thread writeReadHeaders = new Thread(() -> {
                    System.out.println("[Indexer] Started writing read headers to file...");
                    try (BufferedWriter bw = new BufferedWriter (new FileWriter(outPath.toString() + "/header_index.txt"))) {
                        bw.write(Integer.toString(readHeaderMap.size()));
                        bw.newLine();
                        readHeaderMap.forEach((key, value) -> {
                            try {
                                bw.write(key + "\t" + value + "\n");
                            } catch (IOException e) {
                                System.err.println("[Indexer] Error writing read headers to file.");
                                e.printStackTrace();
                            }
                        });
                    } catch (IOException e) {
                        System.err.println("[Indexer] Error writing read headers to file.");
                        e.printStackTrace();
                    }
                    readHeaderMap.clear();
                    System.out.println("[Indexer] Finished writing read headers to file.");
                }, "WriteReadHeaders");
                writeReadHeaders.start();
            }
        }
        // #############################################################################################################

        // Shutdown the ThreadPoolExecutor and print size of each bucket
        shutdownThreadPoolExecutor(threadPoolExecutor);
        System.out.println("[Indexer] Finished indexing.");
        for (int j = 0; j < buckets.length; j++) {
            System.out.println("[Indexer] Size of bucket " + j + ": " + buckets[j].size());
        }
        return buckets;
    }

    /**
     * Converts the bucketMaps (kmer -> taxId) to Bucket objects that contain sorted long arrays
     * with the kmer and taxIds encoded in the longs.
     * @param bucketMaps Array of ConcurrentHashMaps with the kmers and their taxIds for each bucket.
     * @return Array of long arrays (buckets) with sorted longs that encode kmer and taxId.
     */
    public Bucket[] convertBucketMaps(ConcurrentHashMap<Long, Integer>[] bucketMaps) {
        System.out.println("[Indexer] Converting buckets to arrays and sorting...");

        final int threads = Math.min(MAX_THREADS, bucketMaps.length);
        ThreadPoolExecutor threadPoolExecutor = new CustomThreadPoolExecutor(
                threads,
                threads,
                500L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(Math.min(bucketMaps.length, MAX_QUEUE_SIZE)),
                new ThreadPoolExecutor.CallerRunsPolicy());

        Bucket[] buckets = new Bucket[bucketMaps.length];
        for (int i = 0; i < bucketMaps.length; i++) {
            buckets[i] = new Bucket(i + currentBucketRange[0]);
            threadPoolExecutor.submit(new BucketMapConverter(bucketMaps[i], buckets[i]));
        }

        shutdownThreadPoolExecutor(threadPoolExecutor);
        System.out.println("[Indexer] Finished converting buckets.");
        return buckets;
    }

    public Bucket[] convertBucketLists(ConcurrentLinkedQueue<Long>[] bucketLists) {
        System.out.println("[Indexer] Converting buckets to arrays and sorting...");

        final int threads = Math.min(MAX_THREADS, bucketLists.length);
        ThreadPoolExecutor threadPoolExecutor = new CustomThreadPoolExecutor(
                threads,
                threads,
                500L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(Math.min(bucketLists.length, MAX_QUEUE_SIZE)),
                new ThreadPoolExecutor.CallerRunsPolicy());

        Bucket[] buckets = new Bucket[bucketLists.length];
        for (int i = 0; i < bucketLists.length; i++) {
            Bucket bucket = new Bucket(i + currentBucketRange[0]);
            buckets[i] = bucket;
            ConcurrentLinkedQueue<Long> bucketList = bucketLists[i];
            threadPoolExecutor.submit(() -> {
                try {
                    System.out.println("[Indexer] Converting bucket " + bucket.getName() + " to array...");
                    bucket.setContent(bucketList.stream().mapToLong(Long::longValue).toArray());
                    bucketList.clear();
                    System.out.println("[Indexer] Sorting bucket " + bucket.getName() + "...");
                    bucket.sort();
                    System.out.println("[Indexer] Finished converting bucket " + bucket.getName() + ".");
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            });
        }

        shutdownThreadPoolExecutor(threadPoolExecutor);
        System.out.println("[Indexer] Finished converting buckets.");
        return buckets;
    }

    /**
     * Writes the buckets to files.
     * @param buckets Array of Buckets.
     * @param path Path to write the buckets to.
     */
    public void writeBuckets(Bucket[] buckets, Path path) {
        System.out.println("[Indexer] Writing buckets to files...");

        final int threads = Math.min(MAX_THREADS, buckets.length);
        ThreadPoolExecutor threadPoolExecutor = new CustomThreadPoolExecutor(
                threads,
                threads,
                500L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(Math.min(buckets.length, MAX_QUEUE_SIZE)),
                new ThreadPoolExecutor.CallerRunsPolicy());

        for (Bucket bucket : buckets) {
            threadPoolExecutor.submit(() -> {
                try {
                    System.out.println("[Indexer] Starting to write bucket " + bucket.getName());
                    bucket.writeToFile(path);
                    System.out.println("[Indexer] Finished writing bucket " + bucket.getName());
                } catch (Throwable t) {
                    System.err.println("[Indexer] Error writing bucket to file.");
                    t.printStackTrace();
                }
            });
        }

        shutdownThreadPoolExecutor(threadPoolExecutor);
        System.out.println("[Indexer] Finished writing buckets.");
    }

    public static void shutdownThreadPoolExecutor(ThreadPoolExecutor threadPoolExecutor) {
        threadPoolExecutor.shutdown();
        try {
            System.out.println("[Indexer] Waiting for threads to finish...");
            if (!threadPoolExecutor.awaitTermination(2, TimeUnit.HOURS)) {
                System.out.println("[Indexer] Timeout reached. Forcing shutdown.");
                threadPoolExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
