package org.husonlab.diamer2.io.seq;

import org.husonlab.diamer2.io.Utilities;
import org.husonlab.diamer2.seq.SequenceRecord;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;

public class FastqIdReader extends SequenceReader<Integer> {

    private LinkedList<String> headers;

    public FastqIdReader(Path file) {
        super(file);
        headers = new LinkedList<>();
    }

    @Override
    public SequenceRecord<Integer, Character> next() throws IOException {
        if (line != null && line.startsWith("@")) {
            headers.add(line);
            id = headers.size() - 1;
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

    public LinkedList<String> getHeaders() {
        return headers;
    }

    @Override
    public int approximateNumberOfSequences() {
        return Utilities.approximateNumberOfSequences(file, "\n@");
    }
}
