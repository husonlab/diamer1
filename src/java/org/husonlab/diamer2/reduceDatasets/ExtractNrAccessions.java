package org.husonlab.diamer2.reduceDatasets;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ExtractNrAccessions {
    /*
    Extracts the accession numbers from the headers of a multi Sequence file and writes them to a new file.
    args[0]: path to the input Sequence file
    args[1]: path to the output file
     */
    public static void main(String[] args) throws Exception {
        try (BufferedReader br = Files.newBufferedReader(Paths.get(args[0]));
             BufferedWriter bw = Files.newBufferedWriter(Paths.get(args[1]))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(">")) {
                    String[] values = line.split(" ");
                    String accession = values[0].substring(1);
                    bw.write(accession);
                    bw.newLine();
                }
            }
        }
    }
}
