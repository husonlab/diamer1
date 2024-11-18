package org.husonlab.diamer2.indexing;

import org.husonlab.diamer2.io.NCBIReader;
import org.husonlab.diamer2.seq.FASTA;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.*;

import static org.husonlab.diamer2.alphabet.AAEncoder.toBase11andNumber;

public class Indexer {
    private final NCBIReader.Tree tree;
    private final int MAX_THREADS;
    private final int MAX_QUEUE_SIZE;
    private final int FASTA_BATCH_SIZE;
    private final short[] bucketRange;
    private final ConcurrentHashMap<Long, Integer>[] bucketMaps;
    // Queue to store sequence headers, that could not be found in the taxonomy.
    private final ConcurrentLinkedQueue<String> unprocessedFastas = new ConcurrentLinkedQueue<>();

    /**
     * Indexes a FASTA database (multifasta file) of AA sequences.
     * @param MAX_THREADS Maximum number of threads to use for processing FASTA sequences.
     *                    Each thread processes a batch of FASTA sequences and sorts the kmers into the respective bucket.
     * @param MAX_QUEUE_SIZE Maximum number of batches to queue for processing in the ThreadPoolExecutor queue.
     * @param FASTA_BATCH_SIZE Number of FASTA sequences to process in each batch (thread).
     * @param bucketRange Range of buckets to process during one run over the input FASTA database.
     */
    public Indexer(NCBIReader.Tree tree, int MAX_THREADS, int MAX_QUEUE_SIZE, int FASTA_BATCH_SIZE, short[] bucketRange) {
        this.tree = tree;
        this.MAX_THREADS = MAX_THREADS;
        this.MAX_QUEUE_SIZE = MAX_QUEUE_SIZE;
        this.FASTA_BATCH_SIZE = FASTA_BATCH_SIZE;
        this.bucketRange = bucketRange;
        this.bucketMaps = new ConcurrentHashMap[bucketRange[1] - bucketRange[0]];
        for (int i = 0; i < bucketRange[1] - bucketRange[0]; i++) {
            bucketMaps[i] = new ConcurrentHashMap<Long, Integer>();
        }
    }

    /**
     * Run indexing on a FASTA database and add kmers to the buckets.
     * @param fastaFile Path to the FASTA database.
     */
    public void index(File fastaFile) {
        System.out.println("[Indexer] Starting indexing on " + fastaFile);
        final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                MAX_THREADS,
                MAX_THREADS,
                500L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(MAX_QUEUE_SIZE),
                new ThreadPoolExecutor.CallerRunsPolicy());
        String header = null;
        StringBuilder sequence = new StringBuilder();
        FASTA[] batch = new FASTA[FASTA_BATCH_SIZE];

        try (BufferedReader br = new BufferedReader(new FileReader(fastaFile))) {
            int processedFastas = 0;
            long timerStart = System.currentTimeMillis();
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
                    processBatch(batch, threadPoolExecutor);
                    newData = false;
                    if (processedFastas % 100000 == 0) {
                        long sequencesPerSecond = 100000000L / (System.currentTimeMillis() - timerStart);
                        timerStart = System.currentTimeMillis();
                        System.out.println("[Indexer] Processed " + processedFastas/1000000 + "M sequences. " + sequencesPerSecond + " sequences per second.");
                    }
                }
            }
            batch[processedFastas % FASTA_BATCH_SIZE] = new FASTA(header, sequence.toString());
            if (newData) {
                processBatch(batch, threadPoolExecutor);
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
        System.out.println("[Indexer] Number of unprocessed FASTAs: " + unprocessedFastas.size());
    }

    /**
     * Process a batch of FASTA sequences and add kmers to the respective buckets.
     * @param fastas Array (batch) of FASTA sequences to process.
     * @param threadPoolExecutor ThreadPoolExecutor to run the processing in parallel.
     */
    private void processBatch(FASTA[] fastas, ThreadPoolExecutor threadPoolExecutor) {
        try {
            threadPoolExecutor.execute(() -> {
                for (FASTA fasta : fastas) {
                    if (fasta == null) {
                        continue;
                    }
                    String sequence = fasta.getSequence();
                    int taxId = Integer.parseInt(fasta.getHeader().split(" ")[0].substring(1));
                    for (int i = 0; i + 15 <= sequence.length(); i++) {
                        String kmer = sequence.substring(i, i + 15);
                        long kmerEnc = toBase11andNumber(kmer);
                        short bucketId = (short) (kmerEnc & 0b1111111111);
                        if (bucketId < bucketRange[1] - bucketRange[0]) {
                            bucketMaps[bucketId].computeIfAbsent(kmerEnc, k -> taxId);
                            bucketMaps[bucketId].computeIfPresent(kmerEnc, (k, v) -> tree.findLCA(v, taxId));
                        }
                    }
                }
            });
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    public ConcurrentHashMap<Long, Integer>[] getBucketMaps() {
        return bucketMaps;
    }

    /**
     * Converts the bucketMaps (kmer -> taxId) to an array of sorted long arrays
     * with the kmer and taxId both encoded in each long.
     * @return Array of long arrays (buckets) with sorted longs that encode kmer and taxId.
     */
    public long[][] getBuckets() {
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

    public void writeBuckets(Path path) {
        long[][] buckets = getBuckets();
        for (int i = 0; i < bucketRange[1] - bucketRange[0]; i++) {
            try (FileOutputStream fos = new FileOutputStream(path.resolve("bucket" + i + ".bin").toString());
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

    public ConcurrentLinkedQueue<String> getUnprocessedFastas() {
        return unprocessedFastas;
    }
}
