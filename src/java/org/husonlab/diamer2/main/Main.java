package org.husonlab.diamer2.main;

import static org.husonlab.diamer2.indexing.Sorting.radixSort42bits;

import java.io.*;
import java.util.Arrays;


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

//        NCBIReader.AccessionMapping[] mappings = {
//                new NCBIReader.AccessionMapping(args[2], 1, 2)
//        };
//        NCBIReader.Tree tree = NCBIReader.readTaxonomyWithAccessions(args[0], args[1], mappings, true);
//        NCBIReader.annotateNrWithLCA(args[4], args[5], tree);
//        NCBIReader.Tree tree = NCBIReader.readTaxonomy(args[1], args[2]);
//        Indexer indexer = new Indexer(tree, Integer.parseInt(args[0]), 1000, 100, new short[]{0, 10});
//        indexer.index(args[3]);
//        RankMapping.computeKmerRankMapping(indexer.getBuckets(), tree);

        long[] arr = {431L, 1973L, 1L, 7365L, 913L, 64L};
        System.out.println(Arrays.toString(radixSort42bits(arr)));
    }
}