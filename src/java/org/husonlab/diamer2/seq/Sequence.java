package org.husonlab.diamer2.seq;

public class Sequence {

    String header;
    String sequence;

    public Sequence(String header, String sequence) {
        /*
        @param header: the header of the Sequence entry without ">"
        @param sequence: the sequence of the Sequence entry
         */
        this.header = header;
        this.sequence = sequence.replace("\n", "").replace(" ", "");
    }

    public String getHeader() {
        return header;
    }

    public String getSequence() {
        return sequence;
    }

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
