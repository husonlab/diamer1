package org.husonlab.diamer2.io.seq;

import org.husonlab.diamer2.io.Utilities;
import org.husonlab.diamer2.seq.SequenceRecord;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Class to read {@link SequenceRecord}s with sequences and integer ids from a FASTA file.
 * <p>
 *     The headers of the FASTA file must only contain an integer id or an exception will be thrown.
 * </p>
 */
public class FastaIdReader extends SequenceReader<Integer, char[]> {

    private final static int BUFFER_SIZE = 131072;
    private final char[] buffer;
    private int bufferIndex;
    private final StringBuilder idString;
    private int id;
    private final StringBuilder sequence;

    public FastaIdReader(Path file) {
        super(file);
        this.buffer = new char[BUFFER_SIZE];
        this.bufferIndex = BUFFER_SIZE;
        this.idString = new StringBuilder();
        this.sequence = new StringBuilder();
    }

    @Override
    public SequenceRecord<Integer, char[]> next() throws IOException {
        if (readNextId() && readNextSequence()) {
            char[] sequenceArray = new char[sequence.length()];
            sequence.getChars(0, sequence.length(), sequenceArray, 0);
            return new SequenceRecord<>(id, sequenceArray);
        } else {
            return null;
        }
    }

    public boolean readNextId() throws IOException {
        idString.setLength(0);
        while (true) {
            if (bufferIndex >= BUFFER_SIZE) {
                if (fillBuffer() == -1) {
                    return false; // End of file
                }
            }

            while (bufferIndex < BUFFER_SIZE) {
                char c = buffer[bufferIndex++];
                if (c == '>') {
                    idString.setLength(0); // Start new ID
                } else if (c == '\n') {
                    try {
                        id = Integer.parseInt(idString.toString());
                        return true;
                    } catch (NumberFormatException e) {
                        throw new IOException("Invalid sequence ID format");
                    }
                } else if (Character.isDigit(c)) {
                    idString.append(c);
                }
            }
        }
    }

    public boolean readNextSequence() throws IOException {
        sequence.setLength(0);
        while (true) {
            if (bufferIndex >= BUFFER_SIZE) {
                if (fillBuffer() == -1) {
                    return !sequence.isEmpty(); // EOF, return true if sequence was read
                }
            }

            while (bufferIndex < BUFFER_SIZE) {
                char c = buffer[bufferIndex++];
                if (c == '>') {
                    bufferIndex--; // Step back so the next ID can be processed
                    return true;
                } else if (!Character.isISOControl(c)) {
                    sequence.append(c);
                }
            }
        }
    }

    public int fillBuffer() throws IOException {
        int readChars = br.read(buffer);
        if (readChars == -1) {
            return -1;
        }
        bufferIndex = 0;
        return readChars;
    }

    @Override
    public int approximateNumberOfSequences() {
        return Utilities.approximateNumberOfSequences(file, "\n>");
    }
}
