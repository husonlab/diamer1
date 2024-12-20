import org.husonlab.diamer2.seq.alphabet.AAEncoder;
import org.husonlab.diamer2.seq.alphabet.AAKmerEncoder;
import org.husonlab.diamer2.seq.alphabet.DNAEncoder;
import org.husonlab.diamer2.seq.alphabet.DNAKmerEncoder;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReducedAlphabetTest {

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
        assertArrayEquals(
                new short[]{DNAEncoder.codonToAAAndBase11("CCC"), DNAEncoder.codonToAAAndBase11("GGG")},
                DNAEncoder.toAAAndBase11FR("CCC"));
    }

    @Test
    public void testDNAKmerEncoder() {
        DNAKmerEncoder dnaKmerEncoder = new DNAKmerEncoder(4, "TT");
        dnaKmerEncoder.addNucleotide('T');
        dnaKmerEncoder.addNucleotide('C');
        dnaKmerEncoder.addNucleotide('A');
        dnaKmerEncoder.addNucleotide('A');
        dnaKmerEncoder.addNucleotide('T');
        dnaKmerEncoder.addNucleotide('G');
        dnaKmerEncoder.addNucleotide('T');
        dnaKmerEncoder.addNucleotide('C');
        dnaKmerEncoder.addNucleotide('T');
        assertArrayEquals(new long[]{6734, 143}, dnaKmerEncoder.addNucleotide('T'));
        assertArrayEquals(new long[]{6682, 22}, dnaKmerEncoder.addNucleotide('C'));
        assertArrayEquals(new long[]{2432, 88}, dnaKmerEncoder.addNucleotide('G'));
        assertArrayEquals(new long[]{869, 1344}, dnaKmerEncoder.addNucleotide('T'));
        assertArrayEquals(new long[]{299, 2}, dnaKmerEncoder.addNucleotide('T'));
        assertArrayEquals(new long[]{12116, 8}, dnaKmerEncoder.addNucleotide('C'));
        assertArrayEquals(new long[]{9560, 122}, dnaKmerEncoder.addNucleotide('A'));
        assertArrayEquals(new long[]{3289, 2662}, dnaKmerEncoder.addNucleotide('G'));
        assertArrayEquals(new long[]{1508, 1331}, dnaKmerEncoder.addNucleotide('T'));
        assertArrayEquals(new long[]{2675, 11}, dnaKmerEncoder.addNucleotide('T'));
    }
}
