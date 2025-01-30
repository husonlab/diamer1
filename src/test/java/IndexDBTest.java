import org.husonlab.diamer2.indexing.DBIndexer;
import org.husonlab.diamer2.indexing.kmers.KmerEncoder;
import org.husonlab.diamer2.indexing.kmers.KmerExtractor;
import org.husonlab.diamer2.io.NCBIReader;
import org.husonlab.diamer2.io.indexing.DBIndexIO;
import org.husonlab.diamer2.io.seq.FastaIdReader;
import org.husonlab.diamer2.io.seq.SequenceSupplier;
import org.husonlab.diamer2.main.encoders.K15Base11;
import org.husonlab.diamer2.seq.SequenceRecord;
import org.husonlab.diamer2.seq.alphabet.converter.AAtoBase11;
import org.husonlab.diamer2.taxonomy.Tree;
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
                true, true, true, true, true, true, true, true, true, true, true, true, true, true, false, false, true //16395492700556
        }));
        while ((record = supplier.next()) != null) {
            System.out.println(record.getSequenceString());
            System.out.println(Arrays.toString(extractor.extractKmers(record.sequence())));
        }
        Tree tree = NCBIReader.readTaxonomy(
                Path.of("src/test/resources/database/nodes.dmp"),
                Path.of("src/test/resources/database/names.dmp"));
        DBIndexer indexer = new DBIndexer(
                Path.of("C:\\Users\\noel\\Documents\\diamer2\\src\\test\\resources\\database\\db_preprocessed.fsa"),
                Path.of("C:\\Users\\noel\\Documents\\diamer2\\src\\test\\resources\\test_output\\test_index"),
                tree,
                new K15Base11(new boolean[]{true, true, true, true, true, true, true, true, true, true, true, true, true, true, false, false, true}, 22),
                1,
                1,
                1,
                1024,
                false,
                true);
        indexer.index();

        DBIndexIO index = new DBIndexIO(Path.of("C:\\Users\\noel\\Documents\\diamer2\\src\\test\\resources\\test_output\\test_index"));
        System.out.println(index.getBucketIO(0).read());
    }
}
