# Test dataset
### `gtdb_proteins_aa_reps_r220_reduced100`
* 100x reduced version of the GTDB dataset.
* every 100th fasta file was skipped during extraction.
* contains 1,131 fasta files.
  * 3,235,289 sequences
  * 980,063,546 15-mers
  * ~ 7.5 GB when stored in longs
  * 22,855,959

# NCBI dataset:
* 2,613,902 taxon ids in taxonomy
* prot.accession2taxid has about 1,300,000,000 entries
  * almost all taxids also occur in the taxonomy
### Source links
* [nr database](https://ftp.ncbi.nlm.nih.gov/blast/db/FASTA/)
* [taxon dumps](https://ftp.ncbi.nlm.nih.gov/pub/taxonomy/)
* [accession mapping](https://ftp.ncbi.nlm.nih.gov/pub/taxonomy/accession2taxid/)

# generate reduced NCBI database dataset
* Extracting the accessions that are part of the nr.fsa file with the [ExtractNrAcessions](src/java/org/husonlab/diamer2/reduceDatasets/ExtractNrAcessions.java) to see if all accessions in the taxon map are required to process the nr.fsa.
  * Extracted 812,194,751 accession ids.
* Reducing the accessions by 100 with the [Reduce100](src/java/org/husonlab/diamer2/reduceDatasets/Reduce100.java) script.
  * 8,121,948 accessions remaining.
* Extracted the sequences that belong to the reduced accessions from the nr.fsa flie -> nr100.fsa with the [ExtractByAccession](src/java/org/husonlab/diamer2/reduceDatasets/ExtractByAccession.java) script.
* Extracted the taxon ids that belong to the reduced accessions from the prot.accession2taxid.gz file -> prot.accession2taxid100.gz with the [ReduceAccession2Taxid](src/java/org/husonlab/diamer2/reduceDatasets/ReduceAccession2Taxid.java) script.

### k-mer counts
* 2,980,952,285 15-mers in the nr100.fsa file.
* 3,918,560 in one bucket

# Missing taxid assignments?
* Can not find the taxonomic id for 12,917 accessions (~ 0.16 %) in the prot.accession2taxidFull.gz file.
  * e.g. MEQ8169148.1 is missing in the prot.accession2taxidFULL.gz file.
  * 7,609 can be found in the dead_prot.accession2taxid.gz file (some redundant).
  * still 6,026 missing (~ 0.07 %).
  * e.g. MDB5073519.1
  * additionally there are three not matching taxonomic classifications:
  * From one closer check, the FULL classification for the differences looks more correct.
  ```
  Label WP_012019010.1 already exists in the tree with a different node.
    existing: CCTCC AB 2018053 (2493633)
    new: Pseudomonas sp. o96-267 (2479853)
  Label WP_024307544.1 already exists in the tree with a different node.
    existing: CCTCC AB 2018053 (2493633)
    new: unclassified Pseudomonas (196821)
  Label WP_024310016.1 already exists in the tree with a different node.
    existing: CCTCC AB 2018053 (2493633)
    new: Pseudomonas sp. P818 (1336207)
  ```
  

# Handling of weired amino acid letters
* Groups of weired amino acids
  * X: unknown amino acid: together with N since it is the most common amino acid
  * U: selenocysteine: together with cysteine
  * B: aspartic acid or asparagine: together with D and N
  * J: leucine or isoleucine: together with L and I
  * Z: glutamic acid or glutamine: together with E and Q
  * O: pyrrolysine: together with lysine


# Reading
[Parallel Programming Tutorial](https://learning.oreilly.com/course/java-multithreading-and/9781804619377/)