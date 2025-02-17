package org.husonlab.diamer2.io.seq;

import org.husonlab.diamer2.seq.Sequence;
import org.husonlab.diamer2.seq.SequenceRecord;
import org.husonlab.diamer2.seq.converter.Converter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Class for iterating over sequence files multiple times. After each iteration, the {@link #reset()} method must be
 * called to start from the beginning again.
 * <p>
 *     The most important feature is, that it can keep the sequences in memory, so that all but the first iteration
 *     don't need another IO operation. Additionally, a converter can be supplied to convert the sequences in a different
 *     alphabet, that might require less memory. The conversion to a different alphabet is thereby only performed, when
 *     the sequence is actually requested, so that the computation is performed on the threads that process the
 *     sequences.
 * </p>
 * @param <H> Type of the header of the sequence (determined by the {@link SequenceReader})
 * @param <S> Type of the sequence (determined by the {@link Converter} or the {@link SequenceReader} if no converter is
 *           supplied)
 */
public class SequenceSupplier<H, S> implements AutoCloseable {
    /**
     * List to keep the sequence records in memory.
     */
    private final LinkedList<MemoryEntry<H, S>> sequenceRecordContainers;
    private final SequenceReader<H> sequenceReader;
    private final Converter<Character, S> converter;
    private final boolean keepInMemory;
    private boolean finishedReading;
    private Iterator<MemoryEntry<H, S>> iterator;
    private long bytesRead;
    private int sequencesRead;

    /**
     * @param sequenceReader Reader to read the sequences from
     * @param converter Converter to convert the sequences to a different alphabet
     * @param keepInMemory Whether to keep the sequences in memory or not
     */
    public SequenceSupplier(@NotNull SequenceReader<H> sequenceReader, @Nullable Converter<Character, S> converter, boolean keepInMemory) {
        this.sequenceReader = sequenceReader;
        this.converter = converter;
        this.keepInMemory = keepInMemory;
        this.sequenceRecordContainers = new LinkedList<>();
        this.finishedReading = false;
        this.bytesRead = 0;
        this.sequencesRead = 0;
    }

    /**
     * Reads the next sequence record from the input file or gets it from the memory.
     * <p>
     *     The method returns a {@link FutureSequenceRecords} isntead of a single {@link SequenceRecord}, because
     *     the converters potentially convert a single sequence into multiple sequences. The conversion itself is only
     *     performed once the {@link FutureSequenceRecords#getSequenceRecords()} method is called. In case the
     *     sequences should be stored in memory this call also stores the converted sequences in the linked list of
     *     this class. Consequently, the {@link FutureSequenceRecords#getSequenceRecords()} method must be called to
     *     store the converted sequences in memory.
     * </p>
     * @return {@link FutureSequenceRecords} containing potentially multiple {@link SequenceRecord}s depending on the
     *         {@link Converter} or {@code null} if one iteration over the sequences is finished.
     */
    public FutureSequenceRecords<H, S> next() throws IOException {
        if (keepInMemory) {
            if (iterator != null) {
                if (iterator.hasNext()) {
                    // case: All sequences are already in memory
                    MemoryEntry<H, S> entry = iterator.next();
                    sequencesRead = entry.sequencesRead;
                    bytesRead = entry.bytesRead;
                    return entry.futureSequenceRecords;
                } else {
                    // case: All sequences are already in memory and the iterator is at the end of the list
                    return null;
                }
            } else {
                if (finishedReading) {
                    // case: All sequences are in memory after the first iteration,
                    // but the reset() method has not been called.
                    return null;
                } else {
                    SequenceRecord<H, Character> sequenceRecord;
                    if ((sequenceRecord = sequenceReader.next()) != null) {
                        // case: First iteration
                        bytesRead = sequenceReader.getBytesRead();
                        sequencesRead++;
                        MemoryEntry<H, S> entry = new MemoryEntry<>(sequencesRead, bytesRead, null);
                        sequenceRecordContainers.add(entry);
                        return getToBeTranslatedContainer(converter, sequenceRecord, entry);
                    } else {
                        // case: First return of null during first iteration
                        finishedReading = true;
                        return next();
                    }
                }
            }
        } else {
            SequenceRecord<H, Character> sequenceRecord;
            if ((sequenceRecord = sequenceReader.next()) == null) {
                // case: End of file
                return null;
            } else {
                // case: Read next sequence
                bytesRead = sequenceReader.getBytesRead();
                sequencesRead++;
                return getToBeTranslatedContainer(converter, sequenceRecord, null);
            }

        }
    }

    /**
     * Method to create a {@link FutureSequenceRecords} that contains the input {@link SequenceRecord} and returns the
     * converted {@link SequenceRecord}(s) when the {@link FutureSequenceRecords#getSequenceRecords()} method is
     * called.
     * @param converter Converter to convert the sequences when the {@link FutureSequenceRecords#getSequenceRecords()}
     *                  is called. If {@code null}, the input {@link SequenceRecord} is returned.
     * @param sequenceRecord Input {@link SequenceRecord} to be converted
     * @param entry Memory entry to store the converted sequences in memory. If {@code null}, the sequences are not
     *              stored after conversion
     * @return {@link FutureSequenceRecords} containing the input {@link SequenceRecord}.
     */
    public FutureSequenceRecords<H, S> getToBeTranslatedContainer(
            Converter<Character, S> converter, SequenceRecord<H, Character> sequenceRecord, MemoryEntry<H, S> entry) {
        return new FutureSequenceRecords<H, S>() {
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
                    entry.futureSequenceRecords = new FutureSequenceRecords<H, S>() {
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

    /**
     * Resets the iterator to start from the beginning again. If the sequences are stored in memory, the iterator is
     * reset to the beginning of the list. Otherwise, the {@link SequenceReader} is reset to the beginning of the file.
     */
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

    /**
     * @return the size of the sequence file in bytes.
     */
    public long getFileSize() {
        return sequenceReader.getFileSize();
    }

    /**
     * Get the number of bytes that correspond to the bytes that have been read from the file. If the sequences are
     * read from memory, the number of bytes that have initially been read from the file up to the current sequence is
     * returned.
     * @return the number of bytes that have been read from the file or the memory.
     */
    public long getBytesRead() {
        return bytesRead;
    }

    /**
     * @return the number of sequences that have been read from the file or memory in the current iteration.
     */
    public Path getFile() {
        return sequenceReader.getFile();
    }

    /**
     * @return An approximation of the number of sequences in the file. The actual number of sequences can not be
     *         obtained without reading the whole file.
     */
    public int approximateNumberOfSequences() {
        return sequenceReader.approximateNumberOfSequences();
    }

    /**
     * Class to store the sequences in memory during the first iteration. To be able to return some kind of progress
     * status during further iterations, the sequencesRead and bytesRead are stored together with the sequences.
     */
    private static class MemoryEntry<H, S>{
        public final int sequencesRead;
        public final long bytesRead;
        public FutureSequenceRecords<H, S> futureSequenceRecords;

        public MemoryEntry(int sequencesRead, long bytesRead, FutureSequenceRecords<H, S> futureSequenceRecords) {
            this.sequencesRead = sequencesRead;
            this.bytesRead = bytesRead;
            this.futureSequenceRecords = futureSequenceRecords;
        }
    }
}
