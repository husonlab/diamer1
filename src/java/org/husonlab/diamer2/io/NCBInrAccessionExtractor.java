package org.husonlab.diamer2.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;

public class NCBInrAccessionExtractor {
    public static HashSet<String> extractAccessions(String nrPath, int threads) throws IOException {
        HashSet<String> accessions = new HashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(nrPath))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(">")) {
                    String[] split = line.split("\\|");
                    accessions.add(split[1]);
                }
            }
        }
        return accessions;
    }
}
