package org.husonlab.diamer.io.indexing;

import org.husonlab.diamer.io.taxonomy.TreeIO;
import org.husonlab.diamer.taxonomy.Tree;

import java.nio.file.Path;

public class DBIndexIO extends IndexIO {

    private final Path tree;

    /**
     * Create a new IndexIO object.
     *
     * @param indexFolder path to the index folder
     * @param nrOfBuckets number of buckets
     */
    public DBIndexIO(Path indexFolder, int nrOfBuckets) {
        super(indexFolder, nrOfBuckets);
        this.tree = indexFolder.resolve("tree.txt");
    }

    /**
     * Checks if a file containing the taxonomic tree exists in the index folder.
     */
    public boolean treeExists() {
        return tree.toFile().exists();
    }

    /**
     * Reads in the taxonomic tree of the index.
     * @return the taxonomic tree.
     */
    public Tree getTree() {
        if (!treeExists()) {
            throw new RuntimeException("Tried to read non existing tree file: " + tree);
        }
        return TreeIO.loadTree(tree);
    }
}
