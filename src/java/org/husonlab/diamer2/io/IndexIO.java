package org.husonlab.diamer2.io;

import org.husonlab.diamer2.indexing.CustomThreadPoolExecutor;
import org.husonlab.diamer2.logging.Logger;
import org.husonlab.diamer2.logging.OneLineLogger;
import org.husonlab.diamer2.logging.ProgressBar;
import org.husonlab.diamer2.taxonomy.Tree;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Represents an index folder.
 */
public class IndexIO {
    protected final Logger logger;
    protected final Path indexFolder;
    protected final BucketIO[] bucketIOs;

    /**
     * Create a new DBIndexIO object.
     * @param indexFolder path to the index folder
     * is missing
     */
    public IndexIO(Path indexFolder) {
        this.logger = new Logger("DBIndexIO");
        if (!indexFolder.toFile().isDirectory()) {
            if (!indexFolder.toFile().isDirectory()) {
                if (!indexFolder.toFile().mkdirs()) {
                    throw new RuntimeException("Could not create index folder: " + indexFolder);
                }
            }
        }
        this.indexFolder = indexFolder;
        this.bucketIOs = new BucketIO[1024];
        for (int i = 0; i < 1024; i++) {
            bucketIOs[i] = new BucketIO(indexFolder.resolve(i + ".bin").toFile(), i);
        }
    }

    /**
     * Checks which of the bucket files are available.
     * @return true if all buckets are available, false otherwise
     */
    public boolean bucketMissing() {
        boolean bucketMissing = false;
        for (int i = 0; i < 1024; i++) {
            if (!bucketIOs[i].exists()) {
                bucketMissing = true;
            }
        }
        return bucketMissing;
    }

    public BucketIO.BucketReader getBucketReader(int bucketName) {
        if (bucketIOs[bucketName].exists()) {
            return bucketIOs[bucketName].getBucketReader();
        } else {
            throw new RuntimeException("Bucket " + bucketName + " is missing.");
        }
    }

    public BucketIO getBucketIO(int bucketName) {
        return bucketIOs[bucketName];
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

        for (BucketIO bucketIO : bucketIOs) {
            if (bucketIO.exists()) {
                threadPoolExecutor.submit(() -> {
                    BucketIO.BucketReader reader = bucketIO.getBucketReader();
                    while (reader.hasNext()) {
                        long kmer = reader.next();
                        int taxId = (int) (kmer & 0x3FFFFF);
                        if (tree.idMap.containsKey(taxId)) {
                            String rank = tree.idMap.get(taxId).getRank();
                            rankKmers.put(rank, rankKmers.getOrDefault(rank, 0L) + 1);
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
            throw new RuntimeException("Error waiting for thread pool to finish.", e);
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

    public boolean isBucketAvailable(int bucket) {
        return bucketIOs[bucket].exists();
    }
}
