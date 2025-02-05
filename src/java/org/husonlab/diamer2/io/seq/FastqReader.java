package org.husonlab.diamer2.io.seq;

import org.husonlab.diamer2.io.Utilities;
import org.husonlab.diamer2.seq.SequenceRecord;

import java.io.*;
import java.nio.file.Path;

/**
 * Class to read {@link SequenceRecord}s containing the header and the sequence (not the quality) from a FASTQ file.
 */
public class FastqReader extends SequenceReader<String> {

    /**
     * @param file Path to the FASTQ file to read from
     */
    public FastqReader(Path file) {
        super(file);
    }

    @Override
    public SequenceRecord<String, Character> next() throws IOException {
        sequencesRead++;
        if (line != null && line.startsWith("@")) {
            id = line;
            sequence = new StringBuilder(br.readLine());
            br.readLine();
            br.readLine();
            line = br.readLine();
            return SequenceRecord.DNA(id, sequence.toString());
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
