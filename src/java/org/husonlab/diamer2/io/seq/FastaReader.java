package org.husonlab.diamer2.io.seq;

import org.husonlab.diamer2.io.Utilities;
import org.husonlab.diamer2.seq.SequenceRecord;

import java.io.*;
import java.nio.file.Path;

/**
 * Class for reading {@link SequenceRecord}s with string headers and sequences from a file in FASTA format.
 */
public class FastaReader extends SequenceReader<String, char[]> {

    private String line;

    /**
     * @param file Path to the file (gzipped or not) to read from
     */
    public FastaReader(Path file) {
        super(file);
        try {
            this.line = br.readLine();
        } catch (IOException e) {
            throw new RuntimeException("Could not read from sequence file: " + file);
        }
    }

    @Override
    public SequenceRecord<String, char[]> next() throws IOException {
        sequencesRead++;
        if (line != null && line.startsWith(">")) {
            id = line;
            sequence = new StringBuilder();
            while ((line = br.readLine()) != null) {
                line = line.strip();
                if (line.startsWith(">")) {
                    char[] sequenceCharArray = new char[sequence.length()];
                    sequence.getChars(0, sequence.length(), sequenceCharArray, 0);
                    return new SequenceRecord<>(id, sequenceCharArray);
                } else {
                    sequence.append(line);
                }
            }
            char[] sequenceCharArray = new char[sequence.length()];
            sequence.getChars(0, sequence.length(), sequenceCharArray, 0);
            return new SequenceRecord<>(id, sequenceCharArray);
        } else {
            while ((line = br.readLine()) != null) {
                if (line.startsWith(">")) {
                    return next();
                }
            }
            return null;
        }
    }

    @Override
    public int approximateNumberOfSequences() {
        return Utilities.approximateNumberOfSequences(file, "\n>");
    }
}
