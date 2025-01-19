import org.husonlab.diamer2.io.Utilities;
import org.husonlab.diamer2.io.seq.FASTAReader;
import org.husonlab.diamer2.io.seq.FASTQReader;
import org.husonlab.diamer2.io.seq.SequenceSupplier;
import org.husonlab.diamer2.seq.SequenceRecord;
import org.husonlab.diamer2.seq.alphabet.converter.AAtoBase11;
import org.husonlab.diamer2.seq.alphabet.converter.DNAtoBase11;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class TestClass {

    @Test
    public void test() throws IOException {
        try (SequenceSupplier<Short> supplier = new SequenceSupplier<>(
                new FASTQReader(new File("F:\\Studium\\Master\\semester5\\thesis\\data\\test_dataset\\test.fq")),
                new DNAtoBase11(), true)) {
            SequenceRecord<Short> record;
            while ((record = supplier.next()) != null) {
                System.out.println(record.getHeader());
            }
            supplier.next();
            supplier.reset();
            supplier.next();
            System.out.println();
        }
    }
}
