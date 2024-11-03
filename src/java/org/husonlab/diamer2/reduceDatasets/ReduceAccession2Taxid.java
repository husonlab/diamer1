package org.husonlab.diamer2.reduceDatasets;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ReduceAccession2Taxid {
    /*
    Script to reduce the NCBI prot.accession2taxid.gz file to only the accessions that are in the input file.
     */
    public static void main(String[] args) throws Exception {
        HashSet<String> accessions = new HashSet<>();
        // read in the accessions
        try (BufferedReader br = Files.newBufferedReader(Paths.get(args[0]))) {
            String line;
            while ((line = br.readLine()) != null) {
                accessions.add(line);
            }
        }
        // read in the accession2taxid file and write out the lines that have an accession in the set to a new gzipped file
        try (FileInputStream fis = new FileInputStream(args[1]);
             GZIPInputStream gis = new GZIPInputStream(fis);
             BufferedReader br = new BufferedReader(new InputStreamReader(gis));
             OutputStream fos = Files.newOutputStream(Paths.get(args[2]));
             GZIPOutputStream gos = new GZIPOutputStream(fos);
             BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(gos))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split("\t");
                if (accessions.contains(values[1])) {
                    bw.write(line);
                    bw.newLine();
                }
            }
        }
    }
}
