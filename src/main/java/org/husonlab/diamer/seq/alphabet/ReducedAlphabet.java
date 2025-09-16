package org.husonlab.diamer.seq.alphabet;

import java.util.ArrayList;
import java.util.Arrays;

import static org.husonlab.diamer.seq.converter.Utilities.splitAtMinus1;

public abstract class ReducedAlphabet extends Alphabet<Byte> {
    /**
     * Converts an uppercase DNA triplet into a byte array of size two that represents the encoded amino acid in
     * and the encoded amino acid of the reverse complement.
     * @param triplet DNA triplet (upper case)
     * @return {encoded amino acid, encoded amino acid of reverse complement}
     */
    public abstract byte[] translateCodon(String triplet);

    /**
     * Translates a DNA sequence into an array of arrays that represent all six reading frames and all fragments that
     * are separated by stop codons.
     * @param dna DNA sequence to translate (upper case)
     * @return array of arrays with all translated sequences.
     */
    public byte[][] translateRead(char[] dna){

        // The sequence is too short to be translated
        if (dna.length < 3) {
            return new byte[0][];
        }

        StringBuilder triplet = new StringBuilder();
        // Setup byte array for each reading frame
        byte[][] translations = new byte[6][];
        int[] sequenceLengths = new int[3];
        for (int i = 0; i < 3; i++) {
            int len = (dna.length-i)/3;
            sequenceLengths[i] = len;
            translations[i*2] = new byte[len];
            translations[i*2+1] = new byte[len];
        }
        triplet.append(dna[0]).append(dna[1]);

        for (int i = 2; i < dna.length; i++) {
            triplet.append(dna[i]);
            byte[] encoding = translateCodon(triplet.toString());
            int i2 = i*2-4;
            // Forward reading frame
            translations[i2%6][i2/6] = encoding[0];
            // Reverse reading frame, gets filled in reverse order
            translations[(i2%6)+1][sequenceLengths[(i+1)%3]-i2/6-1] = encoding[1];
            triplet.deleteCharAt(0);
        }

        // Split translations at -1 (Unknown codons and stop codons)
        ArrayList<byte[]> splitTranslations = new ArrayList<>();
        for (byte[] translation : translations) {
            if (translation.length > 0) {
                splitTranslations.addAll(Arrays.asList(splitAtMinus1(translation)));
            }
        }
        return splitTranslations.toArray(new byte[0][]);
    }

    /**
     * Translates an amino acid into a number of the alphabet.
     * @param aa amino acid (upper case)
     * @return the number that represents the amino acid in the alphabet
     */
    public abstract byte translateAA(char aa);

    /**
     * Converts a protein sequence into an array of all fragments that are separated by unknown characters (including
     * {@code *}).
     * @param peptide protein sequence (upper case)
     * @return array of all fragments encoded in the alphabet.
     */
    public byte[][] translateDBSequence(char[] peptide) {
        byte[] result = new byte[peptide.length];
        for (int i = 0; i < peptide.length; i++) {
            result[i] = translateAA(peptide[i]);
        }
        return splitAtMinus1(result);
    }
}
