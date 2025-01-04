package org.husonlab.diamer2.io;

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;

public class ReadIndexIO extends DBIndexIO {

    private final File readHeaderMappingFile;

    /**
     * Create a new DBIndexIO object.
     * @param indexFolder path to the index folder
     */
    public ReadIndexIO(Path indexFolder) {
        super(indexFolder);
        readHeaderMappingFile = indexFolder.resolve("header_index.txt").toFile();
    }

    /**
     * Checks if the read header mapping file is available.
     */
    public boolean existsReadHeaderMapping() {
        return readHeaderMappingFile.exists();
    }

    public String[] getReadHeaderMapping() {
        String[] readHeaderMapping;
        try (BufferedReader reader = new BufferedReader(new FileReader(indexFolder.resolve("header_index.txt").toFile()))) {
            int length = Integer.parseInt(reader.readLine());
            readHeaderMapping = new String[length];
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t");
                int readId = Integer.parseInt(parts[0]);
                String header = parts[1];
                readHeaderMapping[readId] = header;
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not read read header mapping file: " + readHeaderMappingFile, e);
        }
        return readHeaderMapping;
    }

    public void writeReadHeaderMapping(HashMap<Integer, String> readHeaderMapping) {
        try {
            try (PrintWriter writer = new PrintWriter(readHeaderMappingFile)) {
                writer.println(readHeaderMapping.size());
                for (int readId : readHeaderMapping.keySet()) {
                    writer.println(readId + "\t" + readHeaderMapping.get(readId));
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Could not write read header mapping file: " + readHeaderMappingFile, e);
        }
    }
}
