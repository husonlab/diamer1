package org.husonlab.diamer2.seq;

/**
 * Represents a sequence with a header and a sequence string.
 */
public class Sequence {

    String header;
    String sequence;

    /**
     * Create a new Sequence object.
     * @param header the header of the sequence
     * @param sequence the sequence of the sequence
     */
    public Sequence(String header, String sequence) {
        this.header = header;
        this.sequence = sequence.replace("\n", "").replace(" ", "");
    }

    /**
     * Get the header of the sequence.
     * @return the header of the sequence
     */
    public String getHeader() {
        return header;
    }

    /**
     * Get the sequence string.
     * @return the sequence
     */
    public String getSequence() {
        return sequence;
    }

    /**
     * Get the reverse complement of the DNA sequence.
     * @return the reverse complement of the DNA sequence
     */
    public String getReverseComplement() {
        StringBuilder reverseComplement = new StringBuilder();
        for (int i = sequence.length() - 1; i >= 0; i--) {
            char c = sequence.charAt(i);
            switch (c) {
                case 'A' -> reverseComplement.append('T');
                case 'T' -> reverseComplement.append('A');
                case 'C' -> reverseComplement.append('G');
                case 'G' -> reverseComplement.append('C');
                default -> reverseComplement.append(c);
            }
        }
        return reverseComplement.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Sequence seq && seq.header.equals(header) && seq.sequence.equals(sequence);
    }

    @Override
    public String toString() {
        return ">%s\n%s".formatted(header, sequence);
    }

}
