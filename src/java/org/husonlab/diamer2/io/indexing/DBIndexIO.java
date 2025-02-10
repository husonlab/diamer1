package org.husonlab.diamer2.io.indexing;

import org.husonlab.diamer2.io.taxonomy.TreeIO;
import org.husonlab.diamer2.taxonomy.Tree;

import java.nio.file.Path;

public class DBIndexIO extends IndexIO {

    private final Path tree;

    /**
     * Create a new IndexIO object.
     * @param indexFolder path to the index folder
     *                    is missing
     */
    public DBIndexIO(Path indexFolder) {
        super(indexFolder);
        this.tree = indexFolder.resolve("tree.txt");
    }

    public boolean treeExists() {
        return tree.toFile().exists();
    }

    public Tree getTree() {
        return TreeIO.loadTree(tree);
    }
}
