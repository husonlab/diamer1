package org.husonlab.diamer2.io;

import org.husonlab.diamer2.seq.Sequence;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

public abstract class SequenceReader implements AutoCloseable {

    protected String header;
    protected StringBuilder sequence;
    protected BufferedReader br;
    protected String line;

    public SequenceReader(BufferedReader br) throws IOException {
        this.header = null;
        this.sequence = new StringBuilder();
        this.br = br;
        line = br.readLine();
    }

    public abstract Sequence next() throws IOException;

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

    @Override
    public void close() throws IOException {
        br.close();
    }
}
