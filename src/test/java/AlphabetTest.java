import org.husonlab.diamer2.seq.CharSequence;
import org.husonlab.diamer2.seq.SequenceRecord;
import org.husonlab.diamer2.seq.ShortSequence;
import org.husonlab.diamer2.seq.alphabet.AlphabetDNA;
import org.husonlab.diamer2.seq.alphabet.Base11Alphabet;
import org.junit.Test;

public class AlphabetTest {
    @Test
    public void testAlphabetDNA() {
        SequenceRecord<Character> record = new SequenceRecord<>(
            "header",
            new CharSequence(new AlphabetDNA(), "ACGT")
        );

        SequenceRecord<Short> record2 = new SequenceRecord<>(
            "header",
            new ShortSequence(new Base11Alphabet(), new short[]{0, 1, 2, 3})
        );

        System.out.println("");
    }
}
