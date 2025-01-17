package org.husonlab.diamer2.io.seq;

import org.husonlab.diamer2.seq.alphabet.converter.Converter;

import java.util.ArrayList;

public class SequenceSupplier implements AutoCloseable {

    private final ArrayList<short[]> sequences;
    private final ArrayList<String> headers;
    private final ArrayList<Long> bytesRead;
    private final SequenceReader sequenceReader;
    private final boolean keepInMemory;
    private final Converter<Character, Short> converter;

    public SequenceSupplier(SequenceReader sequenceReader, boolean keepInMemory, Converter<Character, Short> converter) {
        this.sequenceReader = sequenceReader;
        this.keepInMemory = keepInMemory;
        this.converter = converter;
    }

    @Override
    public void close() throws Exception {

    }
}
