package org.husonlab.diamer2.indexing;

import org.husonlab.diamer2.indexing.kmers.KmerExtractor;
import org.husonlab.diamer2.io.indexing.DBIndexIO;
import org.husonlab.diamer2.io.seq.FutureSequenceRecords;
import org.husonlab.diamer2.io.seq.SequenceSupplier;
import org.husonlab.diamer2.main.GlobalSettings;
import org.husonlab.diamer2.main.encoders.Encoder;
import org.husonlab.diamer2.seq.SequenceRecord;
import org.husonlab.diamer2.taxonomy.Tree;

import java.nio.file.Path;
import java.util.concurrent.*;

public class DBIndexer2 {

    private final static int expectedKmerCount = 400000000;
    private final SequenceSupplier<Integer, byte[]> sup;
    private final Tree tree;
    private final Encoder encoder;
    private final GlobalSettings settings;
    private final int parallelism;
    BlockingQueue<FutureSequenceRecords<Integer, byte[]>[]>[] queues;
    private long[][] kmers;
    private int[][] taxIds;
    private Boolean finished = false;
    private final DBIndexIO dbIndexIO;

    public DBIndexer2(SequenceSupplier<Integer, byte[]> sup,
                     Path indexDir,
                     Tree tree,
                     Encoder encoder,
                     GlobalSettings settings) {
        this.sup = sup;
        this.tree = tree;
        this.encoder = encoder;
        dbIndexIO = encoder.getDBIndexIO();
        this.settings = settings;
        parallelism = Math.min(settings.MAX_THREADS, settings.BUCKETS_PER_CYCLE);
        queues = new BlockingQueue[parallelism];
        kmers = new long[parallelism][];
        taxIds = new int[parallelism][];
        for (int i = 0; i < parallelism; i++) {
            queues[i] = new ArrayBlockingQueue<>(settings.QUEUE_SIZE);
            kmers[i] = new long[expectedKmerCount];
            taxIds[i] = new int[expectedKmerCount];
        }
    }

    public String index() {
        for (int i = 0; i < encoder.getNrOfBuckets(); i += parallelism) {
            Thread readerThread = new Thread(() -> batchSupplier(sup, queues, settings.SEQUENCE_BATCH_SIZE));
            readerThread.start();

            Thread[] threads = new Thread[parallelism];
            for (int j = 0; j < parallelism; j++) {
                int bucketIndex = i + j;
                int finalJ = j;
                threads[j] = new Thread(() -> batchProcessor(bucketIndex, queues[finalJ], kmers[bucketIndex], taxIds[bucketIndex], tree, encoder, finished));
                threads[j].start();
            }

            try {
                readerThread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            finished = true;

            for (int j = 0; j < parallelism; j++) {
                try {
                    threads[j].join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            System.out.println("Finished");
        }
        return "Finished";
    }

    private static void batchSupplier(SequenceSupplier<Integer, byte[]> sup,
                                      BlockingQueue<FutureSequenceRecords<Integer, byte[]>[]>[] queues,
                                      int batchSize) {
        try {
            FutureSequenceRecords<Integer, byte[]>[] batch = new FutureSequenceRecords[batchSize];
            FutureSequenceRecords<Integer, byte[]> futureSequenceRecords;
            int batchIndex = 0;
            while ((futureSequenceRecords = sup.next()) != null) {
                batch[batchIndex++] = futureSequenceRecords;
                if (batchIndex == batchSize) {
                    try {
                        for (BlockingQueue<FutureSequenceRecords<Integer, byte[]>[]> queue : queues) {
                            queue.put(batch);
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    batchIndex = 0;
                    batch = new FutureSequenceRecords[batchSize];
                }
            }
            try {
                for (BlockingQueue<FutureSequenceRecords<Integer, byte[]>[]> queue : queues) {
                    queue.put(batch);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            sup.reset();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void batchProcessor(int bucketIndex,
                                       BlockingQueue<FutureSequenceRecords<Integer, byte[]>[]> queue,
                                       long[] kmers,
                                       int[] taxIds,
                                       Tree tree,
                                       Encoder encoder,
                                       Boolean finished) {
        int kmerIndex = 0;
        KmerExtractor kmerExtractor = encoder.getKmerExtractor();
        try {
            while (true) {
                FutureSequenceRecords<Integer, byte[]>[] batch = queue.poll(2, TimeUnit.SECONDS);
                if (batch == null && finished) {
                    break;
                }
                for (FutureSequenceRecords<Integer, byte[]> futureSequenceRecords : batch) {
                    if (futureSequenceRecords == null) {
                        break;
                    }
                    for (SequenceRecord<Integer, byte[]> sequenceRecord : futureSequenceRecords.getSequenceRecords()) {
                        int taxId = sequenceRecord.id();
                        if (!tree.hasNode(taxId)) continue;
                        for (long kmer : kmerExtractor.extractKmers(sequenceRecord.sequence())) {
                            if (encoder.getBucketNameFromKmer(kmer) == bucketIndex) {
                                kmers[kmerIndex] = kmer;
                                taxIds[kmerIndex] = taxId;
                                kmerIndex++;
                            }
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
