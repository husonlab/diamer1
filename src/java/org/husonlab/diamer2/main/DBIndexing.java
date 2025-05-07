package org.husonlab.diamer2.main;

import org.apache.commons.cli.CommandLine;
import org.husonlab.diamer2.indexing.DBIndexer;
import org.husonlab.diamer2.indexing.StatisticsEstimator;
import org.husonlab.diamer2.indexing.kmers.*;
import org.husonlab.diamer2.io.seq.FastaIdReader;
import org.husonlab.diamer2.io.seq.SequenceSupplierCompressed;
import org.husonlab.diamer2.main.encoders.Encoder;
import org.husonlab.diamer2.seq.alphabet.ReducedAlphabet;
import org.husonlab.diamer2.taxonomy.Tree;
import org.husonlab.diamer2.util.Pair;
import org.husonlab.diamer2.util.logging.Logger;

import java.nio.file.Path;
import java.util.Objects;

import static org.husonlab.diamer2.io.Utilities.getFile;
import static org.husonlab.diamer2.io.Utilities.getFolder;
import static org.husonlab.diamer2.main.CliUtils.*;

public class DBIndexing {
    /**
     * Class to parse the arguments for db indexing and start the indexing process.
     */

    protected static void indexDB(CommandLine cli, GlobalSettings globalSettings) {
        Logger logger = new Logger("DBIndexing");
        // input database, output folder
        checkNumberOfPositionalArguments(cli, 2);
        Path database = getFile(cli.getArgs()[0], true);
        Path output = getFolder(cli.getArgs()[1], false);
        boolean[] mask = getMask(cli);

        // setup log file
        LogFileWriter logFileWriter = new LogFileWriter(output.resolve("run.log"));
        logFileWriter.writeSettings(globalSettings);
        logFileWriter.writeTimeStamp("Indexing started");

        // parse tree
        Tree tree = readTree(cli);

        // parse alphabet or get default
        ReducedAlphabet alphabet = getAlphabet(cli);

        // setup kmer extractor and encoder with filtering options:
        KmerExtractor kmerExtractor = setupKmerExtractor(database, alphabet, mask, cli, globalSettings, logFileWriter, logger);
        Encoder encoder = new Encoder(alphabet, output, null, mask, kmerExtractor, globalSettings.BITS_FOR_IDS);

        try (SequenceSupplierCompressed sup = new SequenceSupplierCompressed(
                new FastaIdReader(database), alphabet::translateDBSequence, globalSettings.KEEP_IN_MEMORY)) {
            // estimate bucket sizes with first 10,000 sequences
            StatisticsEstimator statisticsEstimator = new StatisticsEstimator(sup, encoder, 10_000);
            int estimatedBucketSize = statisticsEstimator.getMaxBucketSize();
            logFileWriter.writeLog(statisticsEstimator.toString());
            if (!cli.hasOption("b")) {
                int suggestedNrOfBuckets = statisticsEstimator.getSuggestedNumberOfBuckets();
                logger.logInfo("Suggested number of buckets: " + suggestedNrOfBuckets);
                logFileWriter.writeLog("Suggested number of buckets: " + suggestedNrOfBuckets);
                globalSettings.setBUCKETS_PER_CYCLE(suggestedNrOfBuckets);
            }
            // starting indexing
            DBIndexer dbIndexer = new DBIndexer(sup, tree, estimatedBucketSize, encoder, globalSettings);
            String runInfo = dbIndexer.index();
            logFileWriter.writeTimeStamp("Indexing finished");
            logFileWriter.writeLog(runInfo);
        } catch (Exception e) {
            logFileWriter.writeLog("Indexing failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static double[] estimateProbabilities(Path database, ReducedAlphabet alphabet, boolean[] mask, GlobalSettings globalSettings) {
        // setup encoder without filtering to estimate AA probabilities
        KmerExtractor kmerExtractor = new KmerExtractor(new KmerEncoder(alphabet.getBase(), mask));
        Encoder encoder = new Encoder(alphabet, null, null, mask, kmerExtractor, globalSettings.BITS_FOR_IDS);
        try (SequenceSupplierCompressed sup = new SequenceSupplierCompressed(
                new FastaIdReader(database), alphabet::translateDBSequence, false)) {
            StatisticsEstimator statisticsEstimator = new StatisticsEstimator(sup, encoder, 10_000);
            return statisticsEstimator.getCharFrequencies();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static KmerExtractor setupKmerExtractor(Path database, ReducedAlphabet alphabet, boolean[] mask, CommandLine cli, GlobalSettings globalSettings, LogFileWriter logFileWriter, Logger logger) {

        // setup default encoder with complexity filtering
        KmerEncoder kmerEncoder = new KmerEncoder(alphabet.getBase(), mask);
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
                    logFileWriter.writeLog("Invalid complexity threshold: " + options[1]);
                    throw new RuntimeException("Invalid complexity threshold: " + options[1]);
                }
                if (threshold > 0) {
                    logFileWriter.writeLog("Filtering: c > " + threshold);
                    logger.logInfo("Filtering: c > " + threshold);
                    return new KmerExtractorFiltered(kmerEncoder, (kmer) -> kmerEncoder.getComplexity() > threshold);
                } else {
                    // without any filtering
                    logFileWriter.writeLog("Invalid complexity threshold, no filtering.");
                    logger.logInfo("Invalid complexity threshold, no filtering.");
                    return new KmerExtractor(new KmerEncoder(alphabet.getBase(), mask));
                }

            } else if (Objects.equals(options[0], "p")) {   // probability filtering
                // parse threshold parameter
                double threshold;
                try {
                    threshold = Double.parseDouble(options[1]);
                } catch (NumberFormatException e) {
                    logFileWriter.writeLog("Invalid probability threshold: " + options[1]);
                    throw new RuntimeException("Invalid probability threshold: " + options[1]);
                }
                if (threshold < 1) {
                    // estimate AA probabilities
                    double[] letterLikelihoods = estimateProbabilities(database, alphabet, mask, globalSettings);
                    // setup encoder with probability filtering
                    KmerEncoder finalKmerEncoder = new KmerEncoder(alphabet.getBase(), mask, letterLikelihoods);
                    logger.logInfo("Filtering: p < " + threshold);
                    logFileWriter.writeLog("Filtering: p < " + threshold);
                    return new KmerExtractorFiltered(finalKmerEncoder, (_) -> finalKmerEncoder.getProbability() > threshold);
                } else {
                    // without any filtering
                    logFileWriter.writeLog("Invalid probability threshold, no filtering.");
                    logger.logInfo("Invalid probability threshold, no filtering.");
                    return new KmerExtractor(new KmerEncoder(alphabet.getBase(), mask));
                }
            } else if (Objects.equals(options[0], "cm") || Objects.equals(options[0], "pm")) {
                // parse window size
                int windowSize;
                try {
                    windowSize = Integer.parseInt(options[1]);
                } catch (NumberFormatException e) {
                    logFileWriter.writeLog("Invalid window size: " + options[1]);
                    throw new RuntimeException("Invalid window size: " + options[1]);
                }
                if (Objects.equals(options[0], "cm")) {  // complexity maximization
                    logFileWriter.writeLog("Filtering: w=" + windowSize + " (complexity maximizer)");
                    logger.logInfo("Filtering: w=" + windowSize + " (complexity maximizer)");
                    return new KmerExtractorComplexityMaximizer(kmerEncoder, windowSize);
                } else if (Objects.equals(options[0], "pm")) {  // probability maximization
                    // estimate AA probabilities
                    double[] letterLikelihoods = estimateProbabilities(database, alphabet, mask, globalSettings);
                    // setup encoder with probability minimizer
                    KmerEncoder finalKmerEncoder = new KmerEncoder(alphabet.getBase(), mask, letterLikelihoods);
                    logFileWriter.writeLog("Filtering: w=" + windowSize + " (probability minimizer)");
                    logger.logInfo("Filtering: w=" + windowSize + " (probability minimizer)");
                    return new KmerExtractorProbabilityMinimizer(finalKmerEncoder, windowSize);
                }
            }
        }

        logFileWriter.writeLog("Filtering: c > 3 (default)");
        logger.logInfo("Filtering: c > 3 (default)");
        return kmerExtractor;
    }
}
