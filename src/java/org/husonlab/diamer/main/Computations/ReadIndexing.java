package org.husonlab.diamer.main.Computations;

import org.apache.commons.cli.CommandLine;
import org.husonlab.diamer.indexing.ReadIndexer;
import org.husonlab.diamer.indexing.StatisticsEstimator;
import org.husonlab.diamer.io.seq.FastqIdReader;
import org.husonlab.diamer.io.seq.SequenceSupplierCompressed;
import org.husonlab.diamer.main.GlobalSettings;
import org.husonlab.diamer.main.encoders.Encoder;

import static org.husonlab.diamer.io.Utilities.getFile;
import static org.husonlab.diamer.io.Utilities.getFolder;
import static org.husonlab.diamer.main.CliUtils.*;
import static org.husonlab.diamer.main.Computations.DBIndexing.setupEncoder;

public class ReadIndexing {
    public static void indexReads(CommandLine cli, GlobalSettings settings) {
        // input reads, output folder
        checkNumberOfPositionalArguments(cli, 2);
        settings.INPUT = getFile(cli.getArgs()[0], true);
        settings.READS_INDEX = getFolder(cli.getArgs()[1], false);

        settings.logFileWriter.writeSettings(settings);
        settings.logFileWriter.writeTimeStamp("Indexing started");

        // setup kmer extractor and encoder with filtering options:
        Encoder encoder = setupEncoder(new FastqIdReader(settings.INPUT), settings.ALPHABET::translateRead, cli, settings);

        try (FastqIdReader fastqIdReader = new FastqIdReader(settings.INPUT);
                SequenceSupplierCompressed sup = new SequenceSupplierCompressed(
                        fastqIdReader, settings.ALPHABET::translateRead, settings.KEEP_IN_MEMORY)) {
            // estimate bucket sizes with first 10,000 sequences
            StatisticsEstimator statisticsEstimator = new StatisticsEstimator(sup, encoder, 1_000);
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
            ReadIndexer readIndexer = new ReadIndexer(sup, fastqIdReader, estimatedBucketSize, encoder, settings);
            String runInfo = readIndexer.index();
            settings.logFileWriter.writeTimeStamp("Indexing finished");
            settings.logFileWriter.writeLog(runInfo);
        } catch (Exception e) {
            settings.logFileWriter.writeLog("Indexing failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
