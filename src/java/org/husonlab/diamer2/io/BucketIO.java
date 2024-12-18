package org.husonlab.diamer2.io;

import org.husonlab.diamer2.indexing.Bucket;

import java.io.*;

public class BucketIO {
    private final File file;
    private final int name;

    public BucketIO(File file, int name) {
        this.file = file;
        this.name = name;
    }

    public Bucket read() throws IOException {
        long[] content;
        try (FileInputStream fis = new FileInputStream(file);
             DataInputStream dis = new DataInputStream(fis)) {
            int length = dis.readInt();
            content = new long[length];
            for (int i = 0; i < length; i++) {
                content[i] = dis.readLong();
            }
        }
        return new Bucket(name, content);
    }

    public void write(Bucket bucket) throws IOException {
        long[] content = bucket.getContent();
        try (FileOutputStream fos = new FileOutputStream(file);
             DataOutputStream dos = new DataOutputStream(fos)) {
            dos.writeInt(content.length);
            for (long l : content) {
                dos.writeLong(l);
            }
        }
    }

    public boolean exists() {
        return file.exists();
    }

    public BucketReader getBucketReader() {
        return new BucketReader(file);
    }

    public static class BucketReader implements AutoCloseable {

        private final FileInputStream fis;
        private final DataInputStream dis;
        private final int length;
        private int position = 0;

        public BucketReader(File file) {
            try {
                fis = new FileInputStream(file);
                dis = new DataInputStream(fis);
            } catch (Exception e) {
                throw new RuntimeException("Could not open bucket file " + file.getName(), e);
            }
            try {
                length = dis.readInt();
            } catch (Exception e) {
                throw new RuntimeException("Could not read length of bucket file " + file.getName(), e);
            }
        }

        public boolean hasNext() {
            return position < length;
        }

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

        public int getLength() {
            return length;
        }
    }
}
