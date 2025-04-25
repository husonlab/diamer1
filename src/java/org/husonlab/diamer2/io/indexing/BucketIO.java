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
        try (BucketReader reader = getBucketReader()) {
            int length = reader.getLength();
            content = new long[length];
            for (int i = 0; i < length; i++) {
                content[i] = reader.next();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new Bucket(name, content);
    }

    /**
     * Writes the content of the bucket to the file.
     * @param bucket the bucket to write
     */
    public void write(Bucket bucket) throws IOException {
        long[] content = bucket.getContent();
        try (BucketWriter writer = getBucketWriter()) {
            for (long l : content) {
                writer.write(l);
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
        private final int length;
        private int position = 0;
        private long previous;
        byte[] buffer = new byte[65_536];
        private int bufferPosition = 0;
        private int bufferLength = 0;

        public BucketReader(Path file) {
            try {
                fis = new FileInputStream(file.toString());
            } catch (Exception e) {
                throw new RuntimeException("Could not open bucket file " + file.toFile().getName(), e);
            }
            try {
                byte[] lengthBytes = fis.readNBytes(4);
                length = ((lengthBytes[0] & 0xFF) << 24) | ((lengthBytes[1] & 0xFF) << 16) |
                        ((lengthBytes[2] & 0xFF) << 8) | (lengthBytes[3] & 0xFF);
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
            position++;
            previous += readNextLong();
            return previous;
        }

        public long readNextLong() {
            long l = 0;
            int i = 0;
            byte b;
            do {
                b = getNextByte();
                l |= ((long) (b & 0b01111111)) << (i * 7);
                i++;
            } while (b < 0);
            return l;
        }

        private byte getNextByte() {
            if (bufferPosition >= bufferLength) {
                fillBuffer();
            }
            return buffer[bufferPosition++];
        }

        private void fillBuffer() {
            try {
                bufferPosition = 0;
                bufferLength = fis.read(buffer);
                if (bufferLength == -1) {
                    throw new EOFException();
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not read from bucket file", e);
            }
        }

        @Override
        public void close() throws Exception {
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
        private long previous;

        public BucketWriter(Path file) {
            this.file = file;
            try {
                fos = new FileOutputStream(file.toString());
                dos = new DataOutputStream(new BufferedOutputStream(fos, 65_536));
                dos.writeInt(0);
            } catch (Exception e) {
                throw new RuntimeException("Could not open bucket file " + file.toFile().getName(), e);
            }
        }

        public void write(long l) {
            try {
                l -= previous;
                previous += l;
                writeLong(l);
                length++;
            } catch (Exception e) {
                throw new RuntimeException("Could not write long to bucket file", e);
            }
        }

        private void writeLong(long l) {
            try {
                while ((l & ~0b01111111) != 0) {
                    dos.writeByte((byte) ((l & 0b01111111) | 0b10000000));
                    l >>>= 7;
                }
                dos.writeByte((byte) l);
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
