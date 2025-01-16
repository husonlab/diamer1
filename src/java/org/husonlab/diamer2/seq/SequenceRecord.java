package org.husonlab.diamer2.seq;

import org.husonlab.diamer2.seq.alphabet.Alphabet;
import org.husonlab.diamer2.seq.alphabet.AlphabetAA;
import org.husonlab.diamer2.seq.alphabet.AlphabetDNA;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

/**
 * Represents a sequence with a header and a sequence string.
 */
public class SequenceRecord<T> implements Iterable<T> {

    private String header;
    private Sequence<T> sequence;

    /**
     * Create a new Sequence object.
     * @param header the header of the sequence
     * @param sequence the sequence of the sequence
     */
    public SequenceRecord(String header, Sequence<T> sequence) {
        this.header = header;
        this.sequence = sequence;
    }

    /**
     * Get the header of the sequence.
     * @return the header of the sequence
     */
    public String getHeader() {
        return header;
    }

    /**
     * Get the sequence.
     * @return the sequence
     */
    public Sequence<T> getSequence() {
        return sequence;
    }

    /**
     * Get the alphabet of the sequence.
     * @return the alphabet of the sequence
     */
    public Alphabet<T> getAlphabet() {
        return sequence.getAlphabet();
    }

    public String getSequenceString() {
        return sequence.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) return false;
        SequenceRecord<?> seq = (SequenceRecord<?>) obj;
        return header.equals(seq.header) && sequence.equals(seq.sequence);
    }

    @Override
    public String toString() {
        return ">%s\n%s".formatted(header, sequence);
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return sequence.iterator();
    }

    public static SequenceRecord<Character> DNA(String header, String sequence) {
        return new SequenceRecord<>(header, new CharSequence(new AlphabetDNA(), sequence));
    }

    public static SequenceRecord<Character> AA(String header, String sequence) {
        return new SequenceRecord<>(header, new CharSequence(new AlphabetAA(), sequence));
    }
}
