package org.husonlab.diamer2.seq.alphabet.converter;

import org.husonlab.diamer2.seq.Sequence;
import org.husonlab.diamer2.seq.Compressed4BitSequence;
import org.husonlab.diamer2.seq.alphabet.Alphabet;
import org.husonlab.diamer2.seq.alphabet.AlphabetDNA;
import org.husonlab.diamer2.seq.alphabet.Base11Alphabet;

import java.util.ArrayList;

/**
 * Converter to convert DNA to a base 11 alphabet that is designed to have about the same likelihood for each number.
 */
public class DNAtoBase11Uniform implements Converter<Character, Byte> {

    private static final Alphabet<Character> SOURCE_ALPHABET = new AlphabetDNA();
    private static final Alphabet<Byte> TARGET_ALPHABET = new Base11Alphabet();

    @Override
    public Sequence<Byte>[] convert(Sequence<Character> sequence) {

        // The sequence is too short to be translated
        if (sequence.length() < 3) {
            return new Sequence[0];
        }
        StringBuilder triplet = new StringBuilder();
        // Setup byte array for each reading frame
        byte[][] translations = new byte[6][];
        int[] sequenceLengths = new int[3];
        for (int i = 0; i < 3; i++) {
            int len = (sequence.length()-i)/3;
            sequenceLengths[i] = len;
            translations[i*2] = new byte[len];
            translations[i*2+1] = new byte[len];
        }
        triplet.append(sequence.get(0)).append(sequence.get(1));

        for (int i = 2; i < sequence.length(); i++) {
            triplet.append(sequence.get(i));
            byte[] encoding = encodeDNA(triplet.toString());
            int i2 = i*2-4;
            // Forward reading frame
            translations[i2%6][i2/6] = encoding[0];
            // Reverse reading frame, gets filled in reverse order
            translations[(i2%6)+1][sequenceLengths[(i+1)%3]-i2/6-1] = encoding[1];
            triplet.deleteCharAt(0);
        }
        if (sequence.length() == 3) {
            return splitAtStopCodons(new Sequence[]{
                    new Compressed4BitSequence(new Base11Alphabet(), translations[0]),
                    new Compressed4BitSequence(new Base11Alphabet(), translations[1])});
        }
        if (sequence.length() == 4) {
            return splitAtStopCodons(new Sequence[]{
                    new Compressed4BitSequence(new Base11Alphabet(), translations[0]),
                    new Compressed4BitSequence(new Base11Alphabet(), translations[1]),
                    new Compressed4BitSequence(new Base11Alphabet(), translations[2]),
                    new Compressed4BitSequence(new Base11Alphabet(), translations[3])});
        }
        return splitAtStopCodons(new Sequence[]{
                new Compressed4BitSequence(new Base11Alphabet(), translations[0]),
                new Compressed4BitSequence(new Base11Alphabet(), translations[1]),
                new Compressed4BitSequence(new Base11Alphabet(), translations[2]),
                new Compressed4BitSequence(new Base11Alphabet(), translations[3]),
                new Compressed4BitSequence(new Base11Alphabet(), translations[4]),
                new Compressed4BitSequence(new Base11Alphabet(), translations[5])});
    }

    /**
     * Splits all input sequences at stop codons {@code -1} and returns the resulting sequences.
     * @param sequences Input sequences
     * @return Sequences split at stop codons
     */
    private Sequence<Byte>[] splitAtStopCodons(Sequence<Byte>[] sequences) {
        ArrayList<Sequence<Byte>> sequencesArrayList = new ArrayList<>();
        for (Sequence<Byte> sequence : sequences) {
            ArrayList<Byte> sequenceArrayList = new ArrayList<>();
            for (byte aa: sequence) {
                if (aa == -1) {
                    if (!sequenceArrayList.isEmpty()) {
                        sequencesArrayList.add(new Compressed4BitSequence(new Base11Alphabet(), sequenceArrayList.toArray(new Byte[0])));
                        sequenceArrayList = new ArrayList<>();
                    }
                } else {
                    sequenceArrayList.add(aa);
                }
            }
            if (!sequenceArrayList.isEmpty()) {
                sequencesArrayList.add(new Compressed4BitSequence(new Base11Alphabet(), sequenceArrayList.toArray(new Byte[0])));
            }
        }
        return sequencesArrayList.toArray(new Sequence[0]);
    }

    @Override
    public Alphabet<Character> getSourceAlphabet() {
        return SOURCE_ALPHABET;
    }

    @Override
    public Alphabet<Byte> getTargetAlphabet() {
        return TARGET_ALPHABET;
    }

    public byte[] encodeDNA(String codon) {
        switch (codon) {
            case "GTC", "GTT" -> { return new byte[]{(byte)3, (byte)8}; }
            case "ATA" -> { return new byte[]{(byte)9, (byte)6}; }
            case "ACG" -> { return new byte[]{(byte)6, (byte)7}; }
            case "TTC" -> { return new byte[]{(byte)9, (byte)5}; }
            case "TAT" -> { return new byte[]{(byte)6, (byte)9}; }
            case "ATC", "ATT" -> { return new byte[]{(byte)9, (byte)8}; }
            case "AGG", "CGG" -> { return new byte[]{(byte)7, (byte)10}; }
            case "TAC" -> { return new byte[]{(byte)6, (byte)3}; }
            case "CTT" -> { return new byte[]{(byte)0, (byte)10}; }
            case "CTC" -> { return new byte[]{(byte)0, (byte)5}; }
            case "ACA", "ACC" -> { return new byte[]{(byte)6, (byte)2}; }
            case "CTG", "TTG" -> { return new byte[]{(byte)0, (byte)7}; }
            case "ACT" -> { return new byte[]{(byte)6, (byte)4}; }
            case "AAG" -> { return new byte[]{(byte)10, (byte)0}; }
            case "CCA" -> { return new byte[]{(byte)10, (byte)3}; }
            case "CTA", "TTA" -> { return new byte[]{(byte)0, (byte)-1}; }
            case "CCC" -> { return new byte[]{(byte)10, (byte)2}; }
            case "CCG", "CCT" -> { return new byte[]{(byte)10, (byte)7}; }
            case "TGA" -> { return new byte[]{(byte)-1, (byte)4}; }
            case "GGG" -> { return new byte[]{(byte)2, (byte)10}; }
            case "TCA" -> { return new byte[]{(byte)4, (byte)-1}; }
            case "AAA" -> { return new byte[]{(byte)10, (byte)9}; }
            case "GTG" -> { return new byte[]{(byte)3, (byte)4}; }
            case "TTT" -> { return new byte[]{(byte)9, (byte)10}; }
            case "GTA" -> { return new byte[]{(byte)3, (byte)6}; }
            case "TAA", "TAG" -> { return new byte[]{(byte)-1, (byte)0}; }
            case "GAG" -> { return new byte[]{(byte)5, (byte)0}; }
            case "AAC", "GAC" -> { return new byte[]{(byte)8, (byte)3}; }
            case "GGA" -> { return new byte[]{(byte)2, (byte)4}; }
            case "GGC", "TGC" -> { return new byte[]{(byte)2, (byte)1}; }
            case "GCG" -> { return new byte[]{(byte)1, (byte)7}; }
            case "AAT", "GAT" -> { return new byte[]{(byte)8, (byte)9}; }
            case "ATG" -> { return new byte[]{(byte)5, (byte)4}; }
            case "TCC" -> { return new byte[]{(byte)4, (byte)2}; }
            case "CGC" -> { return new byte[]{(byte)7, (byte)1}; }
            case "GCT" -> { return new byte[]{(byte)1, (byte)4}; }
            case "CAC" -> { return new byte[]{(byte)4, (byte)3}; }
            case "TGG" -> { return new byte[]{(byte)3, (byte)10}; }
            case "GCA", "GCC" -> { return new byte[]{(byte)1, (byte)2}; }
            case "CAA", "CAG" -> { return new byte[]{(byte)7, (byte)0}; }
            case "AGC" -> { return new byte[]{(byte)4, (byte)1}; }
            case "GAA" -> { return new byte[]{(byte)5, (byte)9}; }
            case "AGT" -> { return new byte[]{(byte)4, (byte)6}; }
            case "TCG", "TCT" -> { return new byte[]{(byte)4, (byte)7}; }
            case "GGT", "TGT" -> { return new byte[]{(byte)2, (byte)6}; }
            case "CAT" -> { return new byte[]{(byte)4, (byte)5}; }
            case "CGT" -> { return new byte[]{(byte)7, (byte)6}; }
            case "AGA", "CGA" -> { return new byte[]{(byte)7, (byte)4}; }
            default -> throw new IllegalArgumentException("Invalid codon: " + codon);
        }
    }
}
