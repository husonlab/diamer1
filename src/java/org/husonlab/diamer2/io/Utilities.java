package org.husonlab.diamer2.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

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
}
