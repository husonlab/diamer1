package org.husonlab.diamer2.io.seq;

import org.husonlab.diamer2.io.Utilities;
import org.husonlab.diamer2.seq.CharSequence;
import org.husonlab.diamer2.seq.SequenceRecord;
import org.husonlab.diamer2.seq.alphabet.DNA;

import java.io.*;
import java.nio.file.Path;

/**
 * Class to read the header and the sequence (not the quality) as {@link SequenceRecord}s from a FASTQ file.
 */
public class FastqReader extends SequenceReader<String, Character, DNA> {

    /**
     * @param file Path to the FASTQ file to read from
     */
    public FastqReader(Path file) {
        super(file, new DNA());
    }

    @Override
    public SequenceRecord<String, Character, DNA> next() throws IOException {
        sequencesRead++;
        if (line != null && line.startsWith("@")) {
            id = line;
            sequence = new StringBuilder(br.readLine());
            br.readLine();
            br.readLine();
            line = br.readLine();
            return new SequenceRecord<>(id, new CharSequence<>(alphabet, sequence.toString().toCharArray()));
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
