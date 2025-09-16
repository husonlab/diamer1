package org.husonlab.diamer.io.accessionMapping;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class to handle the interaction with files containing a mapping of sequence accessions to taxonomic IDs.
 */
public abstract class AccessionMapping {
    /**
     * Get the taxonomic ID for a sequence accession.
     * @param accession Sequence accession.
     * @return The corresponding taxonomic ID or -1 if the accession is not in the mapping.
     */
    abstract public int getTaxId(String accession);
    /**
     * Get the taxonomic IDs for a list of sequence accessions.
     * <p>
     *     It makes sense to use this method if the mapping file is faster for batch queries (e.g. a database).
     * </p>
     * @param accessions List of sequence accessions.
     * @return List of taxonomic IDs in the same order as the input list.
     */
    abstract public ArrayList<Integer> getTaxIds(List<String> accessions);

    /**
     * Utility method to remove the version from a sequence accession.
     * @param accession Sequence accession.
     * @return Sequence accession without the version.
     */
    public static String removeVersion(String accession) {
        return accession.split("\\.")[0];
    }
}
