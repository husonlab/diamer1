package org.husonlab.diamer2.io.seq;

import org.husonlab.diamer2.io.Utilities;
import org.husonlab.diamer2.seq.SequenceRecord;

import java.io.*;
import java.nio.file.Path;

public class FastaReader extends SequenceReader<String> {

    public FastaReader(Path file) {
        super(file);
    }

    @Override
    public SequenceRecord<String, Character> next() throws IOException {
        if (line != null && line.startsWith(">")) {
            id = line;
            sequence = new StringBuilder();
            while ((line = br.readLine()) != null) {
                line = line.strip();
                if (line.startsWith(">")) {
                    return SequenceRecord.DNA(id, sequence.toString());
                } else {
                    sequence.append(line);
                }
            }
            return SequenceRecord.DNA(id, sequence.toString());
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
