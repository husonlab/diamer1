import org.husonlab.diamer2.seq.CharSequence;
import org.husonlab.diamer2.seq.Sequence;
import org.husonlab.diamer2.seq.SequenceRecord;
import org.husonlab.diamer2.seq.ShortSequence;
import org.husonlab.diamer2.seq.alphabet.AlphabetDNA;
import org.husonlab.diamer2.seq.alphabet.Base11Alphabet;
import org.husonlab.diamer2.seq.converter.Converter;
import org.husonlab.diamer2.seq.converter.DNAtoBase11;
import org.junit.Test;

public class AlphabetTest {
    @Test
    public void testAlphabetDNA() {
        SequenceRecord<Character> record = new SequenceRecord<>(
            "header",
            new CharSequence(new AlphabetDNA(), "ACCTGGT")
        );

        SequenceRecord<Short> record2 = new SequenceRecord<>(
            "header",
            new ShortSequence(new Base11Alphabet(), new short[]{0, 1, 2, 3})
        );

        Converter<Character, Short> converter = new DNAtoBase11();

        Sequence<Short>[] test = converter.convert(record.getSequence());

        System.out.println(test);
    }
}
