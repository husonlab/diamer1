package org.husonlab.diamer2.io;

import org.husonlab.diamer2.seq.Sequence;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;

public class SequenceSupplier implements AutoCloseable {
    private LinkedList<Sequence> sequences;
    private final SequenceReader sequenceReader;
    private final boolean keepInMemory;
    private boolean finishedReading;
    private Iterator<Sequence> sequenceIterator;

    public SequenceSupplier(SequenceReader sequenceReader, boolean keepInMemory) {
        this.sequenceReader = sequenceReader;
        this.keepInMemory = keepInMemory;
        this.sequences = new LinkedList<>();
        this.finishedReading = false;
    }

    public Sequence next() throws IOException {
        if (keepInMemory) {
            if (!Objects.isNull(sequenceIterator)) {
                return sequenceIterator.hasNext() ? sequenceIterator.next() : null;
            }
            Sequence sequence = sequenceReader.next();
            if (!Objects.isNull(sequence)) {
                sequences.add(sequence);
            } else {
                finishedReading = true;
            }
            return sequence;
        }
        return sequenceReader.next();
    }

    public void reset() throws IOException {
        if (keepInMemory && finishedReading) {
            sequenceIterator = sequences.iterator();
        } else {
            finishedReading = false;
            sequenceReader.br.reset();
        }
    }

    @Override
    public void close() throws Exception {
        sequenceReader.close();
    }
}
