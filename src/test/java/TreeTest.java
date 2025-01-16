import org.husonlab.diamer2.taxonomy.Tree;
import org.junit.Test;
import static org.junit.Assert.*;

import org.husonlab.diamer2.taxonomy.Node;

import java.util.ArrayList;
import java.util.Arrays;

public class TreeTest {
    @Test
    public void treeMethodsTest() {
        Tree tree = new Tree();
        // Root node
        Node root = new Node(1, null, "root", "Root"); tree.idMap.put(1, root);
        Node eukaryota = new Node(2759, root, "domain", "Eukaryota"); tree.idMap.put(2759, eukaryota);
        Node animalia = new Node(33208, eukaryota, "kingdom", "Animalia"); tree.idMap.put(33208, animalia);
        Node chordata = new Node(9340, animalia, "phylum", "Chordata"); tree.idMap.put(9340, chordata);
        Node mammalia = new Node(40674, chordata, "class", "Mammalia"); tree.idMap.put(40674, mammalia);
        // class with non-standard rank
        Node yinotheria = new Node(9347, mammalia, "subclass", "Yinotheria"); tree.idMap.put(9347, yinotheria);
        Node monotremes = new Node(9255, yinotheria, "order", "Monotremes"); tree.idMap.put(9255, monotremes);
        Node carnivora = new Node(15903, mammalia, "order", "Carnivora"); tree.idMap.put(15903, carnivora);
        Node felidae = new Node(9685, carnivora, "family", "Felidae"); tree.idMap.put(9685, felidae);
        Node panthera = new Node(6024, felidae, "genus", "Panthera"); tree.idMap.put(6024, panthera);
        Node pantheraLeo = new Node(9646, panthera, "species", "Panthera leo"); tree.idMap.put(9646, pantheraLeo);
        Node pantheraTigris = new Node(9705, panthera, "species", "Panthera tigris"); tree.idMap.put(9705, pantheraTigris);
        Node felis = new Node(5828, felidae, "genus", "Felis"); tree.idMap.put(5828, felis);
        Node felisCatus = new Node(9685, felis, "species", "Felis catus"); tree.idMap.put(9685, felisCatus);
        Node canidae = new Node(9615, carnivora, "family", "Canidae"); tree.idMap.put(9615, canidae);
        Node canis = new Node(9616, canidae, "genus", "Canis"); tree.idMap.put(9616, canis);
        Node canisLupus = new Node(9615, canis, "species", "Canis lupus"); tree.idMap.put(9615, canisLupus);
        Node vulpes = new Node(40641, canidae, "genus", "Vulpes"); tree.idMap.put(40641, vulpes);
        Node vulpesVulpes = new Node(18893, vulpes, "species", "Vulpes vulpes"); tree.idMap.put(18893, vulpesVulpes);
        Node primates = new Node(9443, mammalia, "order", "Primates"); tree.idMap.put(9443, primates);
        Node hominidae = new Node(9605, primates, "family", "Hominidae"); tree.idMap.put(9605, hominidae);
        Node homo = new Node(9601, hominidae, "genus", "Homo"); tree.idMap.put(9601, homo);
        Node homoSapiens = new Node(9606, homo, "species", "Homo sapiens"); tree.idMap.put(9606, homoSapiens);
        Node pan = new Node(9593, hominidae, "genus", "Pan"); tree.idMap.put(9593, hominidae);
        Node panTroglodytes = new Node(9598, pan, "species", "Pan troglodytes"); tree.idMap.put(9598, panTroglodytes);
        Node aves = new Node(8782, chordata, "class", "Aves"); tree.idMap.put(8782, aves);
        Node passeriformes = new Node(9158, aves, "order", "Passeriformes"); tree.idMap.put(9185, passeriformes);
        Node arthropoda = new Node(6656, chordata, "phylum", "Arthropoda"); tree.idMap.put(6656, arthropoda);
        Node mollusca = new Node(6447, animalia, "phylum", "Mollusca"); tree.idMap.put(6447, mollusca);
        Node cnidaria = new Node(6073, animalia, "phylum", "Cnidaria"); tree.idMap.put(6073, cnidaria);
        Node plantae = new Node(33090, eukaryota, "kingdom", "Plantae"); tree.idMap.put(33090, plantae);
        Node magnoliophyta = new Node(3392, plantae, "phylum", "Magnoliophyta"); tree.idMap.put(3392, magnoliophyta);
        Node coniferophyta = new Node(3079, plantae, "phylum", "Coniferophyta"); tree.idMap.put(3079, coniferophyta);

        // autoFindRoot()
        assertEquals(tree.autoFindRoot(), root);

        // findLCA()
        assertEquals(tree.findLCA(homoSapiens, magnoliophyta), eukaryota);
        assertEquals(tree.findLCA(homoSapiens, pantheraLeo), mammalia);

        // pathToRoot()
        assertEquals(
                tree.pathToRoot(homoSapiens),
                new ArrayList<>(Arrays.asList(homoSapiens, homo, hominidae, primates, mammalia, chordata, animalia, eukaryota, root))
        );

        // reduceToStandardRanks()
        tree.reduceToStandardRanks();
        assertEquals(
                tree.idMap.get(9347),   // non-standard rank subclass yinotheria
                mammalia
        );

        // getWeightedSubtree()
        Tree subtree = new Tree();
        Node root1 = new Node(1, null, "root", "Root"); subtree.idMap.put(1, root1);
        Node animalia1 = new Node(33208, root, "kingdom", "Animalia"); subtree.idMap.put(33208, animalia1);
        Node chordata1 = new Node(9340, animalia1, "phylum", "Chordata"); subtree.idMap.put(9340, chordata1);
        Node mammalia1 = new Node(40674, chordata1, "class", "Mammalia"); subtree.idMap.put(40674, mammalia1);
        mammalia1.setWeight(5);
        Node primates1 = new Node(9443, mammalia1, "order", "Primates"); subtree.idMap.put(9443, primates1);
        Node hominidae1 = new Node(9605, primates1, "family", "Hominidae"); subtree.idMap.put(9605, hominidae1);
        Node homo1 = new Node(9601, hominidae1, "genus", "Homo"); subtree.idMap.put(9601, homo1);
        Node homoSapiens1 = new Node(9606, homo1, "species", "Homo sapiens"); subtree.idMap.put(9606, homoSapiens1);
        homoSapiens1.setWeight(1);
        Node aves1 = new Node(8782, chordata1, "class", "Aves"); subtree.idMap.put(8782, aves1);
        aves1.setWeight(2);
        subtree.autoFindRoot();
        assertEquals(
                tree.getWeightedSubTree(new ArrayList<>(Arrays.asList(
                        new int[]{9606, 1},     // Homo Sapiens
                        new int[]{8782, 2},     // Aves
                        new int[]{40674, 3},    // Mammalia
                        new int[]{9347, 2}))),  // Yinotheria (non-standard rank "subclass")
                subtree
        );
    }
}