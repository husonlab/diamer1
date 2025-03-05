package org.husonlab.diamer2.io.seq;

import org.husonlab.diamer2.seq.Sequence;
import org.husonlab.diamer2.seq.SequenceRecord;
import org.husonlab.diamer2.seq.alphabet.Alphabet;
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
 * @param <C> Type of the sequence (determined by the {@link Converter} or the {@link SequenceReader} if no converter is
 *           supplied)
 */
public class SequenceSupplier<H, S1, A1 extends Alphabet<S1>, S2, A2 extends Alphabet<S2>> implements AutoCloseable {
    /**
     * List to keep the (future) sequence records in memory.
     */
    private final LinkedList<MemoryEntry<H, S2, A2>> FutureSequenceBuffer;
    private final SequenceReader<H, S1, A1> sequenceReader;
    private final Converter<S1, A1, S2, A2> converter;
    private final boolean keepInMemory;
    private boolean finishedReading;
    private Iterator<MemoryEntry<H, S2, A2>> iterator;
    private long bytesRead;
    private int sequencesRead;

    /**
     * @param sequenceReader Reader to read the sequences from
     * @param converter Converter to convert the sequences to a different alphabet
     * @param keepInMemory Whether to keep the sequences in memory or not
     */
    public SequenceSupplier(@NotNull SequenceReader<H, S1, A1> sequenceReader, @Nullable Converter<S1, A1, S2, A2> converter, boolean keepInMemory) {
        this.sequenceReader = sequenceReader;
        this.converter = converter;
        this.keepInMemory = keepInMemory;
        this.FutureSequenceBuffer = new LinkedList<>();
        this.finishedReading = false;
        this.bytesRead = 0;
        this.sequencesRead = 0;
    }

    /**
     * Reads the next sequence record from the input file or gets it from the buffer of this class.
     * <p>
     *     The method returns a {@link FutureSequenceRecords} instead of a single {@link SequenceRecord}, because
     *     the converters potentially convert a single sequence into multiple sequences. The conversion itself is only
     *     performed once the {@link FutureSequenceRecords#getSequenceRecords()} method is called. In case the
     *     sequences should be stored in memory this call also replaces the FutureSequenceRecord in the buffer with the
     *     converted sequence record so that conversion is only performed once.
     * </p>
     * @return {@link FutureSequenceRecords} containing potentially multiple {@link SequenceRecord}s depending on the
     *         {@link Converter} or {@code null} if one iteration over the sequences is finished.
     */
    public FutureSequenceRecords<H, S2, A2> next() throws IOException {
        if (keepInMemory) {
            if (iterator != null) {
                if (iterator.hasNext()) {
                    // case: All sequences are already in memory
                    MemoryEntry<H, S2, A2> entry = iterator.next();
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
                    SequenceRecord<H, S1, A1> sequenceRecord;
                    if ((sequenceRecord = sequenceReader.next()) != null) {
                        // case: First iteration
                        bytesRead = sequenceReader.getBytesRead();
                        sequencesRead++;
                        MemoryEntry<H, S2, A2> entry = new MemoryEntry<>(sequencesRead, bytesRead, null);
                        FutureSequenceBuffer.add(entry);
                        FutureSequenceRecords<H, S2, A2> futureSequenceRecords = getToBeTranslatedContainer(converter, sequenceRecord, entry);
                        entry.futureSequenceRecords = futureSequenceRecords;
                        return futureSequenceRecords;
                    } else {
                        // case: First return of null during first iteration
                        finishedReading = true;
                        return next();
                    }
                }
            }
        } else {
            SequenceRecord<H, S1, A1> sequenceRecord;
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
    private FutureSequenceRecords<H, S2, A2> getToBeTranslatedContainer(
            Converter<S1, A1, S2, A2> converter, SequenceRecord<H, S1, A1> sequenceRecord, MemoryEntry<H, S2, A2> entry) {
        return new FutureSequenceRecords<H, S2, A2>() {
            @Override
            public LinkedList<SequenceRecord<H, S2, A2>> getSequenceRecords() {
                LinkedList<SequenceRecord<H, S2, A2>> sequenceRecords = new LinkedList<>();
                if (converter != null) {
                    H header = sequenceRecord.id();
                    for (Sequence<S2, A2> sequence : converter.convert(sequenceRecord.sequence())) {
                        sequenceRecords.add(new SequenceRecord<>(header, sequence));
                    }
                } else {
                    sequenceRecords.add((SequenceRecord<H, S2, A2>) sequenceRecord);
                }
                if (entry != null) {
                    entry.futureSequenceRecords = new FutureSequenceRecords<H, S2, A2>() {
                        @Override
                        public LinkedList<SequenceRecord<H, S2, A2>> getSequenceRecords() {
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
            iterator = FutureSequenceBuffer.iterator();
        } else {
            FutureSequenceBuffer.clear();
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
     * @return the file that is read by the {@link SequenceReader}.
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
    private static class MemoryEntry<H, S2, A2 extends Alphabet<S2>>{
        public final int sequencesRead;
        public final long bytesRead;
        public FutureSequenceRecords<H, S2, A2> futureSequenceRecords;

        public MemoryEntry(int sequencesRead, long bytesRead, FutureSequenceRecords<H, S2, A2> futureSequenceRecords) {
            this.sequencesRead = sequencesRead;
            this.bytesRead = bytesRead;
            this.futureSequenceRecords = futureSequenceRecords;
        }
    }
}
