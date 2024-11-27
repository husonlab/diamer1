import org.husonlab.diamer2.alphabet.AAEncoder;
import org.husonlab.diamer2.alphabet.AAKmerEncoder;
import org.husonlab.diamer2.alphabet.DNAEncoder;
import org.husonlab.diamer2.alphabet.DNAKmerEncoder;
import org.junit.Test;

import static org.junit.Assert.*;

public class AlphabetTest {

    @Test
    public void testAAEncoder() {
        assertEquals(10, AAEncoder.toBase11('W'));
        assertEquals(0, AAEncoder.toBase11('K'));
        assertEquals(6, AAEncoder.toBase11('Y'));
        assertEquals(57, AAEncoder.toBase11("NQFL"));
        assertEquals(632, AAEncoder.toBase11("QFLF"));
        assertEquals(6953, AAEncoder.toBase11("FLFA"));
        assertEquals(3281, AAEncoder.toBase11("LFAG"));
    }

    @Test
    public void testAAKmerEncoder() {
        AAKmerEncoder encoder = new AAKmerEncoder(4, 11);
        assertEquals(0, encoder.getEncodedKmer());
        assertEquals(0, encoder.addBack(AAEncoder.toBase11('N')));
        assertEquals(0, encoder.addBack(AAEncoder.toBase11('Q')));
        assertEquals(5, encoder.addBack(AAEncoder.toBase11('F')));
        assertEquals(57, encoder.addBack(AAEncoder.toBase11('L')));
        assertEquals(632, encoder.addBack(AAEncoder.toBase11('F')));
        assertEquals(6953, encoder.addBack(AAEncoder.toBase11('A')));
        assertEquals(3281, encoder.addBack(AAEncoder.toBase11('G')));
    }

    @Test
    public void testDNAEncoder() {
        assertEquals('A', DNAEncoder.toAA("GCT"));
        assertEquals(5, DNAEncoder.codonToAAAndBase11("TTT"));
        assertArrayEquals(new short[]{4, 4}, DNAEncoder.toAAAndBase11FR("CCC"));
    }

    @Test
    public void testDNAKmerEncoder() {
        DNAKmerEncoder dnaKmerEncoder = new DNAKmerEncoder(4, "TT");
        assertArrayEquals(new long[]{5, 6655}, dnaKmerEncoder.addNucleotide('T'));
        assertArrayEquals(new long[]{5, 2662}, dnaKmerEncoder.addNucleotide('C'));
        assertArrayEquals(new long[]{1, 1331}, dnaKmerEncoder.addNucleotide('A'));
        assertArrayEquals(new long[]{55, 605}, dnaKmerEncoder.addNucleotide('A'));
        assertArrayEquals(new long[]{55, 242}, dnaKmerEncoder.addNucleotide('T'));
        assertArrayEquals(new long[]{20, 2783}, dnaKmerEncoder.addNucleotide('G'));
        assertArrayEquals(new long[]{612, 9372}, dnaKmerEncoder.addNucleotide('T'));
        assertArrayEquals(new long[]{607, 2684}, dnaKmerEncoder.addNucleotide('C'));
        assertArrayEquals(new long[]{221, 1584}, dnaKmerEncoder.addNucleotide('T'));
        assertArrayEquals(new long[]{6734, 7507}, dnaKmerEncoder.addNucleotide('T'));
        assertArrayEquals(new long[]{6682, 2906}, dnaKmerEncoder.addNucleotide('C'));
        assertArrayEquals(new long[]{2432, 1475}, dnaKmerEncoder.addNucleotide('G'));
        assertArrayEquals(new long[]{869, 9999}, dnaKmerEncoder.addNucleotide('T'));
        assertArrayEquals(new long[]{299, 2926}, dnaKmerEncoder.addNucleotide('T'));
        assertArrayEquals(new long[]{12116, 2796}, dnaKmerEncoder.addNucleotide('C'));
        assertArrayEquals(new long[]{9560, 2240}, dnaKmerEncoder.addNucleotide('A'));
        assertArrayEquals(new long[]{3289, 266}, dnaKmerEncoder.addNucleotide('G'));
        assertArrayEquals(new long[]{1508, 254}, dnaKmerEncoder.addNucleotide('T'));
        assertArrayEquals(new long[]{2675, 2865}, dnaKmerEncoder.addNucleotide('T'));
    }


}
