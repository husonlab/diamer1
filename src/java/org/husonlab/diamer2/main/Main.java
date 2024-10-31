package org.husonlab.diamer2.main;

import jloda.seq.FastA;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.husonlab.diamer2.alphabet.AminoAcids;
import org.husonlab.diamer2.indexing.Index;
import org.husonlab.diamer2.indexing.IndexTest;
import org.husonlab.diamer2.io.FASTAReader;
import org.husonlab.diamer2.io.GTDBReader;
import org.husonlab.diamer2.seq.FASTA;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;


public class Main {
    public static void main(String[] args) {

        String path = "F:\\Studium\\Master\\semester5\\thesis\\diamer2\\src\\test\\resources\\GTDB\\extracted_subset\\gtdb_proteins_aa_reps_r220_reduced100.tar.gz";
        String path2 = "G:\\Studium\\Master\\semester5\\thesis\\diamer2\\src\\test\\resources\\GTDB\\extracted_subset\\GCA_000170815.1_protein.faa";
        String path3 = "G:\\Studium\\Master\\semester5\\thesis\\diamer2\\src\\test\\resources\\GTDB\\extracted_subset\\GB_GCA_000170815.1_protein.faa.gz";
        String path4 = "G:\\Studium\\Master\\semester5\\thesis\\diamer2\\src\\test\\resources\\GTDB\\extracted_subset\\testFasta.faa";



        /*
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
}