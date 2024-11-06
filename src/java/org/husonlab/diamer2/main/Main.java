package org.husonlab.diamer2.main;

import jloda.seq.FastA;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.husonlab.diamer2.alphabet.AminoAcids;
import org.husonlab.diamer2.graph.Tree;
import org.husonlab.diamer2.indexing.Index;
import org.husonlab.diamer2.indexing.IndexTest;
import org.husonlab.diamer2.io.FASTAReader;
import org.husonlab.diamer2.io.NCBIReader;
import org.husonlab.diamer2.seq.FASTA;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;


public class Main {
    public static void main(String[] args) {

        String pathNodes = "F:\\Studium\\Master\\semester5\\thesis\\diamer2\\src\\test\\resources\\NCBI\\taxdmp\\nodes.dmp";
        String pathNames = "F:\\Studium\\Master\\semester5\\thesis\\diamer2\\src\\test\\resources\\NCBI\\taxdmp\\names.dmp";
        String accessionsReduced = "F:\\Studium\\Master\\semester5\\thesis\\data\\NCBI\\reduced\\accessions100.txt";
        String pathAccessions = "F:\\Studium\\Master\\semester5\\thesis\\data\\NCBI\\reduced\\prot.accession2taxid.FULL100.gz";
        String pathAccessionsDead = "F:\\Studium\\Master\\semester5\\thesis\\data\\NCBI\\reduced\\dead_prot.accession2taxid100.gz";
        String nr = "F:\\Studium\\Master\\semester5\\thesis\\data\\NCBI\\reduced\\nr100.fsa";

        try {
            IndexTest index = runIndexing(nr);
            System.out.println(index.getKmerCount());
        } catch (IOException e) {
            e.printStackTrace();
        }


        /*
        try {
            Tree tree = NCBIReader.readTaxonomy(pathNodes);
            System.out.println(tree.getNodeMap().size());
            NCBIReader.addTaxonomicLabels(tree, pathNames);
            NCBIReader.addAccessions(tree, pathAccessions, (short) 0, (short) 1);
            NCBIReader.addAccessions(tree, pathAccessionsDead, (short) 1, (short) 2);
            System.out.println(tree.getAccessionMap().size());
            HashSet<String> accessions = new HashSet<>();
            // read in the accessions
            try (BufferedReader br = Files.newBufferedReader(Paths.get(accessionsReduced))) {
                String line;
                while ((line = br.readLine()) != null) {
                    accessions.add(line);
                }
            }
            for (String accession: tree.getAccessionMap().keySet()) {
                accessions.remove(accession);
            }
            System.out.println(accessions.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("done");

        try (FileInputStream fis = new FileInputStream(path);
             GZIPInputStream gis = new GZIPInputStream(fis);
             TarArchiveInputStream tar = new TarArchiveInputStream(gis)) {

            TarArchiveEntry entry;
            while ((entry = tar.getNextEntry()) != null) {
                System.out.println(entry.getName());
                if (entry.isFile()) {
                    try (GZIPInputStream innerGis = new GZIPInputStream(tar)) {
                        ArrayList<FASTA> fastas = FASTAReader.readStream(innerGis);
                        System.out.println(fastas.get(0).getHeader());
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] bytearray = new byte[8];
        bytearray[2] = 2;
        bytearray[7] = 8;

        var test = MurmurHash3.murmurhash3x8632(bytearray, 0, 8, 0);
        System.out.println(test);

        try {
            var test = FastAFileIterator.getFastAOrFastQAsFastAIterator("G:\\Studium\\Master\\semester5\\thesis\\diamer2\\src\\test\\resources\\GTDB\\extracted_subset\\GCA_000008885.1_protein.faa");
            while (test.hasNext()) {
                var record = test.next();
                System.out.println(record.getFirst());
                System.out.println(record.getSecond());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            Tree tree = GTDBReader.readTaxonomy("G:\\Studium\\Master\\semester5\\thesis\\diamer2\\src\\test\\resources\\GTDB\\bac120_taxonomy_r220.tsv");
            System.out.println(tree);
            for (Node node: tree.byLabel("f__SURF-5").getChildren()) {
                System.out.println(node);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        */
    }

    private static IndexTest runIndexing(String fastaPath) throws IOException {
        /*
        Indexes a database file
        @param fastaPath: the path to the database FASTA file
         */
        IndexTest index = new IndexTest((short)0b1011111111);
        String header = null;
        StringBuilder sequence = new StringBuilder();
        try (InputStream inputStream = new FileInputStream(fastaPath);
             BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.strip();
                if (line.startsWith(">")) {
                    if (header != null) {
                        FASTA fasta = new FASTA(header, sequence.toString());
                        index.processFASTA(fasta, 0b1001001001);
                        sequence = new StringBuilder();
                    }
                    header = line.substring(1);
                } else if (header != null) {
                    sequence.append(line);
                }
            }
            FASTA fasta = new FASTA(header, sequence.toString());
            index.processFASTA(fasta, 0b1001001001);
        }
        return index;
    }
}