package org.husonlab.diamer2.reduceDatasets;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;

public class ExtractByAcession {
    /*
    Extracts the sequences from a multi FASTA file that have a specific accession number and writes them to a new file.
    args[0]: path to the file with the accession numbers (one per line)
    args[1]: path to the input FASTA file
    args[2]: path to the output file
     */
    public static void main(String[] args) throws Exception {
        // read the accession numbers
        HashSet<String> accessions = new HashSet<>();
        try (BufferedReader br = Files.newBufferedReader(Paths.get(args[0]))) {
            String line;
            while ((line = br.readLine()) != null) {
                accessions.add(line);
            }
        }
        // extract the sequences
        try (BufferedReader br = Files.newBufferedReader(Paths.get(args[1]));
             BufferedWriter bw = Files.newBufferedWriter(Paths.get(args[2]))) {
            String line;
            boolean write = false;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(">")) {
                    String[] values = line.split(" ");
                    String accession = values[0].substring(1);
                    write = accessions.contains(accession);
                }
                if (write) {
                    bw.write(line);
                    bw.newLine();
                }
            }
        }
    }
}
