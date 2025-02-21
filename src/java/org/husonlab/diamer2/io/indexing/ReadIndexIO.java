package org.husonlab.diamer2.io.indexing;

import java.io.*;
import java.nio.file.Path;
import java.util.List;

/**
 * Represents a read index folder with one binary file per bucket and a header mapping file that contains a mapping from
 * internal read IDs to read headers.
 */
public class ReadIndexIO extends IndexIO {

    private final Path readHeaderMappingFile;

    /**
     * Create a new IndexIO object.
     * @param indexFolder path to the index folder
     * @param nrOfBuckets number of buckets
     */
    public ReadIndexIO(Path indexFolder, int nrOfBuckets) {
        super(indexFolder, nrOfBuckets);
        readHeaderMappingFile = indexFolder.resolve("header_index.txt");
    }

    /**
     * Checks if a file containing the read header mapping exists in the index folder.
     */
    public boolean readHeaderMappingExists() {
        return readHeaderMappingFile.toFile().exists();
    }

    /**
     * Reads in the read header mapping file from the index.
     * <p>
     *     The file is expected to have the following format:
     * </p>
     * <p>
     *     {@code <number of reads>}
     * </p>
     * <p>
     *     {@code <read ID>\t<read header>}
     * </p>
     * <p>...</p>
     * @return an array of read headers where the index is the read ID
     */
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

    /**
     * Writes the read header mapping to the index folder.
     * @param readHeaderMapping a list of read headers where the index is the read ID
     */
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
