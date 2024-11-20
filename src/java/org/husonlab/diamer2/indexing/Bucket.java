package org.husonlab.diamer2.indexing;

import org.jetbrains.annotations.Nullable;

import java.io.*;

public class Bucket {
    @Nullable
    private long[] content;

    public Bucket() {
        this.content = null;
    }

    /**
     * Creates a bucket with the provided content.
     * @param bucket Content of the bucket.
     */
    public Bucket(@Nullable long[] bucket) {
        this.content = bucket;
    }

    /**
     * Reads a bucket from a binary file.
     * @param file File to read the bucket from.
     */
    public Bucket(File file) throws IOException {
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
     * @param file File to write the bucket to.
     * @throws NullPointerException If the bucket is empty.
     */
    public void writeToFile(File file) throws IOException {
        if (content != null) {
            try (FileOutputStream fos = new FileOutputStream(file);
                 DataOutputStream dos = new DataOutputStream(fos)) {
                dos.writeInt(content.length);
                for (long l : content) {
                    dos.writeLong(l);
                }
            }
        } else {
            throw new NullPointerException("Bucket is empty and can not be written to file.");
        }
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
}
