package org.husonlab.diamer2.io.seq;

import org.husonlab.diamer2.seq.SequenceRecord;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

public class SequenceSupplierCompressed extends SequenceSupplier<Integer, byte[]>{
    /**
     * @param sequenceReader Reader to read the sequences from
     * @param converter      Converter to convert the sequences to a different alphabet
     * @param keepInMemory   Whether to keep the sequences in memory or not
     */
    public SequenceSupplierCompressed(@NotNull SequenceReader<Integer, char[]> sequenceReader, @NotNull Converter<byte[]> converter, boolean keepInMemory) {
        super(sequenceReader, converter, keepInMemory);
    }



    /**
     * Method to store the converted sequence records in a compressed format in memory.
     * Only the 4 LSD of the bytes are stored.
     */
    @Override
    protected FutureSequenceRecords<Integer, byte[]> getFutureSequenceRecords(
            Converter<byte[]> converter, SequenceRecord<Integer, char[]> sequenceRecord, MemoryEntry<Integer, byte[]> entry) {
        return new FutureSequenceRecords<Integer, byte[]>() {
            @Override
            public LinkedList<SequenceRecord<Integer, byte[]>> getSequenceRecords() {
                LinkedList<SequenceRecord<Integer, byte[]>> sequenceRecords = new LinkedList<>();
                for (byte[] sequence : converter.convert(sequenceRecord.sequence())) {
                    sequenceRecords.add(new SequenceRecord<>(sequenceRecord.id(), sequence));
                }
                if (entry != null) { // the sequenceSupplier should keep the sequences in memory
                    // compress converted sequences
                    long[][] compressedSequences = new long[sequenceRecords.size()][];
                    for (int i = 0; i < sequenceRecords.size(); i++) {
                        compressedSequences[i] = compressByteArray(sequenceRecords.get(i).sequence());
                    }
                    // store id separately
                    int id = sequenceRecord.id();
                    // remove original sequenceRecord
                    entry.futureSequenceRecords = new CompressedFutureSequenceRecords(id, sequenceRecords);
                }
                return sequenceRecords;
            }
        };
    }

    private static class CompressedFutureSequenceRecords implements FutureSequenceRecords<Integer, byte[]> {
        int id;
        long[][] compressedSequences;

        CompressedFutureSequenceRecords(int id, LinkedList<SequenceRecord<Integer, byte[]>> sequenceRecords) {
            this.id = id;
            compressedSequences = new long[sequenceRecords.size()][];
            for (int i = 0; i < sequenceRecords.size(); i++) {
                compressedSequences[i] = compressByteArray(sequenceRecords.get(i).sequence());
            }
        }

        @Override
        public Iterable<SequenceRecord<Integer, byte[]>> getSequenceRecords() {
            // decompress sequences
            LinkedList<SequenceRecord<Integer, byte[]>> decompressedSequenceRecords = new LinkedList<>();
            for (int i = 0; i < compressedSequences.length; i++) {
                decompressedSequenceRecords.add(new SequenceRecord<>(id, decompressByteArray(compressedSequences[i])));
            }
            return decompressedSequenceRecords;
        }
    }

    private static long[] compressByteArray(byte[] arr) {
        long[] compressed = new long[((arr.length + 8) / 16) + 1];
        compressed[0] = arr.length;
        for (int i = 0; i < arr.length; i++) {
            // pack 4 LSD of input value into the long
            compressed[(i + 8) / 16] |= ((long) (arr[i] & 0xF)) << (((i + 8) % 16) * 4);
        }
        return compressed;
    }

    private static byte[] decompressByteArray(long[] arr) {
        byte[] decompressed = new byte[(int) (arr[0] & 0xFFFFFFFFL)];
        for (int i = 0; i < decompressed.length; i++) {
            // unpack 4 LSD of input value from the long
            decompressed[i] = (byte) ((arr[(i + 8) / 16] >> (((i + 8) % 16) * 4)) & 0xF);
        }
        return decompressed;
    }
}
