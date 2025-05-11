package org.husonlab.diamer.main;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.husonlab.diamer.io.NCBIReader;
import org.husonlab.diamer.readAssignment.algorithms.ClassificationAlgorithm;
import org.husonlab.diamer.readAssignment.algorithms.OVA;
import org.husonlab.diamer.readAssignment.algorithms.OVO;
import org.husonlab.diamer.seq.alphabet.*;
import org.husonlab.diamer.taxonomy.Tree;
import org.husonlab.diamer.util.Pair;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.husonlab.diamer.io.Utilities.getFile;

public class CliUtils {
    /**
     * Print help message.
     * @param options command line options
     */
    public static void printHelp(Options options) {
        HelpFormatter helpFormatter = new HelpFormatter();
        // disable alphabetical sorting of options
        helpFormatter.setOptionComparator(null);
        helpFormatter.printHelp("diamer {--preprocess | --indexdb | --assignreads | --statistics} " +
                "[options] <input> <output>", options);
    }

    /**
     * Check if the number of positional arguments is at least minExpected.
     * @param cli command line arguments
     * @param minExpected minimum number of positional arguments
     */
    public static void checkNumberOfPositionalArguments(CommandLine cli, int minExpected) {
        if (cli.getArgs().length < minExpected) {
            System.err.printf("Expected %d positional arguments, got %d\n", minExpected, cli.getArgs().length);
            System.exit(1);
        }
    }

    /**
     * @param cli command line arguments
     * @return pair of NCBI nodes and names dump files
     */
    public static Pair<Path, Path> getNodesAndNames(CommandLine cli) {
        if (!cli.hasOption("no") || !cli.hasOption("na")) {
            System.err.println("At least one of the required NCBI taxonomy files is missing: " +
                    "nodes.dmp (option -no), names.dmp (option -na)");
            System.exit(1);
        }
        return new Pair<>(getFile(cli.getOptionValue("no"), true), getFile(cli.getOptionValue("na"), true));
    }

    /**
     * @param cli           command line arguments
     * @param defaultMask   default mask
     * @return mask from cli or default mask
     */
    public static boolean[] getMask(CommandLine cli, String defaultMask) {
        return cli.hasOption("mask") ? parseMask(cli.getOptionValue("mask")) : parseMask(defaultMask);
    }

    /**
     * Parse a mask string with 1 and 0 into a boolean array.
     * @param mask mask string
     * @return boolean array
     */
    public static boolean[] parseMask(String mask) {
        mask = mask.replaceAll("^0+", "").replaceAll("0+$", "");
        boolean[] result = new boolean[mask.length()];
        for (int i = 0; i < mask.length(); i++) {
            result[i] = mask.charAt(i) == '1';
        }
        return result;
    }

    /**
     * Get the alphabet specified in the command line arguments.
     */
    public static ReducedAlphabet getAlphabet(CommandLine cli, String defaultAlphabet) {
        ReducedAlphabet alphabet;
        if (!cli.hasOption("alphabet") || cli.getOptionValue("alphabet").equals("uniform11")) {
            alphabet = new CustomAlphabet(defaultAlphabet);
        } else {
            alphabet = new CustomAlphabet(cli.getOptionValue("alphabet"));
        }
        return alphabet;
    }

    public static Tree readTree(CommandLine cli) {
        Pair<Path, Path> nodesAndNames = getNodesAndNames(cli);
        Tree tree;
        try {
            tree = NCBIReader.readTaxonomy(nodesAndNames.first(), nodesAndNames.last(), true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return tree;
    }

    public static List<ClassificationAlgorithm> parseAlgorithms(CommandLine cli) {
        List<ClassificationAlgorithm> algorithms = new ArrayList<>();
        if (cli.hasOption("ovo")) {
            String[] thresholds = cli.getOptionValue("ovo").split(",");
            for (String threshold : thresholds) {
                algorithms.add(new OVO(Float.parseFloat(threshold)));
            }
        } else {
            // default is the algorithm of Kraken 2:
            algorithms.add(new OVO(1.0f));
        }
        if (cli.hasOption("ova")) {
            String[] thresholds = cli.getOptionValue("ova").split(",");
            for (String threshold : thresholds) {
                algorithms.add(new OVA(Float.parseFloat(threshold)));
            }
        }

        return algorithms;
    }
}
