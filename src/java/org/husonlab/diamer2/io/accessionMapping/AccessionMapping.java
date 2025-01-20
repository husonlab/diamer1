package org.husonlab.diamer2.io.accessionMapping;

import java.util.ArrayList;
import java.util.List;

public abstract class AccessionMapping {
    abstract public ArrayList<Integer> getTaxIds(List<String> accessions);
    abstract public int getTaxId(String accession);
    public static String removeVersion(String accession) {
        return accession.split("\\.")[0];
    }
}
