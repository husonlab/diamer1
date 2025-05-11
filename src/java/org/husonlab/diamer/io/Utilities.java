package org.husonlab.diamer.io;

import org.husonlab.diamer.util.logging.Logger;

import java.io.*;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class Utilities {

    /**
     * Approximates the number of sequences in a file.
     * <p>
     *     In case of an unzipped file, the method reads a random sample of sequences from the file and calculates the
     *     average number of bytes per sequence. The number of sequences is then approximated by dividing the file size
     *     by the average number of bytes per sequence. In case of a gzipped file, the method reads the first 5000
     *     sequences from the file, which might not be a representative sample.
     * </p>
     * @param file File to approximate the number of sequences in
     * @param delimiter Delimiter that separates sequences ('\n>' for fasta, '\n@' for fastq)
     * @return Approximated number of sequences in the file
     */
    public static int approximateNumberOfSequences(Path file, String delimiter) {
        Logger logger = new Logger("Utilities");
        long averageBytesPerSequence = file.toFile().getName().endsWith(".gz") ?
                getAverageBytesPerSequenceGZIP(file, delimiter) : getAverageBytesPerSequence(file, delimiter);
        long fileSize = file.toFile().length();
        int result = (int) (fileSize / averageBytesPerSequence);
        logger.logInfo("Approximated number of sequences in file " + file + ": " + result);
        return result;
    }

    /**
     * Approximates the average number of bytes per sequence in a file by reading a random sample of sequences.
     * @param file File to approximate the average number of bytes per sequence in
     * @param delimiter Delimiter that separates sequences ('\n>' for fasta, '\n@' for fastq)
     * @return Approximated average number of bytes per sequence in the file
     */
    private static long getAverageBytesPerSequence(Path file, String delimiter) {
        // The number of intended samples can be different from the actual number of samples taken in the end
        // because a random sampling that starts within the last sequence will not yield a valid sample.
        int numberOfIntendedSamples = 5000;
        int numberOfSamples = 0;
        int bufferLength = 1024;
        try (RandomAccessFile raf = new RandomAccessFile(file.toString(), "r")) {
            long fileSize = raf.length();
            long totalBytes = 0;
            byte[] buffer = new byte[bufferLength];
            Pattern pattern = Pattern.compile(delimiter);
            for (int i = 0; i < numberOfIntendedSamples; i++) {
                long randomPosition = (long) (Math.random() * fileSize);
                long firstDelimiter = -1;
                long lastDelimiter = -1;
                for (int j = 0; j < 1000; j++) {
                    raf.seek(randomPosition + j * bufferLength);
                    int bytesRead = raf.read(buffer);
                    if (bytesRead == -1) {
                        break;
                    }
                    String chunk = new String(buffer, 0, bytesRead);
                    Matcher matcher = pattern.matcher(chunk);
                    if (matcher.find()) {
                        firstDelimiter = randomPosition + j * bufferLength + chunk.substring(0, matcher.start()).getBytes().length;
                        randomPosition = firstDelimiter + 4;
                        break;
                    }
                }
                for (int j = 0; j < 1000; j++) {
                    raf.seek(randomPosition + j * bufferLength);
                    int bytesRead = raf.read(buffer);
                    if (bytesRead == -1) {
                        break;
                    }
                    String chunk = new String(buffer, 0, bytesRead);
                    Matcher matcher = pattern.matcher(chunk);
                    if (matcher.find()) {
                        lastDelimiter = randomPosition + j * bufferLength + chunk.substring(0, matcher.start()).getBytes().length;
                        break;
                    }
                }
                if (firstDelimiter != -1 && lastDelimiter != -1) {
                    totalBytes += lastDelimiter - firstDelimiter;
                    numberOfSamples++;
                }
            }
            return totalBytes / numberOfSamples;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Approximates the average number of bytes per sequence in a gzipped file by reading the first 5000 sequences.
     * @param file File to approximate the average number of bytes per sequence in
     * @param delimiter Delimiter that separates sequences ('\n>' for fasta, '\n@' for fastq)
     * @return Approximated average number of bytes per sequence in the file
     */
    private static long getAverageBytesPerSequenceGZIP(Path file, String delimiter) {
        int numberOfIntendedSamples = 5000;
        int numberOfSamples = 0;
        long totalBytes = 0;
        try (CountingInputStream cis = new CountingInputStream(new FileInputStream(file.toString()));
             InputStreamReader isr = new InputStreamReader(new GZIPInputStream(cis))) {
            int c;
            StringBuilder sb = new StringBuilder();
            Pattern pattern = Pattern.compile(delimiter);
            while (numberOfSamples <= numberOfIntendedSamples + 1 && (c = isr.read()) != -1) {
                sb.append((char) c);
                Matcher matcher = pattern.matcher(sb);
                if (matcher.find()) {
                    numberOfSamples++;
                    sb.delete(0, matcher.end());
                }
            }
            totalBytes = cis.getBytesRead();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return numberOfSamples == 0 ? 0 : totalBytes / numberOfSamples;
    }

    /**
     * Get a folder path from a string, check if it exists or not and create folders in the path if necessary.
     * @param path path to folder
     * @param exists whether the folder should exist or not
     * @return absolute path to the folder
     */
    public static Path getFolder(String path, boolean exists) {
        Path result = null;
        try {
            result = Path.of(path).toAbsolutePath();
            if (exists && !result.toFile().exists()) {
                System.err.printf("Folder \"%s\" does not exist\n", path);
                System.exit(1);
            } else if (exists && !result.toFile().isDirectory()) {
                System.err.printf("Path \"%s\" is not a directory\n", path);
                System.exit(1);
            } else if (!exists && result.getParent() != null) {
                result.toFile().mkdirs();
                if (!result.getParent().toFile().exists() || !result.getParent().toFile().isDirectory()) {
                    System.err.printf("Could not create directory \"%s\"\n", result.getParent());
                    System.exit(1);
                }
            }
        } catch (InvalidPathException e) {
            System.err.printf("Invalid path: \"%s\"\n", path);
            System.exit(1);
        }
        return result;
    }

    /**
     * Get a file path from a string, check if it exists or not and create folders in the path if necessary.
     * @param path path to file
     * @param exists whether the file should exist or not
     * @return absolute path to the file
     */
    public static Path getFile(String path, boolean exists) {
        Path result = null;
        try {
            result = Path.of(path).toAbsolutePath();
            if (exists && !result.toFile().exists()) {
                System.err.printf("File \"%s\" does not exist\n", path);
                System.exit(1);
            } else if (exists && result.toFile().isDirectory()) {
                System.err.printf("Path \"%s\" is a directory\n", path);
                System.exit(1);
            } else if (!exists && result.getParent() != null) {
                result.getParent().toFile().mkdirs();
                if (!result.getParent().toFile().exists() || !result.getParent().toFile().isDirectory()) {
                    System.err.printf("Could not create directory \"%s\"\n", result.getParent());
                    System.exit(1);
                }
            }
        } catch (InvalidPathException e) {
            System.err.printf("Invalid path: \"%s\"\n", path);
            System.exit(1);
        }
        return result;
    }
}
