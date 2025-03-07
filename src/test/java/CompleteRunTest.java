import org.husonlab.diamer2.io.Utilities;
import org.husonlab.diamer2.main.Main;
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
        boolean assertInbetween = true;
        Path nodesDmp = Utilities.getFile("src/test/resources/database/taxdmp/nodes.dmp", true);
        Path namesDmp = Utilities.getFile("src/test/resources/database/taxdmp/names.dmp", true);
        Path ncbiAccession2Taxid = Utilities.getFile("src/test/resources/database/taxmap/prot.accession2taxid.gz", true);
        Path ncbiAccession2Taxid2 = Utilities.getFile("src/test/resources/database/taxmap/prot.accession2taxid2.gz", true);
        Path db = Utilities.getFile("src/test/resources/database/db.fsa", true);
        Path dbPreprocessed = Utilities.getFile("src/test/resources/test_output/db_preprocessed/db_preprocessed.fsa.gz", false);
        Path dbPreprocessedExpected = Utilities.getFile("src/test/resources/expected_output/db_preprocessed/db_preprocessed.fsa.gz", true);
        Path dbPreprocessedNTExpected = Utilities.getFile("src/test/resources/expected_output/db_preprocessed_nt/test_nt.fsa", true);
        Path reads = Utilities.getFile("src/test/resources/reads/reads.fq", true);
        Path dbIndex = Utilities.getFolder("src/test/resources/test_output/db_index", false);
        Path dbIndexExpected = Utilities.getFolder("src/test/resources/expected_output/db_index", true);
        Path dbIndexSpaced = Utilities.getFolder("src/test/resources/test_output/db_index_spaced", false);
        Path dbIndexSpacedExpected = Utilities.getFolder("src/test/resources/expected_output/db_index_spaced", true);
        Path dbIndexNT = Utilities.getFolder("src/test/resources/test_output/db_index_nt", false);
        Path dbIndexNTExpected = Utilities.getFolder("src/test/resources/expected_output/db_index_nt", true);
        Path readsIndex = Utilities.getFolder("src/test/resources/test_output/reads_index", false);
        Path readsIndexExpected = Utilities.getFolder("src/test/resources/expected_output/reads_index", true);
        Path readsIndexSpaced = Utilities.getFolder("src/test/resources/test_output/reads_index_spaced", false);
        Path readsIndexSpacedExpected = Utilities.getFolder("src/test/resources/expected_output/reads_index_spaced", true);
        Path readsIndexNuc = Utilities.getFolder("src/test/resources/test_output/reads_index_nuc", false);
        Path readsIndexNucExpected = Utilities.getFolder("src/test/resources/expected_output/reads_index_nuc", true);
        Path output = Utilities.getFolder("src/test/resources/test_output/assignment", false);
        Path outputExpected = Utilities.getFolder("src/test/resources/expected_output/assignment", true);
        Path outputSpaced = Utilities.getFolder("src/test/resources/test_output/assignment_spaced", false);
        Path outputSpacedExpected = Utilities.getFolder("src/test/resources/expected_output/assignment_spaced", true);
        Path outputNT = Utilities.getFolder("src/test/resources/test_output/assignment_nt", false);
        Path outputNTExpected = Utilities.getFolder("src/test/resources/expected_output/assignment_nt", true);

        // Preprocess DB
        Main.main(new String[]{"--preprocess", "-t", "12", "--debug", "--statistics",
                "-no", nodesDmp.toString(), "-na", namesDmp.toString(), db.toString(), dbPreprocessed.toString(),
                ncbiAccession2Taxid.toString(), ncbiAccession2Taxid2.toString()});

        ArrayList<ExclusionRule> exclusionRules = new ArrayList<>();
        if (assertInbetween) {
            exclusionRules.add(new ExclusionRule("run.log", new HashSet<>(List.of(1, 3, 6, 10, 11, 13, 14))));
            assertDirectoriesEqual(dbPreprocessedExpected.getParent().toFile(), dbPreprocessed.getParent().toFile(), exclusionRules);
        }

        // Generate DB index
        Main.main(new String[]{
                "--indexdb", "-t", "12", "-b", "1024", "--mask", "111111111111111", "--debug", "--statistics",
                "-no", nodesDmp.toString(), "-na", namesDmp.toString(), "--encoder", "base11",
                dbPreprocessed.toString(), dbIndex.toString()});
        if (assertInbetween) {
            exclusionRules = new ArrayList<>();
            exclusionRules.add(new ExclusionRule("run.log", new HashSet<>(List.of(1, 3, 6, 10, 11, 13, 14))));
            assertDirectoriesEqual(dbIndexExpected.toFile(), dbIndex.toFile(), exclusionRules);
        }

        Main.main(new String[]{
                "--indexdb", "-t", "12", "-b", "127", "--mask", "111111011110011100011", "--debug", "--statistics",
                "-no", nodesDmp.toString(), "-na", namesDmp.toString(),
                dbPreprocessed.toString(), dbIndexSpaced.toString()});
        if (assertInbetween) {
            assertDirectoriesEqual(dbIndexSpacedExpected.toFile(), dbIndexSpaced.toFile(), exclusionRules);
        }

        Main.main(new String[]{
                "--indexdb", "--debug", "--statistics", "--encoder", "base11nuc",
                "-no", nodesDmp.toString(), "-na", namesDmp.toString(),
                dbPreprocessedNTExpected.toString(), dbIndexNT.toString()});
        if (assertInbetween) {
            assertDirectoriesEqual(dbIndexNTExpected.toFile(), dbIndexNT.toFile(), exclusionRules);
        }

        // Generate read index
        String[] args = new String[]{
                "--indexreads", "-t", "12", "-b", "1024", "--mask", "111111111111111", "--debug", "--statistics",
                reads.toString(), readsIndex.toString()};
        Main.main(args);
        if (assertInbetween) {
            exclusionRules = new ArrayList<>();
            exclusionRules.add(new ExclusionRule("run.log", new HashSet<>(List.of(1, 3, 6, 10, 11))));
            assertDirectoriesEqual(readsIndexExpected.toFile(), readsIndex.toFile(), exclusionRules);
        }

        args = new String[]{
                "--indexreads", "-t", "1", "-b", "127", "--keep-in-memory", "--mask", "111111011110011100011", "--debug", "--statistics",
                reads.toString(), readsIndexSpaced.toString()};
        Main.main(args);
        if (assertInbetween) {
            assertDirectoriesEqual(readsIndexSpacedExpected.toFile(), readsIndexSpaced.toFile(), exclusionRules);
        }

        args = new String[]{
                "--indexreads", "--debug", "--statistics", "--encoder", "base11nuc",
                reads.toString(), readsIndexNuc.toString()};
        Main.main(args);
        if (assertInbetween) {
            assertDirectoriesEqual(readsIndexNucExpected.toFile(), readsIndexNuc.toFile(), exclusionRules);
        }

        // Assign reads
        args = new String[]{
                "--assignreads", "-t", "12", "-b", "12", "--debug", "--statistics", "--encoder", "base11",
                "--ovo", "0.2,0.5,0.6,0.8,0.9,1.0",
                "-no", nodesDmp.toString(), "-na", namesDmp.toString(),
                dbIndex.toString(), readsIndex.toString(), output.toString()};
        Main.main(args);
        if (assertInbetween) {
            assertDirectoriesEqual(output.toFile(), outputExpected.toFile(), exclusionRules);
        }
        args = new String[]{
                "--assignreads", "-m", "12", "-t", "1", "-b", "12", "--debug", "--statistics",
                "--ovo", "0.2,0.5,0.6,0.8,0.9,1.0",
                "-no", nodesDmp.toString(), "-na", namesDmp.toString(),
                dbIndexSpaced.toString(), readsIndexSpaced.toString(), outputSpaced.toString()};
        Main.main(args);
        if (assertInbetween) {
            assertDirectoriesEqual(outputSpaced.toFile(), outputSpacedExpected.toFile(), exclusionRules);
        }
        args = new String[]{
                "--assignreads", "--debug", "--statistics", "--encoder", "base11nuc",
                "--ovo", "0.2,0.5,0.6,0.8,0.9,1.0",
                "-no", nodesDmp.toString(), "-na", namesDmp.toString(),
                dbIndexNT.toString(), readsIndexNuc.toString(), outputNT.toString()};
        Main.main(args);
        if (assertInbetween) {
            assertDirectoriesEqual(outputNT.toFile(), outputNTExpected.toFile(), exclusionRules);
        }

        // Compare output with expected output
        File expectedOutput = new File("src/test/resources/expected_output");
        File actualOutput = new File("src/test/resources/test_output");
        exclusionRules = new ArrayList<>();
        exclusionRules.add(new ExclusionRule("report.txt", new HashSet<>(List.of(1, 2, 3))));
        exclusionRules.add(new ExclusionRule("run.log", new HashSet<>(List.of(1, 3, 6, 10, 11, 13, 14))));
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
