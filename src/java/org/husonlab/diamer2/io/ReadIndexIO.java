package org.husonlab.diamer2.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.HashMap;

public class ReadIndexIO extends IndexIO {

    private final File readHeaderMappingFile;

    /**
     * Create a new DBIndexIO object.
     * @param indexFolder path to the index folder
     * @throws FileNotFoundException if the index folder or the read header mapping file (in case of a READS index)
     *                               is missing
     */
    public ReadIndexIO(Path indexFolder) throws FileNotFoundException {
        super(indexFolder);
        readHeaderMappingFile = indexFolder.resolve("header_index.txt").toFile();
    }

    /**
     * Checks if the read header mapping file is available.
     */
    public boolean existsReadHeaderMapping() {
        return readHeaderMappingFile.exists();
    }

    public HashMap<Integer, String> getReadHeaderMapping() {
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
            throw new RuntimeException("Could not read read header mapping file: " + readHeaderMappingFile, e);
        }
        return readHeaderMapping;
    }
}
