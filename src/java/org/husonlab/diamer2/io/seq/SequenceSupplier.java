package org.husonlab.diamer2.io.seq;

import org.husonlab.diamer2.seq.Sequence;
import org.husonlab.diamer2.seq.SequenceRecord;
import org.husonlab.diamer2.seq.alphabet.converter.Converter;
import org.husonlab.diamer2.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;

public class SequenceSupplier<T> implements AutoCloseable {
    private final LinkedList<MemoryEntry<T>> sequences;
    private final SequenceReader sequenceReader;
    private final Converter<Character, T> converter;
    private final boolean keepInMemory;
    private boolean finishedReading;
    private Iterator<MemoryEntry<T>> iterator;
    private long bytesRead;

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
    }

    private void fillBuffer() throws IOException {
        if (keepInMemory) {
            if (iterator != null && iterator.hasNext()) {
                MemoryEntry<T> entry = iterator.next();
                bytesRead = entry.bytesRead();
                header = entry.header();
                sequenceBuffer = entry.sequences();
                bufferIndex = 0;
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
            bufferIndex = 0;
            bytesRead = sequenceReader.getBytesRead();
            sequences.add(new MemoryEntry<>(bytesRead, header, sequenceBuffer));
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
        bufferIndex = 0;
    }

    public SequenceRecord<T> next() throws IOException {
        if (sequenceBuffer == null || bufferIndex >= sequenceBuffer.length) {
            fillBuffer();
        }
        if (sequenceBuffer == null) {
            return null;
        }
        return new SequenceRecord<T>(header, sequenceBuffer[bufferIndex++]);
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

    public long getFileSize() {
        return sequenceReader.getFileSize();
    }

    public long getBytesRead() {
        return bytesRead;
    }

    public File getFile() {
        return sequenceReader.getFile();
    }

    private record MemoryEntry<T>(long bytesRead, String header, Sequence<T>[] sequences) {}
}
