package org.husonlab.diamer.io.seq;

import org.husonlab.diamer.io.Utilities;
import org.husonlab.diamer.seq.SequenceRecord;

import java.io.*;
import java.nio.file.Path;

/**
 * Class to read the header and the sequence (not the quality) as {@link SequenceRecord}s from a FASTQ file.
 */
public class FastqReader extends SequenceReader<String, String> {
    private String line;
    /**
     * @param file Path to the FASTQ file to read from
     */
    public FastqReader(Path file) {
        super(file);
        try {
            this.line = br.readLine();
        } catch (IOException e) {
            throw new RuntimeException("Could not read from sequence file: " + file);
        }
    }

    @Override
    public SequenceRecord<String, String> next() throws IOException {
        sequencesRead++;
        if (line != null && line.startsWith("@")) {
            id = line;
            sequence = new StringBuilder(br.readLine());
            br.readLine();
            br.readLine();
            line = br.readLine();
            return new SequenceRecord<>(id, sequence.toString());
        } else {
            while ((line = br.readLine()) != null) {
                if (line.startsWith("@")) {
                    return next();
                }
            }
            return null;
        }
    }

    @Override
    public int approximateNumberOfSequences() {
        return Utilities.approximateNumberOfSequences(file, "\n@");
    }
}
