package org.husonlab.diamer2.io.indexing;

import org.husonlab.diamer2.io.Utilities;

import java.io.*;
import java.nio.file.Path;
import java.util.List;

public class ReadIndexIO extends IndexIO {

    private final Path readHeaderMappingFile;

    /**
     * Create a new IndexIO object.
     * @param indexFolder path to the index folder
     */
    public ReadIndexIO(Path indexFolder) {
        super(indexFolder);
        readHeaderMappingFile = indexFolder.resolve("header_index.txt");
    }

    public boolean readHeaderMappingExists() {
        return readHeaderMappingFile.toFile().exists();
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

    public void writeReadHeaderMapping(List<String> readHeaderMapping) {
        try {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(readHeaderMappingFile.toString()))) {
                writer.write(readHeaderMapping.size() + "\n");
                int id = 0;
                for (String header : readHeaderMapping) {
                    writer.write(id++ + "\t" + header + "\n");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not write read header mapping file: " + readHeaderMappingFile, e);
        }
    }
}
