package org.husonlab.diamer2.io.accessionMapping;

import java.io.File;
import java.sql.*;

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
        try (PreparedStatement statement = c.prepareStatement("SELECT Taxonomy FROM mappings WHERE Accession = ?;");){
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
}
