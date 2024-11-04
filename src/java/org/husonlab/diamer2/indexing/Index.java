package org.husonlab.diamer2.indexing;

import org.husonlab.diamer2.seq.FASTA;

import java.util.ArrayList;

public abstract class Index {
    public ArrayList<Long> index;
    short bucket;

    public Index(short bucket) {
        index = new ArrayList<>();
        this.bucket = bucket;
    }

    public abstract void processFASTA(FASTA fasta, int taxId);

}
