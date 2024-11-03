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

# generate reduced NCBI database dataset
* Extracting the accessions that are part of the nr.fsa file with the [ExtractNrAcessions](src/java/org/husonlab/diamer2/reduceDatasets/ExtractNrAcessions.java) to see if all accessions in the taxon map are required to process the nr.fsa.
  * Extracted 812,194,751 accession ids.
* Further reducing the file to 