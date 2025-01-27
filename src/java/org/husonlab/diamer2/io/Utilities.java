package org.husonlab.diamer2.io;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utilities {

    public static Path createPath(Path path) {
        try {
            Files.createDirectories(path);
        } catch (FileAlreadyExistsException e) {
            throw new RuntimeException("The path contains a file with the same name as the directory to create: " + path.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Could not create directory: " + path.getParent(), e);
        }
        return path;
    }

    /**
     * Approximates the number of sequences in a file by randomly sampling sequences in the file and calculating the
     * average number of bytes required for one sequence.
     * @param file File to approximate the number of sequences in
     * @param delimiter Delimiter that separates sequences ('\n>' for fasta, '\n@' for fastq)
     * @return Approximated number of sequences in the file
     */
    public static int approximateNumberOfSequences(Path file, String delimiter) {
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
            double averageBytes = (double) totalBytes / numberOfSamples;
            if (numberOfSamples == 0) {
                return 0;
            }
            return (int) (fileSize / averageBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
