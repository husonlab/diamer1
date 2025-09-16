package org.husonlab.diamer.io.seq;

import org.husonlab.diamer.io.Utilities;
import org.husonlab.diamer.seq.SequenceRecord;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Class to read {@link SequenceRecord}s with sequences and integer ids from a FASTA file.
 * <p>
 *     The headers of the FASTA file must only contain an integer id or an exception will be thrown.
 * </p>
 */
public class FastaIdReader extends SequenceReader<Integer, char[]> {

    protected String line;
    protected int id;
    protected final StringBuilder sequence;

    public FastaIdReader(Path file) {
        super(file);
        try {
            line = br.readLine();
            if (line == null) {
                throw new IOException("Empty file: " + file);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not read input file.", e);
        }
        sequence = new StringBuilder();
    }

    @Override
    public SequenceRecord<Integer, char[]> next() throws IOException {
        if (readNextId()) {
            readNextSequence();
            sequencesRead++;
            char[] sequenceArray = new char[sequence.length()];
            sequence.getChars(0, sequence.length(), sequenceArray, 0);
            return new SequenceRecord<>(id, sequenceArray);
        } else {
            return null;
        }
    }

    public boolean readNextId() throws IOException {
        do {
            if (line == null) {
                return false; // End of file
            } else if (line.startsWith(">")) {
                try {
                    id = Integer.parseInt(line.substring(1));
                    line = br.readLine();
                    return true;
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Invalid sequence ID format");
                }
            }
        } while ((line = br.readLine()) != null);
        return false;
    }

    public void readNextSequence() throws IOException {
        sequence.setLength(0);
        sequence.append('*');
        do {
            if (line.startsWith(">")) {
                sequence.append("*");
                return; // End of sequence
            } else {
                sequence.append(line);
            }
        } while ((line = br.readLine()) != null);
        sequence.append("*"); // End of last sequence
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        line = br.readLine();
    }

    @Override
    public int approximateNumberOfSequences() {
        return Utilities.approximateNumberOfSequences(file, "\n>");
    }
}
