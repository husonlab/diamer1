package org.husonlab.diamer2.io.indexing;

import org.husonlab.diamer2.indexing.Bucket;

import java.io.*;
import java.nio.file.Path;

/**
 * Class to handle IO operations on bucket files / objects.
 */
public class BucketIO {
    private final Path file;
    private final int name;

    public BucketIO(Path file, int name) {
        this.file = file;
        this.name = name;
    }

    /**
     * Reads the content of the bucket from the file.
     * @return a new {@link Bucket} object with the content of the file
     */
    public Bucket read() throws IOException {
        if (!file.toFile().exists()) {
            throw new FileNotFoundException("Tried to read non-existing bucket file: " + file.toFile().getName());
        }
        long[] content;
        try (FileInputStream fis = new FileInputStream(file.toString());
             DataInputStream dis = new DataInputStream(fis)) {
            int length = dis.readInt();
            content = new long[length];
            for (int i = 0; i < length; i++) {
                content[i] = dis.readLong();
            }
        }
        return new Bucket(name, content);
    }

    /**
     * Writes the content of the bucket to the file.
     * @param bucket the bucket to write
     */
    public void write(Bucket bucket) throws IOException {
        long[] content = bucket.getContent();
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file.toString())))) {
            dos.writeInt(content.length);
            for (long l : content) {
                dos.writeLong(l);
            }
        }
    }

    public int getName() {
        return name;
    }

    /**
     * Checks if the bucket file exists.
     */
    public boolean exists() {
        return file.toFile().exists();
    }

    /**
     * Returns a new {@link BucketReader} object for reading the content of the bucket.
     */
    public BucketReader getBucketReader() {
        return new BucketReader(file);
    }

    /**
     * Returns a new {@link BucketWriter} object for writing the content of the bucket vale by value.
     */
    public BucketWriter getBucketWriter() {
        return new BucketWriter(file);
    }

    /**
     * Reader to read a bucket file long by long.
     */
    public static class BucketReader implements AutoCloseable {

        private final FileInputStream fis;
        private final DataInputStream dis;
        private final int length;
        private int position = 0;

        public BucketReader(Path file) {
            try {
                fis = new FileInputStream(file.toString());
                dis = new DataInputStream(new BufferedInputStream(fis, 65_536));
            } catch (Exception e) {
                throw new RuntimeException("Could not open bucket file " + file.toFile().getName(), e);
            }
            try {
                length = dis.readInt();
            } catch (Exception e) {
                throw new RuntimeException("Could not read length of bucket file " + file.toFile().getName(), e);
            }
        }

        public boolean hasNext() {
            return position < length;
        }

        /**
         * Reads the next long from the bucket file.
         * @return the next long
         * @throws RuntimeException if the end of the bucket is reached
         */
        public long next() {
            if (position >= length) {
                throw new RuntimeException("BucketReader: end of bucket reached");
            }
            try {
                position++;
                return dis.readLong();
            } catch (Exception e) {
                throw new RuntimeException("Could not read long from bucket file", e);
            }
        }

        @Override
        public void close() throws Exception {
            dis.close();
            fis.close();
        }

        /**
         * @return the number of longs in the bucket.
         */
        public int getLength() {
            return length;
        }
    }

    public static class BucketWriter implements AutoCloseable {
        private final Path file;
        private final FileOutputStream fos;
        private final DataOutputStream dos;
        private int length = 0;

        public BucketWriter(Path file) {
            this.file = file;
            try {
                fos = new FileOutputStream(file.toString());
                dos = new DataOutputStream(new BufferedOutputStream(fos));
                dos.writeInt(0);
            } catch (Exception e) {
                throw new RuntimeException("Could not open bucket file " + file.toFile().getName(), e);
            }
        }

        public void write(long l) {
            try {
                dos.writeLong(l);
                length++;
            } catch (Exception e) {
                throw new RuntimeException("Could not write long to bucket file", e);
            }
        }

        @Override
        public void close() {
            try {
                dos.flush();
                dos.close();
                fos.close();
                try (RandomAccessFile raf = new RandomAccessFile(file.toString(), "rw")) {
                    raf.writeInt(length);
                }
            } catch (Exception e) {
                throw new RuntimeException("Could not close bucket file", e);
            }
        }

        public int getLength() {
            return length;
        }
    }
}
