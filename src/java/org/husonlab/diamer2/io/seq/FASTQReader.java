package org.husonlab.diamer2.io.seq;

import org.husonlab.diamer2.seq.SequenceRecord;

import java.io.*;

public class FASTQReader extends SequenceReader {

    public FASTQReader(File file) {
        super(file);
    }

    @Override
    public SequenceRecord next() throws IOException {
        if (line != null && line.startsWith("@")) {
            header = line;
            sequence = new StringBuilder(br.readLine());
            br.readLine();
            br.readLine();
            line = br.readLine();
            return SequenceRecord.DNA(header, sequence.toString());
        } else {
            while ((line = br.readLine()) != null) {
                if (line.startsWith("@")) {
                    return next();
                }
            }
            return null;
        }
    }
}
