package org.husonlab.diamer2.main;

import org.apache.commons.cli.CommandLine;
import org.husonlab.diamer2.indexing.ReadIndexer;
import org.husonlab.diamer2.indexing.StatisticsEstimator;
import org.husonlab.diamer2.indexing.kmers.KmerExtractor;
import org.husonlab.diamer2.io.seq.FastqIdReader;
import org.husonlab.diamer2.io.seq.SequenceSupplierCompressed;
import org.husonlab.diamer2.main.encoders.Encoder;
import org.husonlab.diamer2.seq.alphabet.ReducedAlphabet;
import org.husonlab.diamer2.util.Pair;
import org.husonlab.diamer2.util.logging.Logger;

import java.nio.file.Path;

import static org.husonlab.diamer2.io.Utilities.getFile;
import static org.husonlab.diamer2.io.Utilities.getFolder;
import static org.husonlab.diamer2.main.CliUtils.*;
import static org.husonlab.diamer2.main.DBIndexing.setupKmerExtractor;

public class ReadIndexing {
    public static void indexReads(CommandLine cli, GlobalSettings globalSettings) {
        Logger logger = new Logger("ReadIndexing");
        // input reads, output folder
        checkNumberOfPositionalArguments(cli, 2);
        Path reads = getFile(cli.getArgs()[0], true);
        Path output = getFolder(cli.getArgs()[1], false);
        boolean[] mask = getMask(cli);

        // setup log file
        LogFileWriter logFileWriter = new LogFileWriter(output.resolve("run.log"));
        logFileWriter.writeSettings(globalSettings);
        logFileWriter.writeTimeStamp("Indexing started");

        // parse alphabet or get default
        ReducedAlphabet alphabet = getAlphabet(cli);

        // setup kmer extractor and encoder with filtering options:
        KmerExtractor kmerExtractor = setupKmerExtractor(reads, alphabet, mask, cli, globalSettings, logFileWriter, logger);
        Encoder encoder = new Encoder(alphabet, null, output, mask, kmerExtractor, globalSettings.BITS_FOR_IDS);

        try (FastqIdReader fastqIdReader = new FastqIdReader(reads);
                SequenceSupplierCompressed sup = new SequenceSupplierCompressed(
                        fastqIdReader, alphabet::translateRead, globalSettings.KEEP_IN_MEMORY)) {
            // estimate bucket sizes with first 10,000 sequences
            StatisticsEstimator statisticsEstimator = new StatisticsEstimator(sup, encoder, 1_000);
            int estimatedBucketSize = statisticsEstimator.getMaxBucketSize();
            logFileWriter.writeLog(statisticsEstimator.toString());
            if (!cli.hasOption("b")) {
                int suggestedNrOfBuckets = statisticsEstimator.getSuggestedNumberOfBuckets();
                logger.logInfo("Suggested number of buckets: " + suggestedNrOfBuckets);
                logFileWriter.writeLog("Suggested number of buckets: " + suggestedNrOfBuckets);
                globalSettings.setBUCKETS_PER_CYCLE(suggestedNrOfBuckets);
            }
            // starting indexing
            ReadIndexer readIndexer = new ReadIndexer(sup, fastqIdReader, estimatedBucketSize, encoder, globalSettings);
            String runInfo = readIndexer.index();
            logFileWriter.writeTimeStamp("Indexing finished");
            logFileWriter.writeLog(runInfo);
        } catch (Exception e) {
            logFileWriter.writeLog("Indexing failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
