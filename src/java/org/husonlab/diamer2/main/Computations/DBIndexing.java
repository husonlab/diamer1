package org.husonlab.diamer2.main.Computations;

import org.apache.commons.cli.CommandLine;
import org.husonlab.diamer2.indexing.DBIndexer;
import org.husonlab.diamer2.indexing.StatisticsEstimator;
import org.husonlab.diamer2.indexing.kmers.*;
import org.husonlab.diamer2.io.seq.FastaIdReader;
import org.husonlab.diamer2.io.seq.SequenceSupplier;
import org.husonlab.diamer2.io.seq.SequenceSupplierCompressed;
import org.husonlab.diamer2.main.GlobalSettings;
import org.husonlab.diamer2.main.encoders.Encoder;
import org.husonlab.diamer2.taxonomy.Tree;

import java.util.Objects;

import static org.husonlab.diamer2.io.Utilities.getFile;
import static org.husonlab.diamer2.io.Utilities.getFolder;
import static org.husonlab.diamer2.main.CliUtils.*;

public class DBIndexing {
    /**
     * Class to parse the arguments for db indexing and start the indexing process.
     */

    public static void indexDB(CommandLine cli, GlobalSettings settings) {
        // input database fasta file, output folder
        checkNumberOfPositionalArguments(cli, 2);
        settings.INPUT = getFile(cli.getArgs()[0], true);
        settings.DB_INDEX = getFolder(cli.getArgs()[1], false);

        settings.logFileWriter.writeSettings(settings);
        settings.logFileWriter.writeTimeStamp("Indexing started");

        // parse tree
        Tree tree = readTree(cli);

        // setup kmer extractor and encoder with filtering options:
        KmerExtractor kmerExtractor = setupKmerExtractor(settings.ALPHABET::translateDBSequence, cli, settings);

        Encoder encoder = new Encoder(settings, kmerExtractor);
        try (SequenceSupplierCompressed sup = new SequenceSupplierCompressed(
                new FastaIdReader(settings.INPUT), settings.ALPHABET::translateDBSequence, settings.KEEP_IN_MEMORY)) {
            // estimate bucket sizes with first 10,000 sequences
            StatisticsEstimator statisticsEstimator = new StatisticsEstimator(sup, encoder, 10_000);
            int estimatedBucketSize = statisticsEstimator.getMaxBucketSize();
            settings.logFileWriter.writeLog(statisticsEstimator.toString());
            if (!cli.hasOption("b")) {
                int suggestedNrOfBuckets = statisticsEstimator.getSuggestedNumberOfBuckets();
                settings.logger.logInfo("Suggested number of buckets: " + suggestedNrOfBuckets);
                settings.logFileWriter.writeLog("Suggested number of buckets: " + suggestedNrOfBuckets);
                settings.BUCKETS_PER_CYCLE = suggestedNrOfBuckets;
            }
            // starting indexing
            DBIndexer dbIndexer = new DBIndexer(sup, tree, estimatedBucketSize, encoder, settings);
            String runInfo = dbIndexer.index();
            settings.logFileWriter.writeTimeStamp("Indexing finished");
            settings.logFileWriter.writeLog(runInfo);
        } catch (Exception e) {
            settings.logFileWriter.writeLog("Indexing failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static double[] estimateProbabilities(SequenceSupplier.Converter<byte[]> converter, GlobalSettings settings) {
        // setup encoder without filtering to estimate AA probabilities
        KmerExtractor kmerExtractor = new KmerExtractor(new KmerEncoder(settings.ALPHABET.getBase(), settings.MASK));
        Encoder encoder = new Encoder(settings, kmerExtractor);
        try (SequenceSupplierCompressed sup = new SequenceSupplierCompressed(
                new FastaIdReader(settings.INPUT), converter, false)) {
            StatisticsEstimator statisticsEstimator = new StatisticsEstimator(sup, encoder, 10_000);
            return statisticsEstimator.getCharFrequencies();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static KmerExtractor setupKmerExtractor(SequenceSupplier.Converter<byte[]> converter, CommandLine cli, GlobalSettings settings) {

        // setup default encoder with complexity filtering
        KmerEncoder kmerEncoder = new KmerEncoder(settings.ALPHABET.getBase(), settings.MASK);
        KmerExtractor kmerExtractor = new KmerExtractorFiltered(kmerEncoder, (_) -> kmerEncoder.getComplexity() > 3);

        // parse filtering options
        if (cli.hasOption("filtering")) {
            String[] options = cli.getOptionValues("filtering");

            if (Objects.equals(options[0], "c")) {  // complexity filtering
                // parse threshold parameter
                int threshold;
                try {
                    threshold = Integer.parseInt(options[1]);
                } catch (NumberFormatException e) {
                    settings.logFileWriter.writeLog("Invalid complexity threshold: " + options[1]);
                    throw new RuntimeException("Invalid complexity threshold: " + options[1]);
                }
                if (threshold > 0) {
                    settings.logFileWriter.writeLog("Filtering: c > " + threshold);
                    settings.logger.logInfo("Filtering: c > " + threshold);
                    return new KmerExtractorFiltered(kmerEncoder, (kmer) -> kmerEncoder.getComplexity() > threshold);
                } else {
                    // without any filtering
                    settings.logFileWriter.writeLog("Invalid complexity threshold, no filtering.");
                    settings.logger.logInfo("Invalid complexity threshold, no filtering.");
                    return new KmerExtractor(new KmerEncoder(settings.ALPHABET.getBase(), settings.MASK));
                }

            } else if (Objects.equals(options[0], "p")) {   // probability filtering
                // parse threshold parameter
                double threshold;
                try {
                    threshold = Double.parseDouble(options[1]);
                } catch (NumberFormatException e) {
                    settings.logFileWriter.writeLog("Invalid probability threshold: " + options[1]);
                    throw new RuntimeException("Invalid probability threshold: " + options[1]);
                }
                if (threshold < 1) {
                    // estimate AA probabilities
                    double[] letterLikelihoods = estimateProbabilities(converter, settings);
                    // setup encoder with probability filtering
                    KmerEncoder finalKmerEncoder = new KmerEncoder(settings.ALPHABET.getBase(), settings.MASK, letterLikelihoods);
                    settings.logger.logInfo("Filtering: p < " + threshold);
                    settings.logFileWriter.writeLog("Filtering: p < " + threshold);
                    return new KmerExtractorFiltered(finalKmerEncoder, (_) -> finalKmerEncoder.getProbability() > threshold);
                } else {
                    // without any filtering
                    settings.logFileWriter.writeLog("Invalid probability threshold, no filtering.");
                    settings.logger.logInfo("Invalid probability threshold, no filtering.");
                    return new KmerExtractor(new KmerEncoder(settings.ALPHABET.getBase(), settings.MASK));
                }
            } else if (Objects.equals(options[0], "cm") || Objects.equals(options[0], "pm")) {
                // parse window size
                int windowSize;
                try {
                    windowSize = Integer.parseInt(options[1]);
                } catch (NumberFormatException e) {
                    settings.logFileWriter.writeLog("Invalid window size: " + options[1]);
                    throw new RuntimeException("Invalid window size: " + options[1]);
                }
                if (Objects.equals(options[0], "cm")) {  // complexity maximization
                    settings.logFileWriter.writeLog("Filtering: w=" + windowSize + " (complexity maximizer)");
                    settings.logger.logInfo("Filtering: w=" + windowSize + " (complexity maximizer)");
                    return new KmerExtractorComplexityMaximizer(kmerEncoder, windowSize);
                } else if (Objects.equals(options[0], "pm")) {  // probability maximization
                    // estimate AA probabilities
                    double[] letterLikelihoods = estimateProbabilities(converter, settings);
                    // setup encoder with probability minimizer
                    KmerEncoder finalKmerEncoder = new KmerEncoder(settings.ALPHABET.getBase(), settings.MASK, letterLikelihoods);
                    settings.logFileWriter.writeLog("Filtering: w=" + windowSize + " (probability minimizer)");
                    settings.logger.logInfo("Filtering: w=" + windowSize + " (probability minimizer)");
                    return new KmerExtractorProbabilityMinimizer(finalKmerEncoder, windowSize);
                }
            }
        }

        settings.logFileWriter.writeLog("Filtering: c > 3 (default)");
        settings.logger.logInfo("Filtering: c > 3 (default)");
        return kmerExtractor;
    }
}
