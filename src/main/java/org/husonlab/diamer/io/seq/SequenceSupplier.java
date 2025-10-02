package org.husonlab.diamer.io.seq;

import org.husonlab.diamer.seq.SequenceRecord;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Class for iterating over Sequence files multiple times. After each iteration, the {@link #reset()} method must be
 * called to start from the beginning again.
 * <p>
 *     The most important feature is that it can keep the sequences in memory, so that all but the first iteration
 *     don't need another IO operation. Additionally, a converter can be supplied to convert the sequences in a different
 *     alphabet that might require less memory. The conversion to a different alphabet is thereby only performed, when
 *     the Sequence is actually requested, so that the computation is performed on the threads that process the
 *     sequences.
 * </p>
 * @param <H> Type of the header of the Sequence (determined by the {@link SequenceReader})
 */
public class SequenceSupplier<H, S> implements AutoCloseable {

    /**
     * List to keep the (future) Sequence records in memory.
     */
    private final LinkedList<MemoryEntry<H, S>> sequenceMemory;
    private final SequenceReader<H, char[]> sequenceReader;
    private final Converter<S> converter;
    /**
     * variable to use for reading from the SequenceReader
     */
    SequenceRecord<H, char[]> sequenceRecord;
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
    public SequenceSupplier(@NotNull SequenceReader<H, char[]> sequenceReader, @NotNull Converter<S> converter, boolean keepInMemory) {
        this.sequenceReader = sequenceReader;
        this.converter = converter;
        this.keepInMemory = keepInMemory;
        this.sequenceMemory = new LinkedList<>();
        this.finishedReading = false;
        this.bytesRead = 0;
        this.sequencesRead = 0;
    }

    /**
     * Reads the next Sequence record from the input file or gets it from the buffer of this class.
     * <p>
     *     The method returns a {@link FutureSequenceRecords} instead of a single {@link SequenceRecord}, because
     *     the converters potentially convert a single Sequence into multiple sequences. The conversion itself is only
     *     performed once the {@link FutureSequenceRecords#getSequenceRecords()} method is called. In case the
     *     sequences should be stored in memory this call also replaces the FutureSequenceRecord in the buffer with the
     *     converted Sequence record so that conversion is only performed once.
     * </p>
     * @return {@link FutureSequenceRecords} containing potentially multiple {@link SequenceRecord}s depending on the
     *         {@link Converter} or {@code null} if one iteration over the sequences is finished.
     */
    public FutureSequenceRecords<H, S> next() throws IOException {
        // case 1: sequences should be cached in memory
        if (keepInMemory) {
            // case 1.1: iterator over memory is already initialized: all sequences are in memory
            if (iterator != null) {
                // case: 1.1.1: the iterator is not at the end of the cached list of sequences
                if (iterator.hasNext()) {
                    MemoryEntry<H, S> entry = iterator.next();
                    sequencesRead = entry.sequencesRead;
                    bytesRead = entry.bytesRead;
                    return entry.futureSequenceRecords;
                // case: 1.1.2: the iterator is at the end of the cached list of sequences
                } else {
                    return null;
                }
            // case 1.2: iterator over memory is not initialized: not all sequences are in memory
            } else {
                // case: 1.2.1: all sequences are already in memory, but the reset() method has not been called yet
                if (finishedReading) {
                    return null;
                // case: 1.2.2: reading from the SequenceReader is still ongoing
                } else {
                    // case: 1.2.2.1: reading a Sequence was successful
                    if ((sequenceRecord = sequenceReader.next()) != null) {
                        bytesRead = sequenceReader.getBytesRead();
                        sequencesRead++;
                        MemoryEntry<H, S> entry = new MemoryEntry<>(sequencesRead, bytesRead, null);
                        sequenceMemory.add(entry);
                        FutureSequenceRecords<H, S> futureSequenceRecords = getFutureSequenceRecords(converter, sequenceRecord, entry);
                        entry.futureSequenceRecords = futureSequenceRecords;
                        return futureSequenceRecords;
                    // case 1.2.2.2: SequenceReader reached end of file
                    } else {
                        // set finishedReading flag
                        finishedReading = true;
                        return next(); // returns null (case 1.2.1)
                    }
                }
            }
        // case 2: sequences should not be cached in memory
        } else {
            // case 2.1: reading a Sequence was not successful (i.e. end of file)
            if ((sequenceRecord = sequenceReader.next()) == null) {
                return null;
            // case 2.2: reading a Sequence was successful
            } else {
                bytesRead = sequenceReader.getBytesRead();
                sequencesRead++;
                return getFutureSequenceRecords(converter, sequenceRecord, null);
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
    protected FutureSequenceRecords<H, S> getFutureSequenceRecords(
            Converter<S> converter, SequenceRecord<H, char[]> sequenceRecord, MemoryEntry<H, S> entry) {
        return new FutureSequenceRecords<H, S>() {
            /**
             * Method to get the (converted) Sequence records from this object.
             * Depending if the conversion has to be done, this call might need resources.
             * @return List of (converted) Sequence records
             */
            @Override
            public LinkedList<SequenceRecord<H, S>> getSequenceRecords() {
                LinkedList<SequenceRecord<H, S>> sequenceRecords = new LinkedList<>();
                for (S sequence : converter.convert(sequenceRecord.getSequence())) {
                    sequenceRecords.add(new SequenceRecord<>(sequenceRecord.getId(), sequence));
                }
                if (entry != null) {
                    entry.futureSequenceRecords = new FutureSequenceRecords<>() {
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
            iterator = sequenceMemory.iterator();
        } else {
            sequenceMemory.clear();
            finishedReading = false;
            sequenceReader.reset();
        }
    }

    @Override
    public void close() throws IOException {
        sequenceReader.close();
    }

    /**
     * @return the size of the Sequence file in bytes.
     */
    public long getFileSize() {
        return sequenceReader.getFileSize();
    }

    /**
     * Get the number of bytes that correspond to the bytes that have been read from the file. If the sequences are
     * read from memory, the number of bytes that have initially been read from the file up to the current Sequence is
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
    static class MemoryEntry<H, S>{
        protected final int sequencesRead;
        protected final long bytesRead;
        protected FutureSequenceRecords<H, S> futureSequenceRecords;

        public MemoryEntry(int sequencesRead, long bytesRead, FutureSequenceRecords<H, S> futureSequenceRecords) {
            this.sequencesRead = sequencesRead;
            this.bytesRead = bytesRead;
            this.futureSequenceRecords = futureSequenceRecords;
        }
    }

    public interface Converter<S> {
        S[] convert(char[] sequence);
    }

    public static Converter<String> getEmptyConverter() {
        return sequence -> new String[]{new String(sequence)};
    }
}
