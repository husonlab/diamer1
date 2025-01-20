package org.husonlab.diamer2.io.accessionMapping;

import org.husonlab.diamer2.io.CountingInputStream;
import org.husonlab.diamer2.taxonomy.Tree;
import org.husonlab.diamer2.util.logging.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class NCBIMapping extends AccessionMapping {

    private final Logger logger;
    private final HashMap<String, Integer> accessionMap;
    private final Tree tree;
    private final HashSet<String> neededAccessions;

    public NCBIMapping(NCBIMappingFile[] NCBIMappingFiles, Tree tree, HashSet<String> neededAccessions) {
        logger = new Logger("NCBIMapping");
        logger.addElement(new Time());
        accessionMap = new HashMap<>();
        this.tree = tree;
        this.neededAccessions = neededAccessions;

        for (NCBIMappingFile ncbiMappingFile : NCBIMappingFiles) {
            logger.logInfo("Reading accession taxID mapping from: " + ncbiMappingFile.mappingFile);
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

    public void readAccessionMap(NCBIMappingFile ncbiMappingFile) {
        ProgressBar progressBar = new ProgressBar(ncbiMappingFile.mappingFile.length(), 20);
        ProgressLogger progressLogger = new ProgressLogger("Accessions");
        new OneLineLogger("NCBIMapping", 500)
                .addElement(new RunningTime())
                .addElement(progressBar)
                .addElement(progressLogger);

        try (FileInputStream fis = new FileInputStream(ncbiMappingFile.mappingFile);
             GZIPInputStream gis = new GZIPInputStream(fis);
             CountingInputStream cis = new CountingInputStream(gis);
             BufferedReader br = new BufferedReader(new InputStreamReader(cis))) {
            int accessionCol = ncbiMappingFile.accessionCol();
            int taxIdCol = ncbiMappingFile.taxIdCol();
            String line;
            br.readLine(); // skip header
            long i = 0;
            while ((line = br.readLine()) != null) {
                String[] values = line.split("\t");
                String accession = removeVersion(values[accessionCol]);
                int taxId = Integer.parseInt(values[taxIdCol]);
                // only add accessions, if the tax_id is in the tree and the accession is needed
                if (neededAccessions.contains(accession) && tree.idMap.containsKey(taxId)) {
                    accessionMap.computeIfPresent(accession, (key, value) -> {
                        if (taxId == value) {
                            return value;
                        }
                        return tree.findLCA(taxId, value);
                    });
                    accessionMap.computeIfAbsent(accession, key -> taxId);
                }
                progressBar.setProgress(cis.getBytesRead()/6);
                progressLogger.setProgress(++i);
            }
            progressBar.finish();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public record NCBIMappingFile(File mappingFile, int accessionCol, int taxIdCol) {}
}
