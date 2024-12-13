package org.husonlab.diamer2.util;


import org.husonlab.diamer2.io.FASTAReader;
import org.husonlab.diamer2.seq.Sequence;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

public class ReducePreprocessedNr {
    public static void main(String[] args) {
        int skip = Integer.parseInt(args[2]);
        int i = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(args[0]));
             FASTAReader fastaReader = new FASTAReader(br);
             PrintWriter writer = new PrintWriter(args[1])) {
            Sequence sequence;
            while ((sequence = fastaReader.getNextSequence()) != null) {
                if (i++ % skip == 0) {
                    writer.println(sequence);
                }
            }
    } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}