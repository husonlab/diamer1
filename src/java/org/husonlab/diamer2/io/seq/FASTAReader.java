package org.husonlab.diamer2.io.seq;

import org.husonlab.diamer2.seq.SequenceRecord;

import java.io.*;

public class FASTAReader extends SequenceReader {

    public FASTAReader(File file) {
        super(file);
    }

    @Override
    public SequenceRecord next() throws IOException {
        if (line != null && line.startsWith(">")) {
            header = line;
            sequence = new StringBuilder();
            while ((line = br.readLine()) != null) {
                line = line.strip();
                if (line.startsWith(">")) {
                    return SequenceRecord.DNA(header, sequence.toString());
                } else {
                    sequence.append(line);
                }
            }
            return SequenceRecord.DNA(header, sequence.toString());
        } else {
            while ((line = br.readLine()) != null) {
                if (line.startsWith(">")) {
                    return next();
                }
            }
            return null;
        }
    }
}
