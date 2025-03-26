package org.husonlab.diamer2.io;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An input stream that counts the number of bytes read from it.
 */
public class CountingInputStream extends InputStream {

    private final InputStream inputStream;
    private long readBytes;

    public CountingInputStream(InputStream inputStream) {
        this.inputStream = new BufferedInputStream(inputStream, 131072);;
        this.readBytes = 0;
    }

    @Override
    public int read() throws IOException {
        int result = inputStream.read();
        if (result != -1) {
            readBytes++;
        }
        return result;
    }

    @Override
    public int read(@NotNull byte[] b, int off, int len) throws IOException {
        int result = inputStream.read(b, off, len);
        if (result != -1) {
            readBytes += result;
        }
        return result;
    }

    @NotNull
    @Override
    public byte[] readAllBytes() throws IOException {
        return super.readAllBytes();
    }

    /**
     * Returns the number of bytes read from this stream.
     * @return the number of bytes read from this stream
     */
    public long getBytesRead() {
        return readBytes;
    }
}
