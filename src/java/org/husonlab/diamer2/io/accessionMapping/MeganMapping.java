package org.husonlab.diamer2.io.accessionMapping;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MeganMapping extends AccessionMapping {

    private Connection c;

    public MeganMapping(File dbFile) {
        try{
            c = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getTaxId(String accession) {
        try (PreparedStatement statement = c.prepareStatement("SELECT Taxonomy FROM mappings WHERE Accession = ?;")){
            statement.setString(1, removeVersion(accession));
            ResultSet result = statement.executeQuery();
            int taxId = result.getInt(1);
            if (taxId == 0) {
                return -1;
            }
            return taxId;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ArrayList<Integer> getTaxIds(List<String> accessions) {
        try (PreparedStatement statement = c.prepareStatement(
                "SELECT Accession, Taxonomy FROM mappings WHERE Accession in (" + "?".repeat(accessions.size()) + ");")) {
            for (int i = 0; i < accessions.size(); i++) {
                statement.setString(i + 1, removeVersion(accessions.get(i)));
            }
            ResultSet result = statement.executeQuery();
            HashMap<String, Integer> map = new HashMap<>(accessions.size());
            while (result.next()) {
                map.put(result.getString(1), result.getInt(2));
            }
            ArrayList<Integer> taxIds = new ArrayList<>(accessions.size());
            for (String accession : accessions) {
                taxIds.add(map.getOrDefault(removeVersion(accession), -1));
            }
            return taxIds;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
