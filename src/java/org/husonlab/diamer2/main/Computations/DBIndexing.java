package org.husonlab.diamer2.main.Computations;

import org.apache.commons.cli.CommandLine;
import org.husonlab.diamer2.indexing.DBIndexer;
import org.husonlab.diamer2.indexing.StatisticsEstimator;
import org.husonlab.diamer2.indexing.kmers.*;
import org.husonlab.diamer2.io.seq.*;
import org.husonlab.diamer2.main.GlobalSettings;
import org.husonlab.diamer2.main.encoders.Encoder;
import org.husonlab.diamer2.main.encoders.EncoderWithoutKmerExtractor;
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
        Encoder encoder = setupEncoder(new FastaIdReader(settings.INPUT), settings.ALPHABET::translateDBSequence, cli, settings);

        try (SequenceSupplierCompressed sup = new SequenceSupplierCompressed(
                new FastaIdReader(settings.INPUT), settings.ALPHABET::translateDBSequence, settings.KEEP_IN_MEMORY)) {
            // estimate bucket sizes with first 10,000 sequences
            StatisticsEstimator statisticsEstimator = new StatisticsEstimator(sup, encoder, 10_000);
            int estimatedBucketSize = statisticsEstimator.getMaxBucketSize();
            if (estimatedBucketSize < 1) {
                settings.logFileWriter.writeLog("Estimated bucket size is too small (" + estimatedBucketSize +
                        "), consider using different filtering options.");
                throw new RuntimeException("Estimated bucket size is too small (" + estimatedBucketSize +
                        "), consider using different filtering options.");
            }
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

    private static double[] estimateProbabilities(SequenceReader<Integer, char[]> reader, SequenceSupplier.Converter<byte[]> converter, GlobalSettings settings) {
        // setup encoder without filtering to estimate AA probabilities
        Encoder encoder = new Encoder(settings) {
            @Override
            public KmerExtractor getKmerExtractor() {
                return new KmerExtractor(new KmerEncoder(settings.ALPHABET.getBase(), settings.MASK));
            }
        };
        try (SequenceSupplier<Integer, byte[]> sup = new SequenceSupplier<>(
                reader, converter, false)) {
            StatisticsEstimator statisticsEstimator = new StatisticsEstimator(sup, encoder, 10_000);
            return statisticsEstimator.getCharFrequencies();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static Encoder setupEncoder(SequenceReader<Integer, char[]> reader, SequenceSupplier.Converter<byte[]> converter, CommandLine cli, GlobalSettings settings) {

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
                    if (threshold > new EncoderWithoutKmerExtractor(settings).getW()) {
                        settings.logFileWriter.writeLog("Complexity threshold is too high: " + threshold);
                        throw new RuntimeException("Complexity threshold is too high: " + threshold);
                    }
                    settings.logFileWriter.writeLog("Filtering: c > " + threshold);
                    settings.logger.logInfo("Filtering: c > " + threshold);
                    return new Encoder(settings) {
                        @Override
                        public KmerExtractor getKmerExtractor() {
                            // setup default encoder with complexity filtering
                            KmerEncoder kmerEncoder = new KmerEncoder(settings.ALPHABET.getBase(), settings.MASK);
                            return new KmerExtractorFiltered(kmerEncoder, (_) -> kmerEncoder.getComplexity() > threshold);
                        }
                    };
                } else {
                    // without any filtering
                    settings.logFileWriter.writeLog("No filtering.");
                    settings.logger.logInfo("No filtering.");
                    return new Encoder(settings) {
                        @Override
                        public KmerExtractor getKmerExtractor() {
                            return new KmerExtractor(new KmerEncoder(settings.ALPHABET.getBase(), settings.MASK));
                        }
                    };
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
                    if (threshold < 0) {
                        settings.logFileWriter.writeLog("Probability threshold is too low: " + threshold);
                        throw new RuntimeException("Probability threshold is too low: " + threshold);
                    }
                    // estimate AA probabilities
                    double[] letterLikelihoods = estimateProbabilities(reader, converter, settings);
                    // setup encoder with probability filtering
                    settings.logger.logInfo("Filtering: p < " + threshold);
                    settings.logFileWriter.writeLog("Filtering: p < " + threshold);
                    return new Encoder(settings) {
                        @Override
                        public KmerExtractor getKmerExtractor() {
                            KmerEncoder kmerEncoder = new KmerEncoder(settings.ALPHABET.getBase(), settings.MASK, letterLikelihoods);
                            return new KmerExtractorFiltered(kmerEncoder, (_) -> kmerEncoder.getProbability() < threshold);
                        }
                    };
                } else {
                    // without any filtering
                    settings.logFileWriter.writeLog("No filtering.");
                    settings.logger.logInfo("No filtering.");
                    return new Encoder(settings) {
                        @Override
                        public KmerExtractor getKmerExtractor() {
                            return new KmerExtractor(new KmerEncoder(settings.ALPHABET.getBase(), settings.MASK));
                        }
                    };
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
                if (windowSize <= settings.MASK.length) {
                    settings.logFileWriter.writeLog("Window size is too small: " + windowSize);
                    throw new RuntimeException("Window size is too small: " + windowSize);
                }
                if (Objects.equals(options[0], "cm")) {  // complexity maximization
                    settings.logFileWriter.writeLog("Filtering: w=" + windowSize + " (complexity maximizer)");
                    settings.logger.logInfo("Filtering: w=" + windowSize + " (complexity maximizer)");
                    return new Encoder(settings) {
                        @Override
                        public KmerExtractor getKmerExtractor() {
                            KmerEncoder kmerEncoder = new KmerEncoder(settings.ALPHABET.getBase(), settings.MASK);
                            return new KmerExtractorComplexityMaximizer(kmerEncoder, windowSize);
                        }
                    };
                } else if (Objects.equals(options[0], "pm")) {  // probability maximization
                    // estimate AA probabilities
                    double[] letterLikelihoods = estimateProbabilities(reader, converter, settings);
                    // setup encoder with probability minimizer
                    settings.logFileWriter.writeLog("Filtering: w=" + windowSize + " (probability minimizer)");
                    settings.logger.logInfo("Filtering: w=" + windowSize + " (probability minimizer)");
                    return new Encoder(settings) {
                        @Override
                        public KmerExtractor getKmerExtractor() {
                            KmerEncoder finalKmerEncoder = new KmerEncoder(settings.ALPHABET.getBase(), settings.MASK, letterLikelihoods);
                            return new KmerExtractorProbabilityMinimizer(finalKmerEncoder, windowSize);
                        }
                    };
                }
            }
        }

        settings.logFileWriter.writeLog("Filtering: c > 3 (default)");
        settings.logger.logInfo("Filtering: c > 3 (default)");
        return new Encoder(settings) {
            @Override
            public KmerExtractor getKmerExtractor() {
                KmerEncoder kmerEncoder = new KmerEncoder(settings.ALPHABET.getBase(), settings.MASK);
                return new KmerExtractorFiltered(kmerEncoder, (_) -> kmerEncoder.getComplexity() > 3);
            }
        };
    }
}
