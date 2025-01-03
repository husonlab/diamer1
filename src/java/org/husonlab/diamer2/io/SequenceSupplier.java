package org.husonlab.diamer2.io;

import org.husonlab.diamer2.seq.Sequence;
import org.husonlab.diamer2.util.Pair;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;

public class SequenceSupplier implements AutoCloseable {
    private final LinkedList<Pair<Long, Sequence>> sequences;
    private final SequenceReader sequenceReader;
    private final boolean keepInMemory;
    private boolean finishedReading;
    private Iterator<Pair<Long, Sequence>> sequenceIterator;
    private long bytesRead;

    public SequenceSupplier(SequenceReader sequenceReader, boolean keepInMemory) {
        this.sequenceReader = sequenceReader;
        this.keepInMemory = keepInMemory;
        this.sequences = new LinkedList<>();
        this.finishedReading = false;
        this.bytesRead = 0;
    }

    public Sequence next() throws IOException {
        if (keepInMemory) {
            if (!Objects.isNull(sequenceIterator)) {
                if (sequenceIterator.hasNext()) {
                    bytesRead = sequenceIterator.next().getFirst();
                    return sequenceIterator.next().getLast();
                }
                bytesRead = sequenceReader.getBytesRead();
                return null;
            }
            Sequence sequence = sequenceReader.next();
            if (!Objects.isNull(sequence)) {
                sequences.add(new Pair<>(sequenceReader.getBytesRead(), sequence));
            } else {
                finishedReading = true;
            }
            bytesRead = sequenceReader.getBytesRead();
            return sequence;
        }
        Sequence sequence = sequenceReader.next();
        bytesRead = sequenceReader.getBytesRead();
        return sequence;
    }

    public ArrayList<Sequence> next(int n) throws IOException {
        ArrayList<Sequence> sequences = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Sequence sequence = next();
            if (Objects.isNull(sequence)) {
                break;
            }
            sequences.add(sequence);
        }
        return sequences;
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
    public void close() throws IOException {
        sequenceReader.close();
    }

    public static SequenceSupplier getFastaSupplier(File file, boolean keepInMemory) {
        return new SequenceSupplier(new FASTAReader(file), keepInMemory);
    }

    public static SequenceSupplier getFastqSupplier(File file, boolean keepInMemory) {
        return new SequenceSupplier(new FASTQReader(file), keepInMemory);
    }

    public long getFileSize() {
        return sequenceReader.getFileSize();
    }

    public long getBytesRead() {
        return bytesRead;
    }
}
