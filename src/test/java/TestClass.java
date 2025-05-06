import org.husonlab.diamer2.indexing.Bucket;
import org.husonlab.diamer2.indexing.ReadIndexer;
import org.husonlab.diamer2.indexing.StatisticsEstimator;
import org.husonlab.diamer2.indexing.kmers.KmerEncoder;
import org.husonlab.diamer2.io.ReadAssignmentIO;
import org.husonlab.diamer2.io.Utilities;
import org.husonlab.diamer2.io.indexing.BucketIO;
import org.husonlab.diamer2.io.seq.FastaIdReader;
import org.husonlab.diamer2.io.seq.FastqIdReader;
import org.husonlab.diamer2.io.seq.FutureSequenceRecords;
import org.husonlab.diamer2.io.seq.SequenceSupplier;
import org.husonlab.diamer2.io.taxonomy.TreeIO;
import org.husonlab.diamer2.main.GlobalSettings;
import org.husonlab.diamer2.main.Main;
import org.husonlab.diamer2.main.encoders.Encoder;
import org.husonlab.diamer2.main.encoders.W15;
import org.husonlab.diamer2.readAssignment.ReadAssignment;
import org.husonlab.diamer2.readAssignment.algorithms.OVA;
import org.husonlab.diamer2.readAssignment.algorithms.OVO;
import org.husonlab.diamer2.seq.SequenceRecord;
import org.husonlab.diamer2.seq.alphabet.Base11Alphabet;
import org.husonlab.diamer2.seq.alphabet.CustomAlphabet;
import org.husonlab.diamer2.seq.alphabet.ReducedAlphabet;
import org.husonlab.diamer2.taxonomy.Tree;
import org.junit.Test;

import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.husonlab.diamer2.indexing.Sorting.radixInPlaceParallel;
import static org.husonlab.diamer2.io.ReadAssignmentIO.readRawKrakenAssignment;
import static org.husonlab.diamer2.io.NCBIReader.readTaxonomy;
import static org.husonlab.diamer2.main.Main.parseMask;
import static org.junit.Assert.assertEquals;

public class TestClass {
    @Test
    public void analyze_kraken_result() {
        Path output = Path.of("F:/Studium/Master/semester5/thesis/data/test_datasets/zymo_oral/assignment_kraken2_processed");
        GlobalSettings settings = new GlobalSettings(new String[0], 12, 1, 3, false, true, true, false);
        Tree tree = readTaxonomy(Path.of("F:/Studium/Master/semester5/thesis/data/NCBI/taxdmp/nodes.dmp"), Path.of("F:/Studium/Master/semester5/thesis/data/NCBI/taxdmp/names.dmp"), true);
        ReadAssignment readAssignment = readRawKrakenAssignment(tree, Path.of("F:/Studium/Master/semester5/thesis/data/test_datasets/zymo_oral/assignment_kraken2/raw_kraken2.txt"), settings);
        readAssignment.addKmerCountsToTree();
        readAssignment.sortKmerCounts();
        readAssignment.runAssignmentAlgorithmOnKmerCounts(new OVO(0.1f));
        readAssignment.runAssignmentAlgorithmOnKmerCounts(new OVO(0.2f));
        readAssignment.runAssignmentAlgorithmOnKmerCounts(new OVO(0.3f));
        readAssignment.runAssignmentAlgorithmOnKmerCounts(new OVO(0.4f));
        readAssignment.runAssignmentAlgorithmOnKmerCounts(new OVO(0.5f));
        readAssignment.runAssignmentAlgorithmOnKmerCounts(new OVO(0.6f));
        readAssignment.runAssignmentAlgorithmOnKmerCounts(new OVO(0.7f));
        readAssignment.runAssignmentAlgorithmOnKmerCounts(new OVO(0.8f));
        readAssignment.runAssignmentAlgorithmOnKmerCounts(new OVO(0.9f));
        readAssignment.runAssignmentAlgorithmOnKmerCounts(new OVO(1.0f));
        readAssignment.runAssignmentAlgorithmOnKmerCounts(new OVA(0.1f));
        readAssignment.runAssignmentAlgorithmOnKmerCounts(new OVA(0.2f));
        readAssignment.runAssignmentAlgorithmOnKmerCounts(new OVA(0.3f));
        readAssignment.runAssignmentAlgorithmOnKmerCounts(new OVA(0.4f));
        readAssignment.runAssignmentAlgorithmOnKmerCounts(new OVA(0.5f));
        readAssignment.runAssignmentAlgorithmOnKmerCounts(new OVA(0.6f));
        readAssignment.runAssignmentAlgorithmOnKmerCounts(new OVA(0.7f));
        readAssignment.runAssignmentAlgorithmOnKmerCounts(new OVA(0.8f));
        readAssignment.runAssignmentAlgorithmOnKmerCounts(new OVA(0.9f));
        readAssignment.runAssignmentAlgorithmOnKmerCounts(new OVA(1.0f));
        readAssignment.addReadCountsToTree();
        ReadAssignmentIO.writePerReadAssignments(readAssignment, output.resolve("per_read_assignments.tsv"), false, true, settings);
        TreeIO.savePerTaxonAssignment(readAssignment.getTree(), output.resolve("per_taxon_assignments.tsv"));
        TreeIO.saveForMegan(readAssignment.getTree(), output.resolve("megan.tsv"), List.of(new String[]{"kmer count"}), List.of(new String[0]));
    }

    @Test
    public void analyseDBIndex() {
        String args[] = new String[]{
                "--analyze-db-index", "--only-standard-ranks",
                "F:/Studium/Master/semester5/thesis/data/NCBI/100/index_longspaced",
                "C:/Users/noel/Documents/diamer2/statistics/KmerHistogramPerRankNr100Longspaced"
        };
        Main.main(args);
    }

    @Test
    public void averageDBProteinLength() throws IOException {
        HashMap<Integer, Long> lengths = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("F:/Studium/Master/semester5/thesis/data/NCBI/100/nr100_preprocessed.fsa")))) {
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
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("C:/Users/nk035/Documents/diamer2/statistics/protein_lengths.tsv"))) {
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
        try (SequenceSupplier<Integer, byte[]> sup = new SequenceSupplier<>(new FastaIdReader(Path.of("F:/Studium/Master/semester5/thesis/data/NCBI/100/nr100_preprocessed.fsa")),
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
        Path assignmentFolder = Path.of("F:/Studium/Master/semester5/thesis/data/test_dataset/assignment_nr_diamond_filteredE-12");

        int nrOfReads;
        int cols = 15;
        int colIndex = 0;

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

    @Test
    public void testW15() {
        W15 w15 = new W15(new Base11Alphabet(), null, null, new boolean[]{true, true, true}, 22);
        long kmer = 13665795155445L;
//        long kmer = 0xFFFFFFFFFFFFFL;
        int bucket = w15.getBucketNameFromKmer(kmer);
        long kmerWithoutBucket = w15.getKmerWithoutBucketName(kmer);
        long indexEntry = w15.getIndexEntry(0, kmerWithoutBucket);
        int id = w15.getIdFromIndexEntry(indexEntry);
        long kmerFromIndexEntry = w15.getKmerFromIndexEntry(indexEntry);
        long restoredKmer = w15.getKmerFromIndexEntry(bucket, indexEntry);
        System.out.println("Kmer: " + kmer);
        System.out.println("Kmer: " + Long.toBinaryString(kmer));
        System.out.println("Bucket: " + bucket);
        System.out.println("Kmer without bucket: " + kmerWithoutBucket);
        System.out.println("Kmer without bucket: " + Long.toBinaryString(kmerWithoutBucket));
        System.out.println("Index entry: " + indexEntry);
        System.out.println("Index entry: " + Long.toBinaryString(indexEntry));
        System.out.println("ID: " + id);
        System.out.println("Kmer from index entry: " + kmerFromIndexEntry);
        System.out.println("Kmer from index entry: " + Long.toBinaryString(kmerFromIndexEntry));
        System.out.println("Restored kmer: " + restoredKmer);
        System.out.println("Restored kmer: " + Long.toBinaryString(restoredKmer));
        System.out.println(w15.getBucketNameFromKmer(10));
    }

    @Test
    public void testDeltaCompression() throws IOException {
        int size = 20;
        long[] input = new long[size];
        int[] ids = new int[size];
        for (int i = 0; i < input.length; i++) {
            input[i] = ThreadLocalRandom.current().nextLong();
            ids[i] = (int) (input[i] >>> 32);
        }
        input[10] = -1L;
        input[11] = -1L >>> 1;
        input[15] = 1L << 63;
        input[12] = 0;
        input[13] = 1;
        input[14] = -2;

        radixInPlaceParallel(input, ids, 12);
        for (int i = 0; i < size; i++) {
            System.out.println(input[i]);
        }

        BucketIO bucketIO = new BucketIO(Path.of("C:\\Users\\nk035\\Downloads\\testBucket.bin"), 0);
        bucketIO.write(new Bucket(0, input));
        Bucket bucket = bucketIO.read();
        long[] content = bucket.getContent();
        for (int i = 0; i < size; i++) {
            System.out.println(content[i]);
        }
        for (int i = 0; i < size; i++) {
            assertEquals(input[i], content[i]);
        }
    }

    @Test
    public void bucketReaderTest() throws IOException {
        Bucket bucket = (new BucketIO(Path.of("C:\\Users\\nk035\\Downloads\\1008.bin"), 1008)).read();
        System.out.println(bucket.getName());
    }

    @Test
    public void testReadIndexer2() {
        Path reads = Utilities.getFile("src/test/resources/reads/reads.fq", true);
        Path output = Utilities.getFolder("C:/Users/nk035/Downloads/test_index", false);

        boolean[] mask = parseMask("111111111111111");
        ReducedAlphabet alphabet = new Base11Alphabet();
        Encoder encoder = new W15(alphabet, null, output, mask, 22);
        GlobalSettings globalSettings = new GlobalSettings(new String[0], 1, 1024, 13, true, true, false, false);

        try (FastqIdReader fastqIdReader = new FastqIdReader(reads);
             SequenceSupplier<Integer, byte[]> sup = new SequenceSupplier<Integer, byte[]>(
                     fastqIdReader, alphabet::translateRead, globalSettings.KEEP_IN_MEMORY)) {
            ReadIndexer readIndexer = new ReadIndexer(sup, fastqIdReader, 1000, encoder, globalSettings);
//            ReadIndexer readIndexer = new ReadIndexer(sup, fastqIdReader, output, encoder, globalSettings);
            readIndexer.index();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testBucketSizeEstimatorFastq() {
        Path reads = Utilities.getFile("F:\\Studium\\Master\\semester5\\thesis\\data\\test_dataset\\Zymo-GridION-EVEN-3Peaks-R103-merged.fq.gz", true);

        boolean[] mask = parseMask("1111111111111");
        ReducedAlphabet alphabet = new CustomAlphabet("[L][A][GC][VWUBIZO*][SH][EMX][TY][RQ][DN][IF]");
        Encoder encoder = new W15(alphabet, null, null, mask, 22);
        GlobalSettings globalSettings = new GlobalSettings(new String[0], 1, 1024, 13, true, true, false, false);

        int[] bucketSizes;
        int maxBucketSize;
        try (FastqIdReader fastqIdReader = new FastqIdReader(reads);
             SequenceSupplier<Integer, byte[]> sup = new SequenceSupplier<Integer, byte[]>(
                     fastqIdReader, alphabet::translateRead, globalSettings.KEEP_IN_MEMORY)) {
            StatisticsEstimator statisticsEstimator = new StatisticsEstimator(sup, encoder, 10_000);
            bucketSizes = statisticsEstimator.getEstimatedBucketSizes();
            maxBucketSize = statisticsEstimator.getMaxBucketSize();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println("Maximum bucket size: " + maxBucketSize);
        for (int i = 0; i < bucketSizes.length; i++) {
            System.out.println("Bucket " + i + ": " + bucketSizes[i]);
        }
    }

    @Test
    public void testBucketSizeEstimatorFasta() {
//        Path reads = Utilities.getFile("F:\\Studium\\Master\\semester5\\thesis\\data\\NCBI\\nr50\\nr50_preprocessed.fsa", true);
        Path reads = Utilities.getFile("F:\\Studium\\Master\\semester5\\thesis\\data\\NCBI\\nr90\\nr90_preprocessed.fsa.gz", true);

        boolean[] mask = parseMask("1111111111111");
        ReducedAlphabet alphabet = new CustomAlphabet("[L][A][GC][VWUBIZO*][SH][EMX][TY][RQ][DN][IF][PK]");
        Encoder encoder = new W15(alphabet, null, null, mask, 22);
        GlobalSettings globalSettings = new GlobalSettings(new String[0], 1, 1024, 13, false, true, false, false);

        int[] bucketSizes;
        int maxBucketSize;
        try (FastaIdReader fastaIdReader = new FastaIdReader(reads);
             SequenceSupplier<Integer, byte[]> sup = new SequenceSupplier<Integer, byte[]>(
                     fastaIdReader, alphabet::translateDBSequence, globalSettings.KEEP_IN_MEMORY)) {
            StatisticsEstimator statisticsEstimator = new StatisticsEstimator(sup, encoder, 10_000);
            bucketSizes = statisticsEstimator.getEstimatedBucketSizes();
            maxBucketSize = statisticsEstimator.getMaxBucketSize();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println("Maximum bucket size: " + maxBucketSize);
        for (int i = 0; i < bucketSizes.length; i++) {
            System.out.println("Bucket " + i + ": " + bucketSizes[i]);
        }
    }
}
