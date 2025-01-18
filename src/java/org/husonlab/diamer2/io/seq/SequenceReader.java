package org.husonlab.diamer2.io.seq;

import org.husonlab.diamer2.io.CountingInputStream;
import org.husonlab.diamer2.seq.SequenceRecord;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

public abstract class SequenceReader implements AutoCloseable {

    protected final File file;
    protected long fileSize;
    protected String header;
    protected StringBuilder sequence;
    private CountingInputStream cis;
    protected BufferedReader br;
    protected String line;

    public SequenceReader(File file) {
        this.file = file;
        this.header = null;
        this.sequence = new StringBuilder();
        open();
    }

    public abstract SequenceRecord<Character> next() throws IOException;

    public ArrayList<SequenceRecord<Character>> next(int n) throws IOException {
        ArrayList<SequenceRecord<Character>> sequenceRecords = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            SequenceRecord<Character> seq = next();
            if (seq == null) {
                break;
            }
            sequenceRecords.add(seq);
        }
        return sequenceRecords;
    }

    public void open() {
        try {
            this.cis = new CountingInputStream(new FileInputStream(file));
            if (file.getName().endsWith(".gz")) {
                this.br = new BufferedReader(new InputStreamReader(new GZIPInputStream(cis)));
            } else {
                this.br = new BufferedReader(new InputStreamReader(cis));
            }
            this.fileSize = Files.size(Paths.get(file.getAbsolutePath()));
        } catch (IOException e) {
            throw new RuntimeException("Could not find sequence file: " + file.getAbsolutePath());
        }
        try {
            this.line = br.readLine();
        } catch (IOException e) {
            throw new RuntimeException("Could not read from sequence file: " + file.getAbsolutePath());
        }
    }

    public long getFileSize() {
        return fileSize;
    }

    public long getBytesRead() {
        return cis.getBytesRead();
    }

    public abstract int approximateNumberOfSequences();

    public File getFile() {
        return file;
    }

    @Override
    public void close() throws IOException {
        br.close();
    }

    public void reset() throws IOException {
        open();
    }
}
