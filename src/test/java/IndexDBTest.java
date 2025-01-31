import org.husonlab.diamer2.indexing.DBIndexer;
import org.husonlab.diamer2.indexing.ReadIndexer;
import org.husonlab.diamer2.indexing.kmers.KmerEncoder;
import org.husonlab.diamer2.indexing.kmers.KmerExtractor;
import org.husonlab.diamer2.io.NCBIReader;
import org.husonlab.diamer2.io.ReadAssignmentIO;
import org.husonlab.diamer2.io.indexing.BucketIO;
import org.husonlab.diamer2.io.indexing.DBIndexIO;
import org.husonlab.diamer2.io.indexing.ReadIndexIO;
import org.husonlab.diamer2.io.seq.FastaIdReader;
import org.husonlab.diamer2.io.seq.SequenceSupplier;
import org.husonlab.diamer2.main.encoders.K15Base11;
import org.husonlab.diamer2.readAssignment.ReadAssigner;
import org.husonlab.diamer2.readAssignment.ReadAssignment;
import org.husonlab.diamer2.readAssignment.algorithms.OVO;
import org.husonlab.diamer2.seq.CharSequence;
import org.husonlab.diamer2.seq.Sequence;
import org.husonlab.diamer2.seq.SequenceRecord;
import org.husonlab.diamer2.seq.alphabet.AlphabetDNA;
import org.husonlab.diamer2.seq.alphabet.converter.AAtoBase11;
import org.husonlab.diamer2.seq.alphabet.converter.Converter;
import org.husonlab.diamer2.seq.alphabet.converter.DNAtoBase11;
import org.husonlab.diamer2.taxonomy.Tree;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.Assert.assertTrue;

public class IndexDBTest {
    @Test
    public void testIndexDB() throws IOException {
        Path nodesDmp = Path.of("src/test/resources/database/nodes.dmp");
        Path namesDmp = Path.of("src/test/resources/database/names.dmp");
        Path dbPreprocessed = Path.of("src/test/resources/database/db_preprocessed.fsa");
        Path reads = Path.of("src/test/resources/reads/reads.fq");
        Path dbIndex = Path.of("src/test/resources/test_output/db_index");
        Path readsIndex = Path.of("src/test/resources/test_output/reads_index");
        Path output = Path.of("src/test/resources/test_output/assignment");
        boolean[] mask = new boolean[]{
                true, true, true, true, true, true, true, true, true, true, true, true, true, true, true
        };

        // Generate DB index
        Tree tree = NCBIReader.readTaxonomy(nodesDmp, namesDmp);
        DBIndexer indexer = new DBIndexer(dbPreprocessed, dbIndex, tree, new K15Base11(mask, 22), 1, 1, 1, 1024, false, true);
        indexer.index();

        // Generate read index
        ReadIndexer readIndexer = new ReadIndexer(reads, readsIndex, new K15Base11(mask, 22), 1024, 1, 1, 1, true);
        readIndexer.index();

        // Assign reads
        ReadAssigner readAssigner = new ReadAssigner(tree, 1, dbIndex, readsIndex, new K15Base11(mask, 22));
        ReadAssignment assignment = readAssigner.assignReads();
        ReadAssignmentIO.writeRawAssignment(assignment, output.resolve("raw_assignments.tsv"));
        assignment.addKmerCounts();
        assignment.runAssignmentAlgorithm(new OVO(tree, 0.2f));
        assignment.runAssignmentAlgorithm(new OVO(tree, 0.8f));
        ReadAssignmentIO.writePerReadAssignments(assignment, output.resolve("per_read_assignments.tsv"), false, true);
        ReadAssignmentIO.writePerTaxonAssignments(assignment, output.resolve("per_taxon_assignments.tsv"), 1, true);
        ReadAssignmentIO.writeForMEGANImport(assignment, output.resolve("megan.tsv"), 1, 0);

//        // Compare indexing result
//        DBIndexIO index = new DBIndexIO(Path.of("src\\test\\resources\\test_output\\db_index"));
//        System.out.println(index.getBucketIO(0).read());
//        SequenceSupplier<Integer, Byte> supplier = new SequenceSupplier<>(
//                new FastaIdReader(Path.of("src\\test\\resources\\database\\db_preprocessed.fsa")),
//                new AAtoBase11(), false);
//
//        SequenceRecord<Integer, Byte> record;
//        KmerExtractor extractor = new KmerExtractor(new KmerEncoder(11, mask));
//        while ((record = supplier.next()) != null) {
//            System.out.println(record.getSequenceString());
//            System.out.println(Arrays.toString(extractor.extractKmers(record.sequence())));
//            long[] kmers = extractor.extractKmers(record.sequence());
//            for (long kmer : kmers) {
//                int bucketName = (int)(kmer & 0b1111111111L);
//                long kmerEntry = ((kmer >>> 10) << 22) | record.id();
//                BucketIO.BucketReader bucket = index.getBucketReader(bucketName);
//                boolean found = false;
//                while (bucket.hasNext()) {
//                    long bucketEntry = bucket.next();
//                    if (bucketEntry == kmerEntry) {
//                        found = true;
//                    }
//                }
//                assertTrue(found);
//            }
//        }
    }

    @Test
    public void toBase11Test() {
        Converter<Character, Byte> dnaConverter = new DNAtoBase11();
        Converter<Character, Byte> aaConverter = new AAtoBase11();
        for (Sequence<Byte> sequence: dnaConverter.convert(new CharSequence(
                new AlphabetDNA(),
                "ATCGCATTCAATGCTGCTGCGCATCAGATAGCACACGCGCGCGCCATACTGATACAGTTTGCGCAGGCTATGCAGGTTTTCTTCGCGCAGCGGGCATTC"))) {
            System.out.println(sequence);
        }
        for (Sequence<Byte> sequence: aaConverter.convert(new CharSequence(
                new AlphabetDNA(),
                "ECPLREENLHSLRKLYQYGARVCYLMRSSIECD"))) {
            System.out.println(sequence);
        }
    }
}
