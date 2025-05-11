package org.husonlab.diamer.main.Computations;

import org.apache.commons.cli.CommandLine;
import org.husonlab.diamer.main.CliUtils;
import org.husonlab.diamer.main.GlobalSettings;
import org.husonlab.diamer.main.encoders.Encoder;
import org.husonlab.diamer.main.encoders.EncoderWithoutKmerExtractor;
import org.husonlab.diamer.util.DBIndexAnalyzer;

import static org.husonlab.diamer.io.Utilities.getFolder;

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
