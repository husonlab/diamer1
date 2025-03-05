package org.husonlab.diamer2.io.seq;

import org.husonlab.diamer2.io.Utilities;
import org.husonlab.diamer2.seq.CharSequence;
import org.husonlab.diamer2.seq.Sequence;
import org.husonlab.diamer2.seq.SequenceRecord;
import org.husonlab.diamer2.seq.alphabet.Alphabet;

import java.io.*;
import java.nio.file.Path;

/**
 * Class for reading {@link SequenceRecord}s with string headers and sequences from a file in FASTA format.
 */
public class FastaReader<A extends Alphabet<Character>> extends SequenceReader<String, Character, A> {

    /**
     * @param file Path to the file (gzipped or not) to read from
     */
    public FastaReader(Path file, A alphabet) {
        super(file, alphabet);
    }

    @Override
    public SequenceRecord<String, Character, A> next() throws IOException {
        sequencesRead++;
        if (line != null && line.startsWith(">")) {
            id = line;
            sequence = new StringBuilder();
            while ((line = br.readLine()) != null) {
                line = line.strip();
                if (line.startsWith(">")) {
                    return new SequenceRecord<>(id, new CharSequence<>(alphabet, sequence.toString().toCharArray()));
                } else {
                    sequence.append(line);
                }
            }
            return new SequenceRecord<>(id, new CharSequence<>(alphabet, sequence.toString().toCharArray()));
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
