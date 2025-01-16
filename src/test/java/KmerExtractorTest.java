import org.husonlab.diamer2.seq.kmers.KmerExtractor;
import org.husonlab.diamer2.seq.kmers.KmerExtractorDNA;
import org.husonlab.diamer2.seq.kmers.KmerExtractorProtein;
import org.husonlab.diamer2.seq.alphabet.*;
import org.junit.Test;

import static org.junit.Assert.*;

public class KmerExtractorTest {
    @Test
    public void testExtractKmersProtein() {
        String seq = "ACDEFGHIJK";
        ReducedProteinAlphabet alphabet = new Base11Alphabet();
        KmerExtractor extractor = new KmerExtractorProtein(0b011010100L, alphabet);

        long[] emptyKmers = extractor.extractKmers("ACD");
        assertEquals(0, emptyKmers.length);

        long[] kmers = extractor.extractKmers(seq);
        assertEquals(5, kmers.length);
        assertEquals(
                (long)(alphabet.encodeAA('A')*Math.pow(11, 3) +
                alphabet.encodeAA('C')*Math.pow(11, 2) +
                alphabet.encodeAA('E')*11 +
                alphabet.encodeAA('G')),
            kmers[0]);
        assertEquals(
                (long)(alphabet.encodeAA('C')*Math.pow(11, 3) +
                alphabet.encodeAA('D')*Math.pow(11, 2) +
                alphabet.encodeAA('F')*11 +
                alphabet.encodeAA('H')),
            kmers[1]);
        assertEquals(
                (long)(alphabet.encodeAA('D')*Math.pow(11, 3) +
                alphabet.encodeAA('E')*Math.pow(11, 2) +
                alphabet.encodeAA('G')*11 +
                alphabet.encodeAA('I')),
            kmers[2]);
        assertEquals(
                (long)(alphabet.encodeAA('E')*Math.pow(11, 3) +
                alphabet.encodeAA('F')*Math.pow(11, 2) +
                alphabet.encodeAA('H')*11 +
                alphabet.encodeAA('J')),
            kmers[3]);
        assertEquals(
                (long)(alphabet.encodeAA('F')*Math.pow(11, 3) +
                alphabet.encodeAA('G')*Math.pow(11, 2) +
                alphabet.encodeAA('I')*11 +
                alphabet.encodeAA('K')),
            kmers[4]);
    }

    @Test
    public void testExtractKmersDNA() {
        String seq = "ACGTTGAACTTCGGTACATGCT";
        ReducedProteinAlphabet alphabet = new Base11Alphabet();
        KmerExtractor extractor = new KmerExtractorDNA(0b0101100100L, alphabet);

        long[] emptyKmers = extractor.extractKmers("ACGACGACGACGACGAC");
        assertEquals(0, emptyKmers.length);

        long[] kmers = extractor.extractKmers(seq);
        assertEquals(4, kmers.length);
        assertEquals(
                (long)(alphabet.encodeDNA("ACG")[0]*Math.pow(11, 3) +
                alphabet.encodeDNA("AAC")[0]*Math.pow(11, 2) +
                alphabet.encodeDNA("TTC")[0]*11 +
                alphabet.encodeDNA("TGC")[0]),
            kmers[0]);
        assertEquals(
                (long)(alphabet.encodeDNA("TGC")[1]*Math.pow(11, 3) +
                alphabet.encodeDNA("GGT")[1]*Math.pow(11, 2) +
                alphabet.encodeDNA("TTC")[1]*11 +
                alphabet.encodeDNA("ACG")[1]),
            kmers[1]);
        assertEquals(
                (long)(alphabet.encodeDNA("CGT")[0]*Math.pow(11, 3) +
                alphabet.encodeDNA("ACT")[0]*Math.pow(11, 2) +
                alphabet.encodeDNA("TCG")[0]*11 +
                alphabet.encodeDNA("GCT")[0]),
                kmers[2]);
        assertEquals(
                (long)(alphabet.encodeDNA("GCT")[1]*Math.pow(11, 3) +
                alphabet.encodeDNA("GTA")[1]*Math.pow(11, 2) +
                alphabet.encodeDNA("TCG")[1]*11 +
                alphabet.encodeDNA("CGT")[1]),
            kmers[3]);
    }
}
