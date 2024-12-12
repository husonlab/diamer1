package org.husonlab.diamer2.io;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;

public class BucketReader implements AutoCloseable {

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
