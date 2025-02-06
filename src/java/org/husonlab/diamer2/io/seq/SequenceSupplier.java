package org.husonlab.diamer2.io.seq;

import org.husonlab.diamer2.seq.Sequence;
import org.husonlab.diamer2.seq.SequenceRecord;
import org.husonlab.diamer2.seq.alphabet.converter.Converter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedList;

import static java.util.Collections.emptyIterator;

public class SequenceSupplier<H, S> implements AutoCloseable {
    private final LinkedList<MemoryEntry<H, S>> sequenceRecordContainers;
    private final SequenceReader<H> sequenceReader;
    private final Converter<Character, S> converter;
    private final boolean keepInMemory;
    private boolean finishedReading;
    private Iterator<MemoryEntry<H, S>> iterator;
    private long bytesRead;
    private int sequencesRead;

    public SequenceSupplier(@NotNull SequenceReader<H> sequenceReader, @Nullable Converter<Character, S> converter, boolean keepInMemory) {
        this.sequenceReader = sequenceReader;
        this.converter = converter;
        this.keepInMemory = keepInMemory;
        this.sequenceRecordContainers = new LinkedList<>();
        this.finishedReading = false;
        this.bytesRead = 0;
        this.sequencesRead = 0;
    }

    public SequenceRecordContainer<H, S> next() throws IOException {
        if (keepInMemory) {
            if (iterator != null) {
                if (iterator.hasNext()) {
                    MemoryEntry<H, S> entry = iterator.next();
                    sequencesRead = entry.sequencesRead;
                    bytesRead = entry.bytesRead;
                    return entry.sequenceRecordContainer;
                } else {
                    return null;
                }
            } else {
                if (finishedReading) {
                    return null;
                } else {
                    SequenceRecord<H, Character> sequenceRecord;
                    if ((sequenceRecord = sequenceReader.next()) != null) {
                        bytesRead = sequenceReader.getBytesRead();
                        sequencesRead++;
                        MemoryEntry<H, S> entry = new MemoryEntry<>(sequencesRead, bytesRead, null);
                        sequenceRecordContainers.add(entry);
                        return getToBeTranslatedContainer(converter, sequenceRecord, entry);
                    } else {
                        finishedReading = true;
                        return next();
                    }
                }
            }
        } else {
            SequenceRecord<H, Character> sequenceRecord;
            if ((sequenceRecord = sequenceReader.next()) == null) {
                return null;
            }
            return getToBeTranslatedContainer(converter, sequenceRecord, null);
        }
    }

    public SequenceRecordContainer<H, S> getToBeTranslatedContainer(
            Converter<Character, S> converter, SequenceRecord<H, Character> sequenceRecord, MemoryEntry<H, S> entry) {
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
                if (entry != null) {
                    entry.sequenceRecordContainer = new SequenceRecordContainer<H, S>() {
                        @Override
                        public LinkedList<SequenceRecord<H, S>> getSequenceRecords() {
                            return sequenceRecords;
                        }
                    };
                }
                return sequenceRecords;
            }
        };
    }

    public void reset() throws IOException {
        sequencesRead = 0;
        bytesRead = 0;
        if (keepInMemory && finishedReading) {
            iterator = sequenceRecordContainers.iterator();
        } else {
            sequenceRecordContainers.clear();
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

    private static class MemoryEntry<H, S>{
        public final int sequencesRead;
        public final long bytesRead;
        public SequenceRecordContainer<H, S> sequenceRecordContainer;

        public MemoryEntry(int sequencesRead, long bytesRead, SequenceRecordContainer<H, S> sequenceRecordContainer) {
            this.sequencesRead = sequencesRead;
            this.bytesRead = bytesRead;
            this.sequenceRecordContainer = sequenceRecordContainer;
        }
    }
}
