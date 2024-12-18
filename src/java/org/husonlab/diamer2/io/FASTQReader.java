package org.husonlab.diamer2.io;

import org.husonlab.diamer2.seq.Sequence;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

public class FASTQReader extends SequenceReader {

    public FASTQReader(BufferedReader br) throws IOException {
        super(br);
    }

    @Override
    public Sequence next() throws IOException {
        if (line != null && line.startsWith("@")) {
            header = line;
            sequence = new StringBuilder(br.readLine());
            br.readLine();
            br.readLine();
            line = br.readLine();
            return new Sequence(header, sequence.toString());
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
