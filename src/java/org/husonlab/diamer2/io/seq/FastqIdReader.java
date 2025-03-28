package org.husonlab.diamer2.io.seq;

import org.husonlab.diamer2.io.Utilities;
import org.husonlab.diamer2.seq.SequenceRecord;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;

/**
 * Class to read {@link SequenceRecord}s containing the sequence and an integer ID from a FASTQ file.
 * <p>
 *     The ID is the index of the sequence in the file, starting from 0.
 * </p>
 * <p>
 *     A List of the original headers can be obtained with {@link #getHeaders()}.
 * </p>
 */
public class FastqIdReader extends SequenceReader<Integer, char[]> implements HeaderToIdReader {
    private String line;

    /**
     * List to store the headers of the sequences during reading.
     */
    private final LinkedList<String> headers;

    /**
     * Controls if headers should be collected. Will be set to false after calling {@link #removeHeaders()}.
     */
    private boolean collectHeaders;

    /**
     * @param file Path to the file (gzipped or not) to read from
     */
    public FastqIdReader(Path file) {
        super(file);
        try {
            this.line = br.readLine();
        } catch (IOException e) {
            throw new RuntimeException("Could not read from sequence file: " + file);
        }
        headers = new LinkedList<>();
        collectHeaders = true;
    }

    // todo: change to use read(char[]) instead of readLine()
    @Override
    public SequenceRecord<Integer, char[]> next() throws IOException {
        if (line != null && line.startsWith("@")) {
            id = sequencesRead++;
            headers.add(line);
            sequence = new StringBuilder(br.readLine());
            br.readLine();
            br.readLine();
            line = br.readLine();
            char[] sequence = new char[this.sequence.length()];
            this.sequence.getChars(0, this.sequence.length(), sequence, 0);
            return new SequenceRecord<>(id, sequence);
        } else {
            while ((line = br.readLine()) != null) {
                if (line.startsWith("@")) {
                    return next();
                }
            }
            return null;
        }
    }

    /**
     * @return A list with all headers of the sequences that have been read so far
     */
    @Override
    public LinkedList<String> getHeaders() {
        return headers;
    }

    @Override
    public void removeHeaders() {
        headers.clear();
        collectHeaders = false;
    }

    @Override
    public int approximateNumberOfSequences() {
        return Utilities.approximateNumberOfSequences(file, "\n@");
    }
}
