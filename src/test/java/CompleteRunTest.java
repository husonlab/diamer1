import org.husonlab.diamer2.indexing.DBIndexer;
import org.husonlab.diamer2.indexing.ReadIndexer;
import org.husonlab.diamer2.io.NCBIReader;
import org.husonlab.diamer2.io.ReadAssignmentIO;
import org.husonlab.diamer2.io.Utilities;
import org.husonlab.diamer2.io.accessionMapping.AccessionMapping;
import org.husonlab.diamer2.io.accessionMapping.NCBIMapping;
import org.husonlab.diamer2.io.seq.FastaReader;
import org.husonlab.diamer2.io.seq.SequenceSupplier;
import org.husonlab.diamer2.main.encoders.K15Base11;
import org.husonlab.diamer2.readAssignment.ReadAssigner;
import org.husonlab.diamer2.readAssignment.ReadAssignment;
import org.husonlab.diamer2.readAssignment.algorithms.OVO;
import org.husonlab.diamer2.taxonomy.Tree;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class CompleteRunTest {
    @Test
    public void runAndCompareWithExpectedResult() throws IOException {
        Path nodesDmp = Utilities.getFile("src/test/resources/database/taxdmp/nodes.dmp", true);
        Path namesDmp = Utilities.getFile("src/test/resources/database/taxdmp/names.dmp", true);
        Path ncbiAccession2Taxid = Utilities.getFile("src/test/resources/database/taxmap/prot.accession2taxid.gz", true);
        Path ncbiAccession2Taxid2 = Utilities.getFile("src/test/resources/database/taxmap/prot.accession2taxid2.gz", true);
        Path db = Utilities.getFile("src/test/resources/database/db.fsa", true);
        Path dbPreprocessed = Utilities.getFile("src/test/resources/test_output/db_preprocessed/db_preprocessed.fsa.gz", false);
        Path reads = Utilities.getFile("src/test/resources/reads/reads.fq", true);
        Path dbIndex = Utilities.getFolder("src/test/resources/test_output/db_index", false);
        Path dbIndexSpaced = Utilities.getFolder("src/test/resources/test_output/db_index_spaced", false);
        Path readsIndex = Utilities.getFolder("src/test/resources/test_output/reads_index", false);
        Path readsIndexSpaced = Utilities.getFolder("src/test/resources/test_output/reads_index_spaced", false);
        Path output = Utilities.getFolder("src/test/resources/test_output/assignment", false);
        Path outputSpaced = Utilities.getFolder("src/test/resources/test_output/assignment_spaced", false);
        boolean[] mask = new boolean[]{
                true, true, true, true, true, true, true, true, true, true, true, true, true, true, true
        };
        boolean[] maskSpaced = new boolean[]{
                true, true, true, true, true, true, false, true, true, true, true, false, false, true, true, true, false, false, false, true, true
        };

        // Preprocess DB
        Tree tree = NCBIReader.readTaxonomy(nodesDmp, namesDmp);
        SequenceSupplier<String, Character> supplier = new SequenceSupplier<>(new FastaReader(db), null, true);
        HashMap<String, Integer> neededAccessions = NCBIReader.extractAccessions(supplier);
        AccessionMapping accessionMapping = new NCBIMapping(
                Arrays.asList(ncbiAccession2Taxid, ncbiAccession2Taxid2),  tree, neededAccessions);
        supplier.reset();
        NCBIReader.preprocessNR(dbPreprocessed, tree, accessionMapping, supplier);

        // Generate DB index
        DBIndexer indexer = new DBIndexer(dbPreprocessed, dbIndex, tree, new K15Base11(mask, 22), 1, 1, 20, 1024, false, true);
        indexer.index();
        tree.resetNodeProperties();
        indexer = new DBIndexer(dbPreprocessed, dbIndexSpaced, tree, new K15Base11(maskSpaced, 22), 1, 1, 4, 127, false, true);
        indexer.index();

        // Generate read index
        ReadIndexer readIndexer = new ReadIndexer(reads, readsIndex, new K15Base11(mask, 22), 1024, 1, 1, 20, true);
        readIndexer.index();
        readIndexer = new ReadIndexer(reads, readsIndexSpaced, new K15Base11(maskSpaced, 22), 127, 1, 1, 4, true);
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
        ReadAssignmentIO.writeForMEGANImport(assignment, output.resolve("megan.tsv"), 1, 1);
        // spaced
        readAssigner = new ReadAssigner(tree, 1, dbIndexSpaced, readsIndexSpaced, new K15Base11(mask, 22));
        assignment = readAssigner.assignReads();
        ReadAssignmentIO.writeRawAssignment(assignment, outputSpaced.resolve("raw_assignments.tsv"));
        assignment.addKmerCounts();
        assignment.runAssignmentAlgorithm(new OVO(tree, 0.2f));
        assignment.runAssignmentAlgorithm(new OVO(tree, 0.8f));
        ReadAssignmentIO.writePerReadAssignments(assignment, outputSpaced.resolve("per_read_assignments.tsv"), false, true);
        ReadAssignmentIO.writePerTaxonAssignments(assignment, outputSpaced.resolve("per_taxon_assignments.tsv"), 1, true);
        ReadAssignmentIO.writeForMEGANImport(assignment, outputSpaced.resolve("megan.tsv"), 1, 1);

        // Compare output with expected output
        File expectedOutput = new File("src/test/resources/expected_output");
        File actualOutput = new File("src/test/resources/test_output");
        ArrayList<ExclusionRule> exclusionRules = new ArrayList<>();
        exclusionRules.add(new ExclusionRule("report.txt", new HashSet<>(List.of(1, 2, 3))));
        exclusionRules.add(new ExclusionRule("preprocessing_report.txt", new HashSet<>(List.of(1, 2, 3))));
        assertDirectoriesEqual(expectedOutput, actualOutput, exclusionRules);
    }

    public void assertDirectoriesEqual(File dir1, File dir2, List<ExclusionRule> exclusionRules) throws IOException {
        if (!dir1.isDirectory() || !dir2.isDirectory()) {
            throw new AssertionError("Both arguments must be directories. Dir1: " + dir1.getPath() + ", Dir2: " + dir2.getPath());
        }

        File[] files1 = dir1.listFiles();
        File[] files2 = dir2.listFiles();

        if (files1.length != files2.length) {
            throw new AssertionError("Directories %s and %s have different number of files. Dir1: %d, Dir2: %d".formatted(dir1.getPath(), dir2.getPath(), files1.length, files2.length));
        }

        for (File file1 : files1) {
            String relativePath = file1.getPath().substring(dir1.getPath().length());
            File file2 = new File(dir2, relativePath);

            if (!file2.exists()) {
                throw new AssertionError("File " + relativePath + " exists in " + dir1.getPath() + " but not in " + dir2.getPath());
            }

            if (file1.isDirectory() && file2.isDirectory()) {
                assertDirectoriesEqual(file1, file2, exclusionRules);
            } else if (file1.isFile() && file2.isFile()) {
                assertFilesEqual(file1, file2, exclusionRules);
            } else {
                throw new AssertionError("File types do not match. File1: " + file1.getPath() +
                        " (" + (file1.isDirectory() ? "directory" : "file") + "), File2: " + file2.getPath() +
                        " (" + (file2.isDirectory() ? "directory" : "file") + ")");
            }
        }
    }

    public void assertFilesEqual(File file1, File file2, List<ExclusionRule> exclusionRules) throws IOException {
        try (BufferedReader reader1 = new BufferedReader(new FileReader(file1));
             BufferedReader reader2 = new BufferedReader(new FileReader(file2))) {

            String line1, line2;
            int lineNum = 1;

            while ((line1 = reader1.readLine()) != null && (line2 = reader2.readLine()) != null) {
                int finalLineNum = lineNum;
                boolean shouldExclude = exclusionRules.stream()
                        .anyMatch(rule -> rule.shouldExclude(file1.getName(), finalLineNum) ||
                                rule.shouldExclude(file2.getName(), finalLineNum));

                if (!shouldExclude && !line1.equals(line2)) {
                    throw new AssertionError("Files differ at line " + lineNum +
                            ". File1: " + file1.getPath() + ", File2: " + file2.getPath() +
                            "\nLine1: " + line1 + "\nLine2: " + line2);
                }
                lineNum++;
            }

            if (reader1.readLine() != null || reader2.readLine() != null) {
                throw new AssertionError("Files have different number of lines. File1: " +
                        file1.getPath() + ", File2: " + file2.getPath());
            }
        }
    }

    public static class ExclusionRule {
        private final String fileName;
        private final Set<Integer> linesToExclude;

        public ExclusionRule(String fileName, Set<Integer> linesToExclude) {
            this.fileName = fileName;
            this.linesToExclude = linesToExclude;
        }

        public boolean shouldExclude(String fileName, int lineNumber) {
            return this.fileName.equals(fileName) && linesToExclude.contains(lineNumber);
        }
    }
}
