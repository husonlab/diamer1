package org.husonlab.diamer.seq.converter;

import org.husonlab.diamer.seq.SequenceProcessor;
import org.husonlab.diamer.seq.SequenceRecord;
import org.jetbrains.annotations.Nullable;

public class Translator<H> implements SequenceProcessor<H, String> {

    @Override
    public @Nullable SequenceRecord<H, String> next() {
        return null;
    }

    @Override
    public void reset() {

    }

    @Override
    public int getSequencesRead() {
        return 0;
    }

    @Override
    public long getBytesRead() {
        return 0;
    }
}
