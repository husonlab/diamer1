import org.husonlab.diamer2.taxonomy.Tree;
import org.husonlab.diamer2.io.NCBIReader;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

public class NCBIPreprocessTest {
    @Test
    public void testNCBIPreprocess() throws IOException {
        Tree tree = NCBIReader.readTaxonomyWithAccessions(
                new File("src/test/resources/database/nodes.dmp"),
                new File("src/test/resources/database/names.dmp"),
                new NCBIReader.AccessionMapping[] {
                        new NCBIReader.AccessionMapping("src/test/resources/database/prot.accession2taxid.gz", 1, 2),
                }, true);
        NCBIReader.preprocessNR(new File("src/test/resources/database/db.fsa"), new File("src/test/resources/test_output/db_preprocessed.fsa"), tree);
        byte[] preprocessedDB = Files.readAllBytes(Paths.get("src/test/resources/test_output/db_preprocessed.fsa"));
        byte[] expectedPreprocessedDB = Files.readAllBytes(Paths.get("src/test/resources/database/db_preprocessed.fsa"));
        assertArrayEquals(preprocessedDB, expectedPreprocessedDB);
        byte[] skippedSequences = Files.readAllBytes(Paths.get("src/test/resources/test_output/skipped_sequences.fsa"));
        byte[] expectedSkippedSequences = Files.readAllBytes(Paths.get("src/test/resources/database/skipped_sequences.fsa"));
        assertArrayEquals(skippedSequences, expectedSkippedSequences);
    }
}
