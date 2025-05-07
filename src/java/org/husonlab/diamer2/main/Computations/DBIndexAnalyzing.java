package org.husonlab.diamer2.main.Computations;

import org.apache.commons.cli.CommandLine;
import org.husonlab.diamer2.indexing.kmers.KmerExtractor;
import org.husonlab.diamer2.main.CliUtils;
import org.husonlab.diamer2.main.GlobalSettings;
import org.husonlab.diamer2.main.encoders.Encoder;
import org.husonlab.diamer2.main.encoders.EncoderWithoutKmerExtractor;
import org.husonlab.diamer2.util.DBIndexAnalyzer;

import static org.husonlab.diamer2.io.Utilities.getFolder;

public class DBIndexAnalyzing {
    public static void analyzeDBIndex(CommandLine cli, GlobalSettings settings) {
        // db index, output folder
        CliUtils.checkNumberOfPositionalArguments(cli, 2);
        settings.DB_INDEX = getFolder(cli.getArgs()[0], true);
        settings.logFileWriter.writeSettings(settings);

        Encoder encoder = new EncoderWithoutKmerExtractor(settings);
        DBIndexAnalyzer dbIndexAnalyzer = new DBIndexAnalyzer(encoder, settings);
        String runInfo = dbIndexAnalyzer.analyze();
        settings.logFileWriter.writeLog(runInfo);
    }
}
