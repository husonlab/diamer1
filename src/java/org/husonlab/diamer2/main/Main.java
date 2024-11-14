package org.husonlab.diamer2.main;

import org.husonlab.diamer2.indexing.Al11k15;
import org.husonlab.diamer2.indexing.Indexer;
import org.husonlab.diamer2.io.NCBIReader;
import org.husonlab.diamer2.seq.FASTA;

import java.io.*;


public class Main {
    public static void main(String[] args) throws IOException {

//        String pathNodes = "C:\\Users\\noel\\Documents\\diamer2\\src\\test\\resources\\NCBI\\reduced\\nodes100.dmp";
//        String pathNames = "C:\\Users\\noel\\Documents\\diamer2\\src\\test\\resources\\NCBI\\reduced\\names100.dmp";
//        String pathAccessions = "C:\\Users\\noel\\Documents\\diamer2\\src\\test\\resources\\NCBI\\reduced\\prot.accession2taxid.FULL100.gz";
//        String pathAccessionsDead = "C:\\Users\\noel\\Documents\\diamer2\\src\\test\\resources\\NCBI\\reduced\\dead_prot.accession2taxid100.gz";
//        String nr = "F:\\Studium\\Master\\semester5\\thesis\\data\\NCBI\\reduced\\nr100.fsa";
//        String pathNodesFull = "C:\\Users\\noel\\Documents\\diamer2\\src\\test\\resources\\NCBI\\taxdmp\\nodes.dmp";
//        String pathNamesFull = "C:\\Users\\noel\\Documents\\diamer2\\src\\test\\resources\\NCBI\\taxdmp\\names.dmp";
//        String pathAccessionsFull = "F:\\Studium\\Master\\semester5\\thesis\\data\\NCBI\\taxmapping\\prot.accession2taxid.FULL.gz";
//        String pathAccessionsDeadFull = "F:\\Studium\\Master\\semester5\\thesis\\data\\NCBI\\taxmapping\\dead_prot.accession2taxid.gz";

        NCBIReader.AccessionMapping[] mappings = {new NCBIReader.AccessionMapping(args[3], 1, 2)};
        NCBIReader.Tree tree = NCBIReader.readTaxonomy(args[1], args[2], mappings);
        Indexer indexer = new Indexer(tree, Integer.parseInt(args[0]), 1000, 100, new short[]{0, 10});
        indexer.index(args[4]);
    }
}