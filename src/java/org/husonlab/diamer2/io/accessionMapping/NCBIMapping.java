package org.husonlab.diamer2.io.accessionMapping;

import org.husonlab.diamer2.io.CountingInputStream;
import org.husonlab.diamer2.taxonomy.Tree;
import org.husonlab.diamer2.util.logging.*;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class NCBIMapping extends AccessionMapping {

    private final Logger logger;
    private final HashMap<String, Integer> accessionMap;
    private final Tree tree;

    public NCBIMapping(Iterable<Path> ncbiMappingFiles, Tree tree, HashMap<String, Integer> neededAccessions) {
        logger = new Logger("NCBIMapping");
        logger.addElement(new Time());
        accessionMap = neededAccessions;
        this.tree = tree;

        for (Path ncbiMappingFile : ncbiMappingFiles) {
            logger.logInfo("Reading accession taxID mapping from: " + ncbiMappingFile);
            readAccessionMap(ncbiMappingFile);
        }
        logger.logInfo("Finished reading accession taxID mapping (%d entries)".formatted(accessionMap.size()));
    }

    @Override
    public int getTaxId(String accession) {
        return accessionMap.getOrDefault(removeVersion(accession), -1);
    }

    @Override
    public ArrayList<Integer> getTaxIds(List<String> accessions) {
        ArrayList<Integer> taxIds = new ArrayList<>(accessions.size());
        for (String accession : accessions) {
            taxIds.add(accessionMap.getOrDefault(removeVersion(accession), -1));
        }
        return taxIds;
    }

    public void readAccessionMap(Path ncbiMappingFile) {
        ProgressBar progressBar = new ProgressBar(ncbiMappingFile.toFile().length(), 20);
        ProgressLogger progressLogger = new ProgressLogger("Accessions");
        new OneLineLogger("NCBIMapping", 500)
                .addElement(new RunningTime())
                .addElement(progressBar)
                .addElement(progressLogger);

        try (FileInputStream fis = new FileInputStream(ncbiMappingFile.toFile());
             GZIPInputStream gis = new GZIPInputStream(fis);
             CountingInputStream cis = new CountingInputStream(gis);
             BufferedReader br = new BufferedReader(new InputStreamReader(cis))) {
            String line = br.readLine();
            ArrayList<String> header = new ArrayList<>(List.of(line.toLowerCase().split("\t")));
            int accessionCol = header.contains("accession") ? header.indexOf("accession") : header.indexOf("accession.version");
            int taxIdCol = header.indexOf("taxid");
            long i = 0;
            while ((line = br.readLine()) != null) {
                String[] values = line.split("\t");
                String accession = removeVersion(values[accessionCol]);
                int taxId = Integer.parseInt(values[taxIdCol]);
                // only add accessions, if the tax_id is in the tree and the accession is needed
                if (accessionMap.containsKey(accession) && tree.idMap.containsKey(taxId)) {
                    if (accessionMap.get(accession) == -1) {
                        accessionMap.put(accession, taxId);
                    } else {
                        accessionMap.put(accession, tree.findLCA(taxId, accessionMap.get(accession)));
                    }
                }
                progressBar.setProgress((long)(cis.getBytesRead()/6.5));
                progressLogger.setProgress(++i);
            }
            progressBar.finish();
        } catch (IOException | ArrayIndexOutOfBoundsException e) {
            System.err.println("Error reading accession mapping file: " + ncbiMappingFile);
            throw new RuntimeException(e);
        }
    }
}
