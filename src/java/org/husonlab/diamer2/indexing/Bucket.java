package org.husonlab.diamer2.indexing;

import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

public class Bucket {
    private final int name;
    @Nullable
    private long[] content;

    public Bucket(int name) {
        this.name = name;
        this.content = null;
    }

    /**
     * Creates a bucket with the provided content.
     * @param name Name (number) of the bucket.
     * @param bucket Content of the bucket.
     */
    public Bucket(int name, @Nullable long[] bucket) {
        this.name = name;
        this.content = bucket;
    }

    /**
     * ReadAssignment a bucket from a binary file.
     * @param file File to read the bucket from.
     */
    public Bucket(File file) throws IOException {
        this.name = Integer.parseInt(file.getName().split("\\.")[0]);
        try (FileInputStream fis = new FileInputStream(file);
             DataInputStream dis = new DataInputStream(fis)) {
            int length = dis.readInt();
            content = new long[length];
            for (int i = 0; i < length; i++) {
                content[i] = dis.readLong();
            }
        }
    }

    /**
     * Sorts the content of the bucket.
     * @throws NullPointerException If the bucket is empty.
     */
    public void sort() {
        if (content != null) {
            content = Sorting.radixSort44bits(content);
        } else {
            throw new NullPointerException("Bucket is empty and can not be sorted.");
        }
    }

    /**
     * Writes the content of the bucket to a binary file.
     * @param path path to write the bucket to.
     * @throws NullPointerException If the bucket is empty.
     */
    public void writeToFile(Path path) throws IOException {
        File file = path.resolve(name + ".bin").toFile();
        if (content != null) {
            System.out.println("[Bucket " + name + "] Started writing ...");
            try (FileOutputStream fos = new FileOutputStream(file);
                 DataOutputStream dos = new DataOutputStream(fos)) {
                dos.writeInt(content.length);
                for (long l : content) {
                    dos.writeLong(l);
                }
            }
            System.out.println("[Bucket " + name + "] Finished writing.");
        } else {
            throw new NullPointerException("Bucket is empty and can not be written to file.");
        }
    }

    public static ArrayList<Bucket> readBuckets(Path path) {
        return readBucketRange(path, 0, 1024);
    }

    public static ArrayList<Bucket> readBucketRange(Path path, int start, int end) {
        ArrayList<Bucket> buckets = new ArrayList<>();
        for (int i = start; i < end; i++) {
            File file = path.resolve(i + ".bin").toFile();
            if (file.exists()) {
                try {
                    buckets.add(new Bucket(file));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.err.println("Bucket not found: " + i);
            }
        }
        return buckets;
    }

    /**
     * Sets the content of the bucket.
     * @param content Content of the bucket.
     */
    public void setContent(@Nullable long[] content) {
        this.content = content;
    }

    /**
     * Returns the content of the bucket.
     * @return Content of the bucket.
     */
    @Nullable
    public long[] getContent() {
        return content;
    }

    public int getName() {
        return name;
    }
}
