package org.husonlab.diamer2.io;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utilities {
    public static void checkFilesAndFolders(File[] files, Path[] paths) {

        ArrayList<File> existingFiles = new ArrayList<>();
        ArrayList<Path> existingFolders = new ArrayList<>();

        for (File file : files) {
            try {
                Files.createDirectories(file.toPath().getParent());
            } catch (FileAlreadyExistsException e) {
                throw new RuntimeException("The path contains a file with the same name as the directory to create: " + file.toPath().getParent());
            } catch (IOException e) {
                throw new RuntimeException("Could not create directory: " + file.toPath().getParent(), e);
            }
            if (file.isDirectory()) {
                throw new RuntimeException("The path is a directory: " + file);
            }
            if (file.isFile()) {
                existingFiles.add(file);
            }
        }

        for (Path path : paths) {
            if (Files.isDirectory(path)) {
                existingFolders.add(path);
            }
            try {
                Files.createDirectories(path);
            } catch (FileAlreadyExistsException e) {
                throw new RuntimeException("The path contains a file with the same name as the directory to create: " + path.getParent());
            } catch (IOException e) {
                throw new RuntimeException("Could not create directory: " + path.getParent(), e);
            }
        }

        if (!existingFiles.isEmpty() || !existingFolders.isEmpty()) {
            for (int i = 0; i < 10; i++) {
                System.out.println("The following files and folders already exist:");
                for (File file : existingFiles) {
                    System.out.println(file);
                }
                for (Path path : existingFolders) {
                    System.out.println(path);
                }
                System.out.println("Do you want to overwrite them? (Y/N)");
                try {
                    int c = System.in.read();
                    if (c == 'y' || c == 'Y') {
                        break;
                    } else if (c == 'n' || c == 'N') {
                        System.exit(1);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

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

    public static void createPath(File file) {
        createPath(Path.of(file.getParent()));
    }

    public static int approximateNumberOfSequences(File file, String delimiter) {
        int numberOfIntendedSamples = 5000;
        int numberOfSamples = 0;
        int bufferLength = 1024;
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
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
