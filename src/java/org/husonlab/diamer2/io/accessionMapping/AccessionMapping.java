package org.husonlab.diamer2.io.accessionMapping;

public abstract class AccessionMapping {
    abstract public int getTaxId(String accession);
    public String removeVersion(String accession) {
        return accession.split("\\.")[0];
    }
}
