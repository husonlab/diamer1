package org.husonlab.diamer2.util.logging;

import org.husonlab.diamer2.main.GlobalSettings;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class LogFileWriter {
    private final Path output;
    public LogFileWriter(Path output) {
        this.output = output;
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(output.toFile())))) {
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
