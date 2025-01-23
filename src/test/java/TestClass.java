import org.husonlab.diamer2.io.seq.FastaReader;
import org.husonlab.diamer2.io.seq.FastqReader;
import org.husonlab.diamer2.io.seq.SequenceSupplier;
import org.husonlab.diamer2.seq.Compressed4BitSequence;
import org.husonlab.diamer2.seq.SequenceRecord;
import org.husonlab.diamer2.seq.alphabet.Base11Alphabet;
import org.husonlab.diamer2.seq.alphabet.converter.AAtoBase11;
import org.husonlab.diamer2.seq.alphabet.converter.DNAtoBase11;
import org.husonlab.diamer2.util.logging.OneLineLogger;
import org.husonlab.diamer2.util.logging.ProgressBar;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class TestClass {

    @Test
    public void test() throws IOException {
        long[] counts = new long[11];
        try (SequenceSupplier<String, Byte> supplier = new SequenceSupplier<>(
                new FastqReader(new File("F:\\Studium\\Master\\semester5\\thesis\\data\\test_dataset\\Zymo-GridION-EVEN-3Peaks-R103-merged.fq")),
                new DNAtoBase11(), false)) {

            ProgressBar progressBar = new ProgressBar(supplier.getFileSize(), 20);
            new OneLineLogger("Counter", 1000).addElement(progressBar);

            SequenceRecord<String, Byte> record;
            while ((record = supplier.next()) != null) {
                progressBar.setProgress(supplier.getBytesRead());
                for (Byte base : record.sequence()) {
                    counts[base]++;
                }
            }
        }

        for (int i = 0; i < 11; i++) {
            System.out.println(i + ": " + counts[i]);
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("C:\\Users\\nk035\\Documents\\diamer2\\statistics\\base11_counts_zymo.txt"))) {
            for (int i = 0; i < 11; i++) {
                writer.write(i + ": " + counts[i] + "\n");
            }
        }
    }

    @Test
    public void test2() throws IOException {
        long[] counts = new long[11];
        try (SequenceSupplier<String, Byte> supplier = new SequenceSupplier<>(
                new FastaReader(new File("F:\\Studium\\Master\\semester5\\thesis\\data\\NCBI\\100\\nr100.fsa")),
                new AAtoBase11(), false)) {

            ProgressBar progressBar = new ProgressBar(supplier.getFileSize(), 20);
            new OneLineLogger("Counter", 1000).addElement(progressBar);

            SequenceRecord<String, Byte> record;
            while ((record = supplier.next()) != null) {
                progressBar.setProgress(supplier.getBytesRead());
                for (Byte base : record.sequence()) {
                    counts[base]++;
                }
            }
        }

        for (int i = 0; i < 11; i++) {
            System.out.println(i + ": " + counts[i]);
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("C:\\Users\\nk035\\Documents\\diamer2\\statistics\\base11_counts_nr100.txt"))) {
            for (int i = 0; i < 11; i++) {
                writer.write(i + ": " + counts[i] + "\n");
            }
        }
    }

    @Test
    public void testCompressed4BitSequence() {
        Compressed4BitSequence sequence = new Compressed4BitSequence(new Base11Alphabet(),
                new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 9, 5, 10, 6, 8, 2});
        System.out.println(sequence);
    }
}
