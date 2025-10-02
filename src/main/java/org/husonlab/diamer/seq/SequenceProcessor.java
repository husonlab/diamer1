package org.husonlab.diamer.seq;

import org.jetbrains.annotations.Nullable;

public interface SequenceProcessor <H, S> {
    @Nullable
    SequenceRecord<H, S> next();

    void reset();

    int getSequencesRead();

    long getBytesRead();
}
