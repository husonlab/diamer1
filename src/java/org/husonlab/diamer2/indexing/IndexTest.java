package org.husonlab.diamer2.indexing;

import org.husonlab.diamer2.seq.FASTA;
import static org.husonlab.diamer2.alphabet.AminoAcids.enforceAAAlphabet;
import static org.husonlab.diamer2.alphabet.AminoAcids.to11Num_15;

import java.util.ArrayList;

public class IndexTest extends Index {

    long kmerCount = 0;
    long sequenceCount = 0;

    public void processFASTAs_old(ArrayList<FASTA> fastas, int taxId) {
        for (FASTA fasta: fastas){
            fasta = enforceAAAlphabet(fasta);
            String seq = fasta.getSequence();

            long id = ((long) taxId) << 32;
            int i = 0;
            while(i + 15 < seq.length()){
                String kmer = seq.substring(i, i + 15);
                long idAndSeq = id | to11Num_15(kmer);
                index.add(idAndSeq);
                i++;
            }
        }
    }

    @Override
    public void processFASTAs(ArrayList<FASTA> fastas, int taxId) {
        for (FASTA fasta: fastas){
            sequenceCount++;
            fasta = enforceAAAlphabet(fasta);
            String seq = fasta.getSequence();

            long id = ((long) taxId) << 32;
            int i = 0;
            while(i + 15 < seq.length()){
                String kmer = seq.substring(i, i + 15);
                //long idAndSeq = id | to11Num_15(kmer);
                kmerCount++;
                i++;
            }
        }
    }

    public long getKmerCount(){
        return kmerCount;
    }
    public long getSequenceCount(){
        return sequenceCount;
    }
}
