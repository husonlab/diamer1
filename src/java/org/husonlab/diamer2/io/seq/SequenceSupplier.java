package org.husonlab.diamer2.io.seq;

import org.husonlab.diamer2.seq.Sequence;
import org.husonlab.diamer2.seq.SequenceRecord;
import org.husonlab.diamer2.seq.alphabet.converter.Converter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;

import static java.util.Collections.emptyIterator;

public class SequenceSupplier<H, S> implements AutoCloseable {
    private final LinkedList<MemoryEntry<H, S>> sequences;
    private final LinkedList<MemoryEntry2<H, S>> sequenceRecordContainers;
    private final SequenceReader<H> sequenceReader;
    private final Converter<Character, S> converter;
    private final boolean keepInMemory;
    private boolean finishedReading;
    private Iterator<MemoryEntry<H, S>> iterator;
    private Iterator<MemoryEntry2<H, S>> iterator2;
    private long bytesRead;
    private int sequencesRead;

    private H id;
    private Sequence<S>[] sequenceBuffer;
    private int bufferIndex;

    public SequenceSupplier(@NotNull SequenceReader<H> sequenceReader, @Nullable Converter<Character, S> converter, boolean keepInMemory) {
        this.sequenceReader = sequenceReader;
        this.converter = converter;
        this.keepInMemory = keepInMemory;
        this.sequences = new LinkedList<>();
        this.sequenceRecordContainers = new LinkedList<>();
        this.finishedReading = false;
        this.bytesRead = 0;
        this.sequencesRead = 0;
    }

    private void fillBuffer() throws IOException {
        bufferIndex = 0;
        if (keepInMemory) {
            if (iterator != null && iterator.hasNext()) {
                MemoryEntry<H, S> entry = iterator.next();
                bytesRead = entry.bytesRead();
                sequencesRead = entry.sequencesRead();
                id = entry.id();
                sequenceBuffer = entry.sequences();
                return;
            }
            SequenceRecord<H, Character> sequenceRecord = sequenceReader.next();
            if (sequenceRecord == null) {
                finishedReading = true;
                sequenceBuffer = null;
                id = null;
                return;
            }
            if (converter != null) {
                sequenceBuffer = converter.convert(sequenceRecord.sequence());
            } else {
                sequenceBuffer = new Sequence[]{sequenceRecord.sequence()};
            }
            id = sequenceRecord.id();
            bytesRead = sequenceReader.getBytesRead();
            sequencesRead++;
            sequences.add(new MemoryEntry<H, S>(sequencesRead, bytesRead, id, sequenceBuffer));
            return;
        }
        SequenceRecord<H, Character> sequenceRecord = sequenceReader.next();
        if (sequenceRecord == null) {
            sequenceBuffer = null;
            id = null;
            return;
        }
        if (converter != null) {
            sequenceBuffer = converter.convert(sequenceRecord.sequence());
        } else {
            sequenceBuffer = new Sequence[]{sequenceRecord.sequence()};
        }
        id = sequenceRecord.id();
        bytesRead = sequenceReader.getBytesRead();
        sequencesRead++;
    }

    public SequenceRecordContainer<H, S> nextRecordContainer() throws IOException {
        if (keepInMemory) {
            if (iterator2 != null) {
                if (iterator2.hasNext()) {
                    MemoryEntry2<H, S> entry = iterator2.next();
                    sequencesRead = entry.sequencesRead;
                    bytesRead = entry.bytesRead;
                    return entry.sequenceRecordContainer;
                } else {
                    return null;
                }
            } else {
                SequenceRecord<H, Character> sequenceRecord;
                if ((sequenceRecord = sequenceReader.next()) == null) {
                    finishedReading = true;
                    iterator2 = sequenceRecordContainers.iterator();
                    return nextRecordContainer();
                }
                MemoryEntry2<H, S> entry = new MemoryEntry2<>(sequencesRead, bytesRead, null);
                sequenceRecordContainers.add(entry);
                return getToBeTranslatedContainer(converter, sequenceRecord);
            }
        } else {
            SequenceRecord<H, Character> sequenceRecord;
            if ((sequenceRecord = sequenceReader.next()) == null) {
                return null;
            }
            return getToBeTranslatedContainer(converter, sequenceRecord);
        }
    }

    public SequenceRecordContainer<H, S> getToBeTranslatedContainer(
            Converter<Character, S> converter, SequenceRecord<H, Character> sequenceRecord) {
        return new SequenceRecordContainer<H, S>() {
            @Override
            public LinkedList<SequenceRecord<H, S>> getSequenceRecords() {
                LinkedList<SequenceRecord<H, S>> sequenceRecords = new LinkedList<>();
                if (converter != null) {
                    H header = sequenceRecord.id();
                    for (Sequence<S> sequence : converter.convert(sequenceRecord.sequence())) {
                        sequenceRecords.add(new SequenceRecord<>(header, sequence));
                    }
                } else {
                    sequenceRecords.add((SequenceRecord<H, S>) sequenceRecord);
                }
                return sequenceRecords;
            }
        };
    }

    public SequenceRecord<H, S> next() throws IOException {
        if (sequenceBuffer == null || bufferIndex >= sequenceBuffer.length) {
            fillBuffer();
        }
        if (sequenceBuffer == null) {
            return null;
        }
        if (sequenceBuffer.length == 0) {
            return getEmptySequenceRecord(id);
        }
        return new SequenceRecord<>(id, sequenceBuffer[bufferIndex++]);
    }

    public ArrayList<SequenceRecord<H, S>> next(int n) throws IOException {
        ArrayList<SequenceRecord<H, S>> sequenceRecords = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            SequenceRecord<H, S> sequenceRecord = next();
            if (Objects.isNull(sequenceRecord)) {
                break;
            }
            sequenceRecords.add(sequenceRecord);
        }
        return sequenceRecords;
    }

    public void reset() throws IOException {
        sequenceBuffer = null;
        id = null;
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

    public SequenceSupplier<H, S> open() {
        sequenceReader.open();
        return this;
    }

    private SequenceRecord<H, S> getEmptySequenceRecord(H id) {
        return new SequenceRecord<H, S>(id, new Sequence<S>(converter.getTargetAlphabet()) {
            @NotNull
            @Override
            public Iterator<S> iterator() {
                return emptyIterator();
            }

            @Override
            public S get(int index) {
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

    public Path getFile() {
        return sequenceReader.getFile();
    }

    public int approximateNumberOfSequences() {
        return sequenceReader.approximateNumberOfSequences();
    }

    private record MemoryEntry<H, S>(int sequencesRead, long bytesRead, H id, Sequence<S>[] sequences) {}

    private static class MemoryEntry2<H, S>{
        public final int sequencesRead;
        public final long bytesRead;
        public SequenceRecordContainer<H, S> sequenceRecordContainer;

        public MemoryEntry2(int sequencesRead, long bytesRead, SequenceRecordContainer<H, S> sequenceRecordContainer) {
            this.sequencesRead = sequencesRead;
            this.bytesRead = bytesRead;
            this.sequenceRecordContainer = sequenceRecordContainer;
        }
    }
}
