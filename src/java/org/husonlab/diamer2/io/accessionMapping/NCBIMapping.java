package org.husonlab.diamer2.io.accessionMapping;

import org.husonlab.diamer2.io.CountingInputStream;
import org.husonlab.diamer2.taxonomy.Tree;
import org.husonlab.diamer2.util.logging.*;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Class to handle NCBI taxmapping files (accession2taxid).
 */
public class NCBIMapping extends AccessionMapping {

    private final Logger logger;
    /**
     * Whether a neededAccessions HashMap was provided to limit the size of the resulting HashMap.
     */
    private final boolean filterAccessions;
    private final HashMap<String, Integer> accessionMap;
    private final Tree tree;

    /**
     * Constructor for NCBIMapping.
     * @param ncbiMappingFiles List of paths to the gzipped NCBI mapping files.
     * @param tree A taxonomic tree to check if the taxIDs are in the tree and to find the LCA in the unlikely case that
     *             the same accession maps to different taxIDs.
     * @param neededAccessions A HashMap that already contains the accessions that are needed as keys to limit the size
     *                         of the resulting HashMap (the mapping files can be quite large).
     */
    public NCBIMapping(Iterable<Path> ncbiMappingFiles, Tree tree, @Nullable HashMap<String, Integer> neededAccessions) {
        logger = new Logger("NCBIMapping");
        logger.addElement(new Time());
        if (neededAccessions == null) {
            accessionMap = new HashMap<>();
            filterAccessions = false;
        } else {
            accessionMap = neededAccessions;
            filterAccessions = true;
        }
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

    /**
     * Reads the accessions from one gzipped mapping file and adds them to the HashMap.
     * <p>
     *     The file must be tab separated and have the following columns: "accession"/"accession.version", "taxid".
     * </p>
     * @param ncbiMappingFile Path to the NCBI mapping file.
     */
    private void readAccessionMap(Path ncbiMappingFile) {
        ProgressBar progressBar = new ProgressBar(ncbiMappingFile.toFile().length(), 20);
        ProgressLogger progressLogger = new ProgressLogger("Accessions");
        new OneLineLogger("NCBIMapping", 500)
                .addElement(new RunningTime())
                .addElement(progressBar)
                .addElement(progressLogger);

        try (FileInputStream fis = new FileInputStream(ncbiMappingFile.toFile());
             CountingInputStream cis = new CountingInputStream(fis);
             GZIPInputStream gis = new GZIPInputStream(cis);
             BufferedReader br = new BufferedReader(new InputStreamReader(gis))) {
            String line = br.readLine();
            ArrayList<String> header = new ArrayList<>(List.of(line.toLowerCase().split("\t")));
            int accessionCol = header.contains("accession") ? header.indexOf("accession") : header.indexOf("accession.version");
            int taxIdCol = header.indexOf("taxid");
            if (accessionCol == -1 || taxIdCol == -1) {
                throw new RuntimeException("NCBI mapping file does not contain the necessary columns (accession/accession.version, taxid).");
            }
            long i = 0;
            while ((line = br.readLine()) != null) {
                String[] values = line.split("\t");
                String accession = removeVersion(values[accessionCol]);
                int taxId = Integer.parseInt(values[taxIdCol]);
                // only add accessions if the tax_id is in the tree, and the accession is needed
                if (tree.idMap.containsKey(taxId) && (!filterAccessions || accessionMap.containsKey(accession))) {
                    if (!accessionMap.containsKey(accession) || accessionMap.get(accession) == -1) {
                        accessionMap.put(accession, taxId);
                    } else {
                        accessionMap.put(accession, tree.findLCA(taxId, accessionMap.get(accession)));
                    }
                }
                progressBar.setProgress(cis.getBytesRead());
                progressLogger.setProgress(++i);
            }
            progressBar.finish();
        } catch (IOException | ArrayIndexOutOfBoundsException e) {
            System.err.println("Error reading accession mapping file: " + ncbiMappingFile);
            throw new RuntimeException(e);
        }
    }
}
