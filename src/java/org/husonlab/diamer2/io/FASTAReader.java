package org.husonlab.diamer2.io;

import org.husonlab.diamer2.seq.Sequence;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

public class FASTAReader implements AutoCloseable {

    private String header;
    private StringBuilder sequence;
    private BufferedReader br;
    private String line;

    public FASTAReader(BufferedReader br) throws IOException {
        this.header = null;
        this.sequence = new StringBuilder();
        this.br = br;
        line = br.readLine();
    }

    public Sequence next() throws IOException {
        if (line != null && line.startsWith(">")) {
            header = line;
            sequence = new StringBuilder();
            while ((line = br.readLine()) != null) {
                line = line.strip();
                if (line.startsWith(">")) {
                    return new Sequence(header, sequence.toString());
                } else {
                    sequence.append(line);
                }
            }
            return new Sequence(header, sequence.toString());
        } else {
            while ((line = br.readLine()) != null) {
                if (line.startsWith(">")) {
                    return next();
                }
            }
            return null;
        }
    }

    public ArrayList<Sequence> next(int n) throws IOException {
        ArrayList<Sequence> sequences = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Sequence seq = next();
            if (seq == null) {
                break;
            }
            sequences.add(seq);
        }
        return sequences;
    }

    public static ArrayList<Sequence> read(String filename) throws IOException {
        /*
        @param filename: the name of the Sequence file
        @return: the last Sequence entry in the file
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

    public static ArrayList<Sequence> readStream(InputStream inputStream) throws IOException {
        /*
        @param inputStream: the input stream of a Sequence file
        @return: the next Sequence entry in the file
         */
        ArrayList<Sequence> sequences = new ArrayList<>();
        String header = null;
        StringBuilder sequence = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.strip();
                if (line.startsWith(">")) {
                    if (header != null) {
                        sequences.add(new Sequence(header, sequence.toString()));
                        sequence = new StringBuilder();
                    }
                    header = line.substring(1);
                } else if (header != null) {
                    sequence.append(line);
                }
            }
            sequences.add(new Sequence(header, sequence.toString()));
        }
        return sequences;
    }

    @Override
    public void close() throws IOException {
        br.close();
    }
}
