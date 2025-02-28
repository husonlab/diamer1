package org.husonlab.diamer2.main;

import org.husonlab.diamer2.io.CountingInputStream;
import org.husonlab.diamer2.util.logging.OneLineLogger;
import org.husonlab.diamer2.util.logging.ProgressLogger;

import java.io.*;

public class ReadSpeedTest {
    public static void main(String[] args) {
        ProgressLogger progressLogger = new ProgressLogger("bytes", 100);
        new OneLineLogger("ReadSpeedTest", 100).addElement(progressLogger);
        try (CountingInputStream cis = new CountingInputStream(new FileInputStream(args[0]));
             BufferedReader br = new BufferedReader(new InputStreamReader(cis))) {
            while (br.readLine() != null) {
                progressLogger.setProgress(cis.getBytesRead());
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
