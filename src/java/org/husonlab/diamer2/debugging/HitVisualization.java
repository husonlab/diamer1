package org.husonlab.diamer2.debugging;

import org.husonlab.diamer2.alphabet.AAEncoder;
import org.husonlab.diamer2.alphabet.DNAEncoder;

import java.util.HashMap;

public class HitVisualization {

    /**
     * To test and visualize manually which kmers map to which other kmers in a protein and DNA sequence for debugging.
     */
    public static void main(String[] args) {
        String proteinSequence = "MPPSRPAKRSADGGGISDDDTAALGGGKSKQARSDRGPEDFSSVVKNRLQSYSRTGQACDRCKVRKIRCDALAEGCSHCINLNLECYVTDRVTGRTERRGYLQQLEREKNSMLTHIRDLERLCYEITVYQLSADDLRSSAQSPSEPGAGELPGSTGGGSKLTDGWSRYGALWIKYASTSQPADATIRPRIPQREWQSRPDQICWGVVGDDAPFSSLKGTTLTLLGTTIETTSFDAPDIDEPAAGVDSSMPLYNKSMLAFLRSSMGVNPVVQAELPSRENAFMYAEWYFISVACFLPLLHKPTFFKLVSSSCCF";
        String dnaSequence = "GTTTCGACGCGTCTAGTAGGCGCGAGTCGACTATGTGCCATTAAAGTATTCAGCGACAAAACCAGTTGACACCAGCTCCAACGAAAATCTCTCGATTCTTCAAGCTCTAAGTCCAGTTCGCACAAGAAGAGTAGCCGCAAGAACCCTCAATTCTTCTTCTACCGACTACAGGAAACCTACACCTACTCATCTCTAAACCGGAATCGAGCTCTTCTTCCGTGTACTCCTCCGGTTC";

        HashMap<Long, String> proteinIndex = new HashMap<>();

        for (int i = 0; i < proteinSequence.length() - 14; i++) {
            String kmer = proteinSequence.substring(i, i + 15);
            long kmerEnc = AAEncoder.toBase11(kmer);
            proteinIndex.put(kmerEnc, kmer);
        }
        int count = 0;
        for (int i = 0; i < dnaSequence.length() - 44; i++) {
            String kmerF = dnaSequence.substring(i, i + 45);
            String kmerR = new StringBuilder(kmerF).reverse().toString();
            long kmerFEnc = DNAEncoder.toAAAndBase11(kmerF);
            long kmerREnc = DNAEncoder.toAAAndBase11(kmerR);
            if (proteinIndex.containsKey(kmerFEnc)) {
                System.out.println(++count + "\tProtein kmer: " + proteinIndex.get(kmerFEnc) + " DNA kmer: " + kmerF);
            }
            if (proteinIndex.containsKey(kmerREnc)) {
                System.out.println(++count + "\tProtein kmer: " + proteinIndex.get(kmerREnc) + " DNA kmer: " + kmerR);
            }
        }
    }
}
