package org.husonlab.diamer.io.seq;

import java.util.LinkedList;

public interface HeaderToIdReader {
    /**
     * @return a list with all headers of the sequences that have been read so far. The index of the header in the list
     *         is the id that was given to the returned SequenceRecord.
     */
    LinkedList<String> getHeaders();

    /**
     * Method to clear the headers from memory.
     */
    void removeHeaders();
}
