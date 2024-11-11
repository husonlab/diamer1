package org.husonlab.diamer2.main;


import org.husonlab.diamer2.seq.FASTA;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.husonlab.diamer2.alphabet.AminoAcids.to11Num_15;

public class TestParallel {
    private final int MAX_THREADS;
    private final int MAX_QUEUE_SIZE;
    private final int FASTA_BATCH_SIZE;
    private final short[] bucketRange;
    private final String dbPath;
    private final ThreadPoolExecutor fastaExecutor;
    // Array of ConcurrentHashMaps to store the index
    private final ConcurrentHashMap<Long, Short>[] kmerMap;

    public TestParallel(int MAX_THREADS, int MAX_QUEUE_SIZE, int FASTA_BATCH_SIZE, short[] bucketRange, String dbPath) {
        this.MAX_THREADS = MAX_THREADS;
        this.MAX_QUEUE_SIZE = MAX_QUEUE_SIZE;
        this.FASTA_BATCH_SIZE = FASTA_BATCH_SIZE;
        this.bucketRange = bucketRange;
        this.dbPath = dbPath;
        this.fastaExecutor = new ThreadPoolExecutor(
                MAX_THREADS,
                MAX_THREADS,
                500L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(MAX_QUEUE_SIZE),
                new ThreadPoolExecutor.CallerRunsPolicy());
        this.kmerMap = new ConcurrentHashMap[bucketRange[1] - bucketRange[0]];
        for (int i = 0; i < bucketRange[1] - bucketRange[0]; i++) {
            kmerMap[i] = new ConcurrentHashMap<Long, Short>();
        }
    }



    public void run(String[] args) throws IOException {
        String header = null;
        StringBuilder sequence = new StringBuilder();
        FASTA[] fastas = new FASTA[FASTA_BATCH_SIZE];

        try (BufferedReader br = new BufferedReader(new FileReader(dbPath))) {
            String line;
            int fastaCount = 0;
            long fastaTime = System.currentTimeMillis();
            boolean newData = false;

            while ((line = br.readLine()) != null) {
                line = line.strip();
                if (line.startsWith(">")) {
                    if (header != null) {
                        fastas[fastaCount % FASTA_BATCH_SIZE] = new FASTA(header, sequence.toString());
                        fastaCount++;
                        newData = true;
                        sequence = new StringBuilder();
                    }
                    header = line.substring(1);
                } else if (header != null) {
                    sequence.append(line);
                }
                if (fastaCount % FASTA_BATCH_SIZE == 0 && newData) {
                    processFasta(fastas);
                    newData = false;
                    if (fastaCount % 100000 == 0) {
                        long sequencesPerSecond = 100000 * 1000L / (System.currentTimeMillis() - fastaTime);
                        fastaTime = System.currentTimeMillis();
                        System.out.println("Processed " + (float)fastaCount/(float)1000000 + "M sequences. " + sequencesPerSecond + " sequences per second.");
                    }
                }
            }
            fastas[fastaCount % FASTA_BATCH_SIZE] = new FASTA(header, sequence.toString());
            processFasta(fastas);
        }
        fastaExecutor.shutdown();
        try {
            fastaExecutor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (int j = 0; j < kmerMap.length; j++) {
            System.out.println("Bucket " + j + ": " + kmerMap[j].size());
        }
    }

    public Map<Long, Short>[] getKmerMap() {
        return kmerMap;
    }

    public boolean isRunning() {
        return fastaExecutor.getActiveCount() > 0;
    }

    public void shutdown() {
        fastaExecutor.shutdown();
    }

    private void processFasta(FASTA[] fastas) {
        FastaProcessor processor = new FastaProcessor(fastas);

        try {
            fastaExecutor.execute(processor);
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    private class FastaProcessor implements Runnable {
        private final FASTA[] fastas;

        public FastaProcessor(FASTA[] fastas) {
            this.fastas = fastas;
        }

        @Override
        public void run() {
            for (FASTA fasta : fastas) {
                String sequence = fasta.getSequence();
                for (int i = 0; i + 15 < sequence.length(); i++) {
                    String kmer = sequence.substring(i, i + 15);
                    long kmerEnc = to11Num_15(kmer);
                    short bucketId = (short) (kmerEnc & 0b1111111111);

                    if (bucketId < bucketRange[1] - bucketRange[0]) {
                        kmerMap[bucketId].computeIfAbsent(kmerEnc, k -> (short) 0);
                        kmerMap[bucketId].computeIfPresent(kmerEnc, (k, v) -> (short) (v + 1));
                    }
                }
            }
        }
    }
}
