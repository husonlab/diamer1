package org.husonlab.diamer2.indexing;

import org.husonlab.diamer2.seq.FASTA;

import java.util.ArrayList;

public abstract class Index {
    ArrayList<Long> index;

    public Index() {
        index = new ArrayList<>();
    }

    public abstract void processFASTAs(ArrayList<FASTA> fastas, int taxId);

    public ArrayList<Long> getIndex() {
        return index;
    }
}
