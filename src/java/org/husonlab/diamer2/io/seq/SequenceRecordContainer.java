package org.husonlab.diamer2.io.seq;

import org.husonlab.diamer2.seq.SequenceRecord;

import java.util.LinkedList;

public abstract class SequenceRecordContainer<H, S> {
    abstract public LinkedList<SequenceRecord<H, S>> getSequenceRecords();
}
