import org.husonlab.diamer.io.ReadAssignmentIO;
import org.husonlab.diamer.io.taxonomy.TreeIO;
import org.husonlab.diamer.main.GlobalSettings;
import org.husonlab.diamer.readAssignment.ReadAssignment;
import org.husonlab.diamer.readAssignment.algorithms.OVA;
import org.husonlab.diamer.readAssignment.algorithms.OVO;
import org.husonlab.diamer.taxonomy.Tree;

import java.nio.file.Path;
import java.util.List;

import static org.husonlab.diamer.io.NCBIReader.readTaxonomy;
import static org.husonlab.diamer.io.ReadAssignmentIO.readRawKrakenAssignment;

public class AnalyzeKrakenResultWithDiamer {
    /**
     * Class for analyzing the kraken output with the algorithms of DIAMER.
     */

    public void analyze_kraken_result() {
        Path output = Path.of("processed_kraken_output");
        GlobalSettings settings = new GlobalSettings(new String[0], null, null, null, output.resolve("run.log"));
        Tree tree = readTaxonomy(Path.of("nodes.dmp"), Path.of("names.dmp"), true);
        ReadAssignment readAssignment = readRawKrakenAssignment(tree, Path.of("kraken_output.txt"), settings);
        readAssignment.addKmerCountsToTree();
        readAssignment.sortKmerCounts();
        ReadAssignmentIO.writeRawAssignment(readAssignment, output.resolve("raw_assignments.tsv"));
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
}
