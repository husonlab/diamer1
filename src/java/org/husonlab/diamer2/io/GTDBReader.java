package org.husonlab.diamer2.io;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.husonlab.diamer2.graph.Tree;
import org.husonlab.diamer2.indexing.Index;
import org.husonlab.diamer2.seq.FASTA;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

public class GTDBReader {
    public static Tree readTaxonomy(String filename) throws IOException {
        Tree tree = new Tree();
        try (BufferedReader br = Files.newBufferedReader(Paths.get(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split("\t");
                String seqId = values[0];
                String[] taxonomy = values[1].split(";");
                String domain = taxonomy[0];
                String phylum = taxonomy[1];
                String clazz = taxonomy[2];
                String order = taxonomy[3];
                String family = taxonomy[4];
                String genus = taxonomy[5];
                String species = taxonomy[6];
                tree.addLeaf(domain, phylum, clazz, order, family, genus, species, seqId);
            }
        }
        return tree;
    }

    public static long readAAFastas(String filename, Index index) throws IOException {

        long fastaCount = 0;

        try (FileInputStream fis = new FileInputStream(filename);
             GZIPInputStream gis = new GZIPInputStream(fis);
             TarArchiveInputStream tis = new TarArchiveInputStream(gis)) {

            TarArchiveEntry entry;
            int taxId = 0;

            while ((entry = tis.getNextEntry()) != null) {
                if (entry.isFile() && entry.getName().endsWith("faa.gz")) {
                    fastaCount++;
                    System.out.println("Reading: " + entry.getName());
                    // Reads bytes to a buffer and generates new Stream to pass to FASTAReader
                    // This is necessary to avoid closing the TarArchiveInputStream before all contained files are read
                    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    long bytesToRead = entry.getSize();
                    int bytesRead;
                    while ((bytesRead = tis.read(buffer, 0, (int)Math.min(buffer.length, bytesToRead))) != -1) {
                        byteStream.write(buffer, 0, bytesRead);
                        bytesToRead -= bytesRead;
                        if (bytesToRead <= 0) break;
                    }

                    // Create new GZIPInputStream from buffer and read FASTA entries
                    GZIPInputStream streamFromBuffer = new GZIPInputStream(new ByteArrayInputStream(byteStream.toByteArray()));
                    ArrayList<FASTA> fastas = FASTAReader.readStream(streamFromBuffer);
                    index.processFASTAs(fastas, taxId);
                    taxId++;
                }
            }
        }
        return fastaCount;
    }
}
