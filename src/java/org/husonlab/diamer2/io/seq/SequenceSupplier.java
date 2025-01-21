package org.husonlab.diamer2.io.seq;

import org.husonlab.diamer2.seq.Sequence;
import org.husonlab.diamer2.seq.SequenceRecord;
import org.husonlab.diamer2.seq.alphabet.converter.Converter;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;

import static java.util.Collections.emptyIterator;

public class SequenceSupplier<T> implements AutoCloseable {
    private final LinkedList<MemoryEntry<T>> sequences;
    private final SequenceReader sequenceReader;
    private final Converter<Character, T> converter;
    private final boolean keepInMemory;
    private boolean finishedReading;
    private Iterator<MemoryEntry<T>> iterator;
    private long bytesRead;
    private int sequencesRead;

    private String header;
    private Sequence<T>[] sequenceBuffer;
    private int bufferIndex;

    public SequenceSupplier(@NotNull SequenceReader sequenceReader, @NotNull Converter<Character, T> converter, boolean keepInMemory) {
        this.sequenceReader = sequenceReader;
        this.converter = converter;
        this.keepInMemory = keepInMemory;
        this.sequences = new LinkedList<>();
        this.finishedReading = false;
        this.bytesRead = 0;
        this.sequencesRead = 0;
    }

    private void fillBuffer() throws IOException {
        bufferIndex = 0;
        if (keepInMemory) {
            if (iterator != null && iterator.hasNext()) {
                MemoryEntry<T> entry = iterator.next();
                bytesRead = entry.bytesRead();
                sequencesRead = entry.sequencesRead();
                header = entry.header();
                sequenceBuffer = entry.sequences();
                return;
            }
            SequenceRecord<Character> sequenceRecord = sequenceReader.next();
            if (sequenceRecord == null) {
                finishedReading = true;
                sequenceBuffer = null;
                header = null;
                return;
            }
            sequenceBuffer = converter.convert(sequenceRecord.getSequence());
            header = sequenceRecord.getHeader();
            bytesRead = sequenceReader.getBytesRead();
            sequencesRead++;
            sequences.add(new MemoryEntry<>(sequencesRead, bytesRead, header, sequenceBuffer));
            return;
        }
        SequenceRecord<Character> sequenceRecord = sequenceReader.next();
        if (sequenceRecord == null) {
            sequenceBuffer = null;
            header = null;
            return;
        }
        sequenceBuffer = converter.convert(sequenceRecord.getSequence());
        header = sequenceRecord.getHeader();
        bytesRead = sequenceReader.getBytesRead();
        sequencesRead++;
    }

    public SequenceRecord<T> next() throws IOException {
        if (sequenceBuffer == null || bufferIndex >= sequenceBuffer.length) {
            fillBuffer();
        }
        if (sequenceBuffer == null) {
            return null;
        }
        if (sequenceBuffer.length == 0) {
            return getEmptySequenceRecord(header);
        }
        SequenceRecord<T> sequenceRecord = new SequenceRecord<>(header, sequenceBuffer[bufferIndex++]);
        sequenceRecord.setId(sequencesRead - 1);
        return sequenceRecord;
    }

    public ArrayList<SequenceRecord<T>> next(int n) throws IOException {
        ArrayList<SequenceRecord<T>> sequenceRecords = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            SequenceRecord<T> sequenceRecord = next();
            if (Objects.isNull(sequenceRecord)) {
                break;
            }
            sequenceRecords.add(sequenceRecord);
        }
        return sequenceRecords;
    }

    public void reset() throws IOException {
        sequenceBuffer = null;
        header = null;
        bufferIndex = 0;
        sequencesRead = 0;
        if (keepInMemory && finishedReading) {
            iterator = sequences.iterator();
        } else {
            finishedReading = false;
            sequenceReader.reset();
        }
    }

    @Override
    public void close() throws IOException {
        sequenceReader.close();
    }

    public SequenceSupplier<T> open() {
        sequenceReader.open();
        return this;
    }

    private SequenceRecord<T> getEmptySequenceRecord(String header) {
        return new SequenceRecord<T>(header, new Sequence<T>(converter.getTargetAlphabet()) {
            @NotNull
            @Override
            public Iterator<T> iterator() {
                return emptyIterator();
            }

            @Override
            public T get(int index) {
                return null;
            }

            @Override
            public int length() {
                return 0;
            }
        });
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

    private record MemoryEntry<T>(int sequencesRead, long bytesRead, String header, Sequence<T>[] sequences) {}
}
