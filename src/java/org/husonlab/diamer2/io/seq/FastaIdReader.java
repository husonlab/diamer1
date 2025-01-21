package org.husonlab.diamer2.io.seq;

import org.husonlab.diamer2.io.Utilities;
import org.husonlab.diamer2.seq.SequenceRecord;

import java.io.File;
import java.io.IOException;

public class FastaIdReader extends SequenceReader<Integer> {

    public FastaIdReader(File file) {
        super(file);
    }

    @Override
    public SequenceRecord<Integer, Character> next() throws IOException {
        if (line != null && line.startsWith(">")) {
            try {
                id = Integer.parseInt(line.substring(1));
            } catch (NumberFormatException e) {
                throw new IOException("Could not parse sequence id from line: " + line);
            }
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
