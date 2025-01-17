package org.husonlab.diamer2.io.seq;

import org.husonlab.diamer2.seq.SequenceRecord;
import org.husonlab.diamer2.util.Pair;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;

public class SequenceSupplierOld implements AutoCloseable {
    private final LinkedList<Pair<Long, SequenceRecord>> sequences;
    private final SequenceReader sequenceReader;
    private final boolean keepInMemory;
    private boolean finishedReading;
    private Iterator<Pair<Long, SequenceRecord>> sequenceIterator;
    private long bytesRead;

    public SequenceSupplierOld(SequenceReader sequenceReader, boolean keepInMemory) {
        this.sequenceReader = sequenceReader;
        this.keepInMemory = keepInMemory;
        this.sequences = new LinkedList<>();
        this.finishedReading = false;
        this.bytesRead = 0;
    }

    public SequenceRecord next() throws IOException {
        if (keepInMemory) {
            if (!Objects.isNull(sequenceIterator)) {
                if (sequenceIterator.hasNext()) {
                    Pair<Long, SequenceRecord> next = sequenceIterator.next();
                    bytesRead = next.first();
                    return next.last();
                }
                bytesRead = sequenceReader.getBytesRead();
                return null;
            }
            SequenceRecord sequenceRecord = sequenceReader.next();
            if (!Objects.isNull(sequenceRecord)) {
                sequences.add(new Pair<>(sequenceReader.getBytesRead(), sequenceRecord));
            } else {
                finishedReading = true;
            }
            bytesRead = sequenceReader.getBytesRead();
            return sequenceRecord;
        }
        SequenceRecord sequenceRecord = sequenceReader.next();
        bytesRead = sequenceReader.getBytesRead();
        return sequenceRecord;
    }

    public ArrayList<SequenceRecord> next(int n) throws IOException {
        ArrayList<SequenceRecord> sequenceRecords = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            SequenceRecord sequenceRecord = next();
            if (Objects.isNull(sequenceRecord)) {
                break;
            }
            sequenceRecords.add(sequenceRecord);
        }
        return sequenceRecords;
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

    public SequenceSupplierOld open() {
        sequenceReader.open();
        return this;
    }

    public static SequenceSupplierOld getFastaSupplier(File file, boolean keepInMemory) {
        return new SequenceSupplierOld(new FASTAReader(file), keepInMemory);
    }

    public static SequenceSupplierOld getFastqSupplier(File file, boolean keepInMemory) {
        return new SequenceSupplierOld(new FASTQReader(file), keepInMemory);
    }

    public long getFileSize() {
        return sequenceReader.getFileSize();
    }

    public long getBytesRead() {
        return bytesRead;
    }

    public File getFile() {
        return sequenceReader.getFile();
    }
}
