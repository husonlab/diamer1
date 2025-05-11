//import org.husonlab.diamer.io.seq.FastaReader;
//import org.husonlab.diamer.seq.SequenceRecord;
//import org.junit.Test;
//
//import java.io.BufferedWriter;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.nio.file.Path;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
//import static org.husonlab.diamer.seq.converter.Utilities.encodeAABase11Uniform;
//
//public class AminoAcidFrequencies {
//    @Test
//    public void countAminoAcidFrequencies() {
//        Path path = Path.of("F:\\Studium\\Master\\semester5\\thesis\\data\\NCBI\\100\\nr100.fsa.gz");
//        long[] counts = new long[57];
//        try (FastaReader reader = new FastaReader(path)) {
//            SequenceRecord<String, Character> record;
//            while ((record = reader.next()) != null) {
//                for (char c : record) {
//                    counts[c - 65]++;
//                }
//            }
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        List<Map.Entry<Character, Long>> frequencyList = new ArrayList<>();
//        long total = 0;
//        for (int i = 0; i < counts.length; i++) {
//            if (counts[i] > 0) {
//                frequencyList.add(Map.entry((char)(i + 65), counts[i]));
//                total += counts[i];
//            }
//        }
//        frequencyList.sort((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()));
//        try (BufferedWriter bw = new BufferedWriter(new FileWriter("F:\\Studium\\Master\\semester5\\thesis\\data\\analyses\\amino_acid_frequency\\nr100.tsv"))) {
//            bw.write("Total: " + total + "\n");
//            for (Map.Entry<Character, Long> entry : frequencyList) {
//                bw.write(entry.getKey() + "\t" + entry.getValue() + "\t" + entry.getValue() / (double)total + "\n");
//            }
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    @Test
//    public void countReducedAminoAcidFrequencies() {
//        Path path = Path.of("F:\\Studium\\Master\\semester5\\thesis\\data\\NCBI\\100\\nr100.fsa.gz");
//        long[] counts = new long[11];
//        try (FastaReader reader = new FastaReader(path)) {
//            SequenceRecord<String, Character> record;
//            while ((record = reader.next()) != null) {
//                for (char c : record) {
//                    counts[encodeAABase11Uniform(c)]++;
//                }
//            }
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        List<Map.Entry<Integer, Long>> frequencyList = new ArrayList<>();
//        long total = 0;
//        for (int i = 0; i < counts.length; i++) {
//            if (counts[i] > 0) {
//                frequencyList.add(Map.entry(i, counts[i]));
//                total += counts[i];
//            }
//        }
//        frequencyList.sort((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()));
//        try (BufferedWriter bw = new BufferedWriter(new FileWriter("F:\\Studium\\Master\\semester5\\thesis\\data\\analyses\\amino_acid_frequency\\nr100_base11Uniform.tsv"))) {
//            bw.write("Total: " + total + "\n");
//            for (Map.Entry<Integer, Long> entry : frequencyList) {
//                bw.write(entry.getKey() + "\t" + entry.getValue() + "\t" + entry.getValue() / (double)total + "\n");
//            }
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
//}
