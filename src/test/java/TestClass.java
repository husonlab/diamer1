import org.husonlab.diamer2.io.ReadAssignmentIO;
import org.husonlab.diamer2.io.seq.FastaIdReader;
import org.husonlab.diamer2.io.seq.SequenceReader;
import org.husonlab.diamer2.io.seq.SequenceSupplier;
import org.husonlab.diamer2.io.taxonomy.TreeIO;
import org.husonlab.diamer2.main.GlobalSettings;
import org.husonlab.diamer2.main.Main;
import org.husonlab.diamer2.main.encoders.W15;
import org.husonlab.diamer2.readAssignment.ReadAssignment;
import org.husonlab.diamer2.readAssignment.algorithms.OVO;
import org.husonlab.diamer2.seq.SequenceRecord;
import org.husonlab.diamer2.taxonomy.Tree;
import org.husonlab.diamer2.util.DBIndexAnalyzer;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static org.husonlab.diamer2.io.ReadAssignmentIO.readRawKrakenAssignment;
import static org.husonlab.diamer2.io.NCBIReader.readTaxonomy;
import static org.husonlab.diamer2.main.Main.parseMask;

public class TestClass {
    @Test
    public void test() {
        Path output = Path.of("F:\\Studium\\Master\\semester5\\thesis\\data\\test_dataset\\assignment_kraken2_nr_processed");
        GlobalSettings settings = new GlobalSettings(new String[0], 12, 1, 3, false, true, true, false);
        Tree tree = readTaxonomy(Path.of("F:\\Studium\\Master\\semester5\\thesis\\data\\NCBI\\taxdmp\\nodes.dmp"), Path.of("F:\\Studium\\Master\\semester5\\thesis\\data\\NCBI\\taxdmp\\names.dmp"), true);
        ReadAssignment readAssignment = readRawKrakenAssignment(tree, Path.of("F:\\Studium\\Master\\semester5\\thesis\\data\\test_dataset\\assignment_kraken2_nr\\output.txt"), settings);
        readAssignment.addKmerCountsToTree();
        readAssignment.sortKmerCounts();
        readAssignment.runAssignmentAlgorithmOnKmerCounts(new OVO(0.2f));
        readAssignment.runAssignmentAlgorithmOnKmerCounts(new OVO(0.5f));
        readAssignment.runAssignmentAlgorithmOnKmerCounts(new OVO(0.6f));
        readAssignment.runAssignmentAlgorithmOnKmerCounts(new OVO(0.8f));
        readAssignment.runAssignmentAlgorithmOnKmerCounts(new OVO(0.9f));
        readAssignment.runAssignmentAlgorithmOnKmerCounts(new OVO(1.0f));
        readAssignment.addReadCountsToTree();
        ReadAssignmentIO.writePerReadAssignments(readAssignment, output.resolve("per_read_assignments.tsv"), false, true, settings);
        TreeIO.savePerTaxonAssignment(readAssignment.getTree(), output.resolve("per_taxon_assignments.tsv"));
        TreeIO.saveForMegan(readAssignment.getTree(), output.resolve("megan.tsv"), List.of(new String[]{"kmer count"}), List.of(new String[0]));
    }

    @Test
    public void analyseDBIndex() {
        String args[] = new String[]{
                "--analyze-db-index", "--only-standard-ranks",
                "F:\\Studium\\Master\\semester5\\thesis\\data\\NCBI\\100\\index_longspaced",
                "C:\\Users\\noel\\Documents\\diamer2\\statistics\\KmerHistogramPerRankNr100Longspaced"
        };
        Main.main(args);
    }

    @Test
    public void testReadSpeed() throws IOException {
    }
}
