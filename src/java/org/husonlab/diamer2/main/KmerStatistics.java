package org.husonlab.diamer2.main;

import org.husonlab.diamer2.indexing.CustomThreadPoolExecutor;
import org.husonlab.diamer2.indexing.kmers.KmerEncoder;
import org.husonlab.diamer2.indexing.kmers.KmerExtractor;
import org.husonlab.diamer2.io.seq.FastaIdReader;
import org.husonlab.diamer2.io.seq.FutureSequenceRecords;
import org.husonlab.diamer2.io.seq.SequenceSupplier;
import org.husonlab.diamer2.seq.SequenceRecord;
import org.husonlab.diamer2.seq.alphabet.AA;
import org.husonlab.diamer2.seq.alphabet.Base11Custom;
import org.husonlab.diamer2.seq.converter.AAtoBase11Custom;
import org.husonlab.diamer2.util.StatisticsCollector;
import org.husonlab.diamer2.util.logging.Logger;
import org.husonlab.diamer2.util.logging.OneLineLogger;
import org.husonlab.diamer2.util.logging.ProgressBar;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.husonlab.diamer2.io.Utilities.getFile;
import static org.husonlab.diamer2.io.Utilities.getFolder;
import static org.husonlab.diamer2.main.Main.parseMask;

public class KmerStatistics {
    /**
     * @param args threads, alphabet, mask, bucket range start, bucket range end, input, output
     */
    public static void main(String[] args) throws IOException {
        int threads = Integer.parseInt(args[0]);
        String alphabet = args[1];
        String maskString = args[2];
        final int bucketRangeStart = Integer.parseInt(args[3]);
        final int bucketRangeEnd = Integer.parseInt(args[4]);
        Path input = getFile(args[5], true);
        Path output = getFolder(args[6], false);
        AAtoBase11Custom aaToBase11Custom = new AAtoBase11Custom(alphabet);
        int base = aaToBase11Custom.getBase();
        int bins = 1000;
        boolean[] mask = parseMask(maskString);
        KmerEncoder kmerEncoder = new KmerEncoder(base, mask);
        long maxKmerValue = (long)Math.pow(11, kmerEncoder.getK() - kmerEncoder.getS());
        final StatisticsCollector statisticsCollector = new StatisticsCollector(maxKmerValue, bins);

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(output.resolve("readme.txt").toString()))) {
            bw.write("start: " + LocalDateTime.now() + "\n");
            bw.write("input: " + input + "\n");
            bw.write("output: " + output + "\n");
            bw.write("alphabet: " + alphabet + "\n");
            bw.write("mask: " + maskString + "\n");
            bw.write("bucket range: " + bucketRangeStart + " - " + bucketRangeEnd + "\n");
        }

        try (SequenceSupplier<Integer, Character, AA, Byte, Base11Custom> sup = new SequenceSupplier<>(
                new FastaIdReader<>(input, new AA()), aaToBase11Custom, false);
             CustomThreadPoolExecutor executor = new CustomThreadPoolExecutor(1, threads, 1000, 20, new Logger("ThreadPoolExecutor"))) {

            ProgressBar progressBar = new ProgressBar(sup.getFileSize(), 20);
            new OneLineLogger("StatisticsCollector", 500).addElement(progressBar);

            FutureSequenceRecords<Integer, Byte, Base11Custom> futureSequenceRecords;
            while ((futureSequenceRecords = sup.next()) != null) {
                progressBar.setProgress(sup.getBytesRead());
                FutureSequenceRecords<Integer, Byte, Base11Custom> finalFutureSequenceRecords = futureSequenceRecords;
                executor.submit(() -> {
                    KmerExtractor kmerExtractor = new KmerExtractor(new KmerEncoder(base, mask));
                    for (SequenceRecord<Integer, Byte, Base11Custom> record : finalFutureSequenceRecords.getSequenceRecords()) {
                        for (long kmer: kmerExtractor.extractKmers(record.sequence())) {
                            statisticsCollector.addToHistogram(kmer);
                            int bucket = (int)(kmer & 0x3FFL);
                            if (bucket >= bucketRangeStart && bucket < bucketRangeEnd) {
                                statisticsCollector.addToKmerCounts(kmer);
                            }
                        }
                    }
                });
            }
            progressBar.finish();
        }
        statisticsCollector.writeStatistics(output);
    }
}
