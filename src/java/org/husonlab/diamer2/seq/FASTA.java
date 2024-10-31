package org.husonlab.diamer2.seq;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

public class FASTA {

    String header;
    String sequence;

    public FASTA(String header, String sequence) {
        /*
        @param header: the header of the FASTA entry without ">"
        @param sequence: the sequence of the FASTA entry
         */
        this.header = header;
        this.sequence = sequence;
    }

    public String getHeader() {
        return ">" + header;
    }

    public String getSequence() {
        return sequence;
    }

    public String toString() {
        return ">%s\n%s".formatted(header, sequence);
    }

}
