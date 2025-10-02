package org.husonlab.diamer.io.seq;

import org.husonlab.diamer.seq.SequenceProcessor;
import org.husonlab.diamer.seq.SequenceRecord;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class SequenceBuffer<H, S> implements SequenceProcessor<H, S> {
    /**
     * List to keep the (future) Sequence records in memory.
     */
    private final List<BufferEntry<H, S>> buffer;
    private final SequenceProcessor<H, S> input;
    private boolean finishedReading;
    private Iterator<BufferEntry<H, S>> iterator;
    private long bytesRead;
    private int sequencesRead;

    public SequenceBuffer(SequenceProcessor<H, S> input) {
        this.buffer = new LinkedList<>();
        this.input = input;
        this.finishedReading = false;
        this.bytesRead = 0;
        this.sequencesRead = 0;
    }

    @Override
    public SequenceRecord<H, S> next() {
        if (finishedReading) {
            if (iterator != null && iterator.hasNext()) {
                BufferEntry<H, S> entry = iterator.next();
                this.sequencesRead = entry.sequencesRead;
                this.bytesRead = entry.bytesRead;
                return entry.sequenceRecord;
            } else {
                return null;
            }
        } else {
            SequenceRecord<H, S> seq = input.next();
            if (seq == null) {
                finishedReading = true;
                return null;
            } else {
                sequencesRead = input.getSequencesRead();
                bytesRead = input.getBytesRead();
                BufferEntry<H, S> entry = new BufferEntry<>(sequencesRead, bytesRead, seq);
                buffer.add(entry);
                return new SequenceRecord<H, S>(seq.getId(), seq.getSequence()) {
                    @Override
                    public S getSequence() {
                        S sequence = super.getSequence();
                        entry.setSequenceRecord(new SequenceRecord<>(super.getId(), sequence));
                        return sequence;
                    }
                };
            }
        }
    }

    @Override
    public void reset() {
        if (finishedReading) {
            this.iterator = buffer.iterator();
        } else {
            input.reset();
            this.buffer.clear();
        }
    }

    @Override
    public int getSequencesRead() {
        return sequencesRead;
    }

    @Override
    public long getBytesRead() {
        return bytesRead;
    }

    static class BufferEntry<H, S>{
        protected final int sequencesRead;
        protected final long bytesRead;
        protected SequenceRecord<H, S> sequenceRecord;

        public BufferEntry(int sequencesRead, long bytesRead, SequenceRecord<H, S> sequenceRecord) {
            this.sequencesRead = sequencesRead;
            this.bytesRead = bytesRead;
            this.sequenceRecord = sequenceRecord;
        }

        protected void setSequenceRecord(SequenceRecord<H, S> sequenceRecord) {
            this.sequenceRecord = sequenceRecord;
        }
    }
}
