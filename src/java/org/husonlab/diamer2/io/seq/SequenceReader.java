package org.husonlab.diamer2.io.seq;

import org.husonlab.diamer2.io.CountingInputStream;
import org.husonlab.diamer2.seq.SequenceRecord;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

/**
 * Class for reading string sequences from a file (gzipped or not) with multiple sequences.
 * The sequences are supposed to have a header in the form of a string or an integer (H).
 * <p>
 *     The class is supposed to be implemented in sequence readers that are specific to the format of the file.
 * </p>
 * @param <H> Type of the header
 */
public abstract class SequenceReader<H, S> implements AutoCloseable {

    protected final Path file;
    protected long fileSize;
    protected H id;
    protected StringBuilder sequence;
    private CountingInputStream cis;
    protected BufferedReader br;
    protected int sequencesRead;

    /**
     * @param file Path to the file (gzipped or not) to read from
     */
    public SequenceReader(Path file) {
        this.file = file;
        this.id = null;
        this.sequence = new StringBuilder();
        sequencesRead = 0;
        open();
    }

    /**
     * Reads over the file until the next sequence is found and returns it.
     * @return SequenceRecord with the header and the sequence
     */
    public abstract SequenceRecord<H, S> next() throws IOException;

    /**
     * Reads the next n sequences from the file and returns them.
     * @param n Number of sequences to read
     * @return List of {@link SequenceRecord}s
     */
    public ArrayList<SequenceRecord<H, S>> next(int n) throws IOException {
        ArrayList<SequenceRecord<H, S>> sequenceRecords = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            SequenceRecord<H, S> seq = next();
            if (seq == null) {
                break;
            }
            sequenceRecords.add(seq);
        }
        return sequenceRecords;
    }

    /**
     * (Re)opens the sequence file for reading.
     */
    public void open() {
        try {
            this.cis = new CountingInputStream(new FileInputStream(file.toString()));
            if (file.toFile().getName().endsWith(".gz")) {
                this.br = new BufferedReader(new InputStreamReader(new GZIPInputStream(cis, 131072)), 131072);
            } else {
                this.br = new BufferedReader(new InputStreamReader(cis), 131072);
            }
            this.fileSize = Files.size(Paths.get(file.toString()));
        } catch (IOException e) {
            throw new RuntimeException("Could not find sequence file: " + file);
        }
    }

    /**
     * @return The size of the input file in bytes
     */
    public long getFileSize() {
        return fileSize;
    }

    /**
     * @return The number of bytes read from the file since it was opened or reset.
     */
    public long getBytesRead() {
        return cis.getBytesRead();
    }

    /**
     * @return The number of sequences read from the file since it was opened or reset.
     */
    public int getSequencesRead() {
        return sequencesRead;
    }

    /**
     * @return An approximation of the number of sequences in the file
     */
    public abstract int approximateNumberOfSequences();

    /**
     * @return The path to the file that is being read
     */
    public Path getFile() {
        return file;
    }

    @Override
    public void close() throws IOException {
        br.close();
    }

    /**
     * Resets the reader to the beginning of the file.
     */
    public void reset() throws IOException {
        sequencesRead = 0;
        open();
    }
}
