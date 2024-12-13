package org.husonlab.diamer2.io;

import org.husonlab.diamer2.readAssignment.ReadAssigner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.HashMap;

public class ReadIndexIO extends DBIndexIO {

    /**
     * Create a new DBIndexIO object.
     *
     * @param indexFolder path to the index folder
     * @throws FileNotFoundException if the index folder or the read header mapping file (in case of a READS index)
     *                               is missing
     */
    public ReadIndexIO(Path indexFolder) throws FileNotFoundException {
        super(indexFolder);
        checkReadHeaderMapping();
    }

    /**
     * Checks if the read header mapping file is available.
     * @throws FileNotFoundException if the read header mapping file is missing
     */
    private void checkReadHeaderMapping() throws FileNotFoundException {
        File readHeaderMapping = indexFolder.resolve("header_index.txt").toFile();
        if (!readHeaderMapping.exists()) {
            logger.logWarning("Read header mapping (header_index.txt) is missing in the index folder.");
            throw new FileNotFoundException("Read header mapping is missing.");
        }
    }

    public HashMap<Integer, String> getReadHeaderMapping() throws Exception {
        HashMap<Integer, String> readHeaderMapping;
        try (BufferedReader reader = new BufferedReader(new FileReader(indexFolder.resolve("header_index.txt").toFile()))) {
            int length = Integer.parseInt(reader.readLine());
            readHeaderMapping = new HashMap<>(length);
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t");
                int readId = Integer.parseInt(parts[0]);
                String header = parts[1];
                readHeaderMapping.put(readId, header);
            }
        } catch (Exception e) {
            logger.logError("Error reading read header mapping: " + e.getMessage());
            throw new Exception("Error reading read header mapping: " + e.getMessage());
        }
        return readHeaderMapping;
    }
}
