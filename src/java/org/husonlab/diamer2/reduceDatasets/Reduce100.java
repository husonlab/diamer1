package org.husonlab.diamer2.reduceDatasets;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Reduce100 {
    /*
    Extracts every 100th line from a file and writes it to a new file.
     */
    public static void main(String[] args) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(Paths.get(args[0]));
             BufferedWriter bw = Files.newBufferedWriter(Paths.get(args[1]))) {
            String line;
            int n = 0;
            while ((line = br.readLine()) != null) {
                if (n % 100 == 0) {
                    bw.write(line);
                    bw.newLine();
                }
                n++;
            }
        }
    }
}
