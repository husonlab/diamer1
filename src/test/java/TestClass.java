import org.husonlab.diamer2.indexing.kmers.KmerEncoder;
import org.husonlab.diamer2.indexing.kmers.KmerExtractor;
import org.husonlab.diamer2.io.ReadAssignmentIO;
import org.husonlab.diamer2.io.seq.FastaIdReader;
import org.husonlab.diamer2.io.seq.FutureSequenceRecords;
import org.husonlab.diamer2.io.seq.SequenceReader;
import org.husonlab.diamer2.io.seq.SequenceSupplier;
import org.husonlab.diamer2.io.taxonomy.TreeIO;
import org.husonlab.diamer2.main.GlobalSettings;
import org.husonlab.diamer2.main.Main;
import org.husonlab.diamer2.main.encoders.Encoder;
import org.husonlab.diamer2.main.encoders.W15;
import org.husonlab.diamer2.readAssignment.ReadAssignment;
import org.husonlab.diamer2.readAssignment.algorithms.OVO;
import org.husonlab.diamer2.seq.SequenceRecord;
import org.husonlab.diamer2.seq.alphabet.Alphabet;
import org.husonlab.diamer2.seq.alphabet.Base11Alphabet;
import org.husonlab.diamer2.taxonomy.Tree;
import org.husonlab.diamer2.util.DBIndexAnalyzer;
import org.junit.Test;

import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
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
    public void averageDBProteinLength() throws IOException {
        HashMap<Integer, Long> lengths = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("F:\\Studium\\Master\\semester5\\thesis\\data\\NCBI\\100\\nr100_preprocessed.fsa")))) {
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                if (line.startsWith(">") && !sb.isEmpty()) {
                    lengths.put(sb.length(), lengths.getOrDefault(sb.length(), 0L) + 1);
                    sb.setLength(0);
                } else if (!line.startsWith(">")) {
                    sb.append(line);
                }
            }
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("C:\\Users\\nk035\\Documents\\diamer2\\statistics\\protein_lengths.tsv"))) {
            bw.write("length\tcount\n");
            for (Integer length : lengths.keySet()) {
                bw.write(length + "\t" + lengths.get(length) + "\n");
            }
        }
    }

    @Test
    public void kmerComplexity() {
        long[] complexity = new long[15];
        KmerEncoder kmerEncoder = new KmerEncoder(11, parseMask("111111111111111"), new double[] {
            0.29923406,
            0.216387433,
            0.223466117,
            0.07384495,
            0.048369393,
            0.038860925,
            0.030088765,
            0.012823821,
            0.021374994,
            0.022660274,
            0.012889268,
        });
        Base11Alphabet alphabet = new Base11Alphabet();
        try (SequenceSupplier<Integer, byte[]> sup = new SequenceSupplier<>(new FastaIdReader(Path.of("F:\\Studium\\Master\\semester5\\thesis\\data\\NCBI\\100\\nr100_preprocessed.fsa")),
                alphabet::translateDBSequence, false)) {
            FutureSequenceRecords<Integer, byte[]> futureSequenceRecords;
            while ((futureSequenceRecords = sup.next()) != null) {
                for (SequenceRecord<Integer, byte[]> sequenceRecord : futureSequenceRecords.getSequenceRecords()) {
                    byte[] sequence = sequenceRecord.sequence();
                    int seqLength = sequence.length;
                    if (seqLength >= 15) {
                        kmerEncoder.reset();
                        // add the first k-1 characters to the encoder
                        for (int i = 0; i < 15 - 1; i++) {
                            kmerEncoder.addBack(sequence[i]);
                        }
                        // add the remaining characters to the encoder and store the resulting encoding
                        for (int i = 15 - 1; i < seqLength; i++) {
                            kmerEncoder.addBack(sequence[i]);
                            complexity[kmerEncoder.getComplexity()]++;
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println(Arrays.toString(complexity));
    }

    @Test
    public void compareWithKraken() throws FileNotFoundException {
        Path krakenAssignment = Path.of("F:/Studium/Master/semester5/thesis/data/test_dataset/assignment_kraken2_nr/output.txt");
        Path assignmentFolder = Path.of("F:\\Studium\\Master\\semester5\\thesis\\data\\test_dataset\\assignment_nr_diamond_filteredE-12");

        int nrOfReads;

        try (BufferedReader br = new BufferedReader(new FileReader(assignmentFolder.resolve("raw_assignment.tsv").toString()))) {
            nrOfReads = Integer.parseInt(br.readLine());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (BufferedReader br = new BufferedReader(new FileReader(krakenAssignment.toString()))) {

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
