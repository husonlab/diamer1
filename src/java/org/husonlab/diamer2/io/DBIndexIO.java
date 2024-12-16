package org.husonlab.diamer2.io;

import org.husonlab.diamer2.indexing.CustomThreadPoolExecutor;
import org.husonlab.diamer2.logging.Logger;
import org.husonlab.diamer2.logging.OneLineLogger;
import org.husonlab.diamer2.logging.ProgressBar;
import org.husonlab.diamer2.taxonomy.Tree;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Represents an index folder.
 */
public class DBIndexIO {
    protected final Logger logger;
    protected final Path indexFolder;
    protected final boolean[] availableBuckets;

    /**
     * Create a new DBIndexIO object.
     * @param indexFolder path to the index folder
     * @throws FileNotFoundException if the index folder or the read header mapping file (in case of a READS index)
     * is missing
     */
    public DBIndexIO(Path indexFolder) throws FileNotFoundException {
        this.logger = new Logger("DBIndexIO");
        if (!indexFolder.toFile().isDirectory()) {
            logger.logWarning("DBIndexIO folder does not exist.");
            throw new FileNotFoundException("DBIndexIO folder does not exist.");
        }
        this.indexFolder = indexFolder;
        this.availableBuckets = new boolean[1024];
        checkAvailableBuckets();
    }

    /**
     * Checks which of the bucket files are available.
     */
    private void checkAvailableBuckets() {
        for (int i = 0; i < 1024; i++) {
            File bucket = indexFolder.resolve(i + ".bin").toFile();
            if (bucket.exists()) {
                availableBuckets[i] = true;
            } else {
                logger.logWarning("Bucket " + i + " is missing.");
                availableBuckets[i] = false;
            }
        }
    }

    @Nullable
    public BucketReader getBucketReader(int bucket) {
        if (!availableBuckets[bucket]) {
            logger.logWarning("Bucket " + bucket + " is missing.");
            return null;
        }
        return new BucketReader(indexFolder.resolve(bucket + ".bin").toFile());
    }

    public void writeIndexStatistics(Tree tree, File output, int MAX_THREADS) {
        logger.logInfo("calculating index statistics ...");
        ProgressBar progressBar = new ProgressBar(1024, 20);
        Logger progressBarLogger = new OneLineLogger("Index", 1000).addElement(progressBar);
        final ConcurrentHashMap<String, Long> rankKmers = new ConcurrentHashMap<>();
        ThreadPoolExecutor threadPoolExecutor = new CustomThreadPoolExecutor(
                MAX_THREADS,
                MAX_THREADS,
                500L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(1024),
                new ThreadPoolExecutor.CallerRunsPolicy());

        for (int i = 0; i < 1024; i++) {

            if (availableBuckets[i]) {
                int finalI = i;
                threadPoolExecutor.submit(() -> {
                    BucketReader reader = getBucketReader(finalI);
                    if (reader != null) {
                        while (reader.hasNext()) {
                            long kmer = reader.next();
                            int taxId = (int) (kmer & 0x3FFFFF);
                            if (tree.idMap.containsKey(taxId)) {
                                String rank = tree.idMap.get(taxId).getRank();
                                rankKmers.put(rank, rankKmers.getOrDefault(rank, 0L) + 1);
                            }
                        }
                    }
                    progressBar.incrementProgress();
                });
            }
        }

        threadPoolExecutor.shutdown();
        try {
            threadPoolExecutor.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            logger.logError("Error waiting for thread pool to finish: " + e.getMessage());
        }

        progressBar.finish();
        for (String rank : rankKmers.keySet()) {
            logger.logInfo(rank + "\t" + rankKmers.get(rank));
        }
        try (FileWriter writer = new FileWriter(output)) {
            long totalKmers = 0;
            for (String rank : rankKmers.keySet()) {
                String line = rank + "\t" + rankKmers.get(rank) + "\n";
                writer.write(line);
                totalKmers += rankKmers.get(rank);
            }
            writer.write("Total kmers: " + totalKmers + "\n");
        } catch (IOException e) {
            logger.logError("Error writing to output file: " + e.getMessage());
        }
    }

    public Path getIndexFolder() {
        return indexFolder;
    }

    public boolean[] getAvailableBuckets() {
        return availableBuckets;
    }

    public boolean isBucketAvailable(int bucket) {
        return availableBuckets[bucket];
    }
}
