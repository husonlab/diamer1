import org.husonlab.diamer2.indexing.kmers.KmerEncoder;
import org.husonlab.diamer2.indexing.kmers.KmerExtractor;
import org.husonlab.diamer2.io.seq.FastaIdReader;
import org.husonlab.diamer2.io.seq.SequenceSupplier;
import org.husonlab.diamer2.seq.SequenceRecord;
import org.husonlab.diamer2.seq.alphabet.converter.AAtoBase11;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

public class IndexDBTest {
    @Test
    public void testIndexDB() throws IOException {
        SequenceSupplier<Integer, Byte> supplier = new SequenceSupplier<>(
                new FastaIdReader(Path.of("C:\\Users\\noel\\Documents\\diamer2\\src\\test\\resources\\database\\db_preprocessed.fsa")),
                new AAtoBase11(), false);

        SequenceRecord<Integer, Byte> record;
        KmerExtractor extractor = new KmerExtractor(new KmerEncoder(11, new boolean[]{
                true, false, true, true, true, true, true, false, false, true
        }));
        while ((record = supplier.next()) != null) {
            System.out.println(record.getSequenceString());
            System.out.println(Arrays.toString(extractor.extractKmers(record.sequence())));
        }
    }
}
