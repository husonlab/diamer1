package org.husonlab.diamer2.indexing;

import org.husonlab.diamer2.seq.FASTA;
import static org.husonlab.diamer2.alphabet.AminoAcids.enforceAAAlphabet;
import static org.husonlab.diamer2.alphabet.AminoAcids.to11Num_15;

import java.util.ArrayList;

public class Al11k15 extends Bucket {

    long kmerCount = 0;

    public Al11k15(short bucket) {
        super(bucket);
    }

    public void processFASTAs_old(ArrayList<FASTA> fastas, int taxId) {
        for (FASTA fasta: fastas){
            fasta = enforceAAAlphabet(fasta);
            String seq = fasta.getSequence();

            int i = 0;
            while(i + 15 < seq.length()){
                String kmer = seq.substring(i, i + 15);
                long idAndSeq = (long) taxId | (to11Num_15(kmer) << 22);
                index.add(idAndSeq);
                i++;
            }
        }
    }

    @Override
    public void processFASTA(FASTA fasta, int taxId) {
        //fasta = enforceAAAlphabet(fasta);
        String seq = fasta.getSequence();

        int i = 0;
        while(i + 15 < seq.length()){
            String kmer = seq.substring(i, i + 15);
            long kmerNum = to11Num_15(kmer);
            if ((kmerNum & (bucket & 0b1111111111)) == (bucket & 0b1111111111)) {
                long numAndSeq = (long) taxId | (kmerNum << 22);
                index.add(numAndSeq);
            }
            //kmerCount++;
            i++;
        }
    }

    public long getKmerCount(){
        return kmerCount;
    }
}
