package org.husonlab.diamer2.io;

import org.husonlab.diamer2.seq.FASTA;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

public class FASTAReader {

    public static ArrayList<FASTA> read(String filename) throws IOException {
        /*
        @param filename: the name of the FASTA file
        @return: the last FASTA entry in the file
         */

        if (filename.endsWith(".gz")) {
            try (InputStream inputStream = new GZIPInputStream(Files.newInputStream(Paths.get(filename)))) {
                return readStream(inputStream);
            }
        } else {
            try (InputStream inputStream = Files.newInputStream(Paths.get(filename))) {
                return readStream(inputStream);
            }
        }
    }

    public static ArrayList<FASTA> readStream(InputStream inputStream) throws IOException {
        /*
        @param inputStream: the input stream of a FASTA file
        @return: the next FASTA entry in the file
         */
        ArrayList<FASTA> fastas = new ArrayList<>();
        String header = null;
        StringBuilder sequence = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.strip();
                if (line.startsWith(">")) {
                    if (header != null) {
                        fastas.add(new FASTA(header, sequence.toString()));
                        sequence = new StringBuilder();
                    }
                    header = line.substring(1);
                } else if (header != null) {
                    sequence.append(line);
                }
            }
            fastas.add(new FASTA(header, sequence.toString()));
        }
        return fastas;
    }
}
