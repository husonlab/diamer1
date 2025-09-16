package org.husonlab.diamer.util.logging;

import org.husonlab.diamer.main.GlobalSettings;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.husonlab.diamer.io.Utilities.getFile;
import static org.husonlab.diamer.io.Utilities.getFolder;

public class LogFileWriter {
    private final Path output;
    public LogFileWriter(Path output) {
        this.output = output;
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(getFile(output.toString(), false).toString())))) {
            writer.println(LocalDateTime.now());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeSettings(GlobalSettings globalSettings) {
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(output.toFile(), true)))) {
            writer.print(globalSettings.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeTimeStamp(String prefix) {
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(output.toFile(), true)))) {
            writer.println(prefix + ": " + LocalDateTime.now());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeLog(String log) {
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(output.toFile(), true)))) {
            writer.println(log);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
