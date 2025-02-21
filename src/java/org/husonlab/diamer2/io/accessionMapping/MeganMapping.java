package org.husonlab.diamer2.io.accessionMapping;

import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Class to handle the SQLite database used by MEGAN.
 */
public class MeganMapping extends AccessionMapping {

    private final Connection c;

    /**
     * Creates a new MeganMapping object and connects to the SQLite database.
     * @param dbFile Path to the SQLite database file.
     */
    public MeganMapping(Path dbFile) {
        try{
            c = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
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
        // Create a prepared statement with a variable number of placeholders for all the accessions.
        try (PreparedStatement statement = c.prepareStatement(
                "SELECT Accession, Taxonomy FROM mappings WHERE Accession in (" + ",?"
                        .repeat(accessions.size()).replaceFirst(",", "") + ");")) {
            for (int i = 0; i < accessions.size(); i++) {
                statement.setString(i + 1, removeVersion(accessions.get(i)));
            }
            ResultSet result = statement.executeQuery();

            // map the result back to the accessions
            HashMap<String, Integer> map = new HashMap<>(accessions.size());
            if (!result.isBeforeFirst()) {
                return new ArrayList<>();
            }
            do {
                map.put(result.getString(1), result.getInt(2));
            } while (result.next());

            // convert the hashmap back to an arrayList in the same order as the input
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
