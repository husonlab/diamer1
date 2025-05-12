# DIAMER
The Double-Indexed k-mer Taxonomic Classifier DIAMER is a bioinformatics tool
to taxonomically classify long reads of next generation sequencing technologies
by comparison to a protein reference database.

This tool was created as part of the master thesis by Noel Kubach in 2025.

# Usage
To classify a FASTQ dataset of long reads with DIAMER, four steps have to be done:
1) A reference database has to be prepared
2) The reference database has to be indexed
3) The reads have to be indexed
4) The reads are assigned to the reference taxonomy

Step 1 and 2 have not to be repeated for further datasets.

## 1. Database preparation
DIAMER uses the NCBI protein blast database as reference.
Either the full NR database can be used or smaller, clustered versions provided by [MEGAN](https://software-ab.cs.uni-tuebingen.de/download/megan7/welcome.html).
### Downloading the taxonomy
For either version of NR, the NCBI taxonomy is required. It can be downloaded from the FTP server of NCBI:
* [Taxonomy](https://ftp.ncbi.nlm.nih.gov/pub/taxonomy/)
  (the `nodes.dmp` and `names.dmp` file from the `taxdump` archive are required)

### Full NR
#### Downloading NR
The NR database and the required accession2taxid mapping files can be downloaded
from the FTP server of NCBI.
* [NR](https://ftp.ncbi.nlm.nih.gov/blast/db/FASTA/)
* [accession2taxid](https://ftp.ncbi.nlm.nih.gov/pub/taxonomy/accession2taxid/)
(the `prot.accession2taxid.FULL` together with the `dead_prot.accession2taxid` are recommended)
  * alternatively the [MEGAN mapping file](https://software-ab.cs.uni-tuebingen.de/download/megan7/welcome.html)
  for the full NR can be downloaded and used as described [here](#prepare-the-database)
#### Preparing NR
The `accession2taxid` mapping files are used to replace all headers in the NR FASTA with the taxId of the LCA
of all organisms that contain the sequence.

The input database FASTA file can be gzipped or not, the output will always be gzipped.

````shell
java -jar diamer.jar --preprocess -no <path to nodes.dmp> -na <path to names.dmp> <path to nr.fsa.gz> <output file> <path to prot.accession2taxid.FULL> <path to dead_prot.accession2taxid> [<paths to further mapping files>...]
````

### Clustered NR (MEGAN)
[MEGAN](https://software-ab.cs.uni-tuebingen.de/download/megan7/welcome.html) provided clustered versions
of the NR database, that are clustered at either 50% (NR50) or 90% (NR90) sequence identity.
The corresponding FASTA files and mapping database files can be found on the [homepage of MEGAN](https://software-ab.cs.uni-tuebingen.de/download/megan7/welcome.html).

Note: The zipped MEGAN database files have to be extracted before they can be used.

#### Prepare the Database
To annotate the NR90 or NR50 database with the associated NCBI taxIds, the following command is used:

````shell
java -jar diamer.jar --preprocess -no <path to nodes.dmp> -na <path to names.dmp> <path to database> <>
````

## 2. Indexing the reference database
The prepared reference database has to be indexed. In this step, a sorted list of all k-mers from the reference database,
mapped to the LCA of all organisms they occur in is created. The list is stored in 1024 separate bucket files.

````shell
java -jar diamer.jar --indexdb [optional arguments] -no <path to nodes.dmp> -na <path to names.dmp> <path to prepared database> <output path>
````

## 3. Indexing the reads
Similar to the database index, a read index is created that consists of a sorted list of all k-mer-sequenceID
pairs. This list is again stored in the form of 1024 bucket files that correspond to the bucket files of the
database index.

The input FASTQ file can be gzipped.

````shell
java -jar diamer.jar --indexreads [optional arguments] <path to FASTQ> <output path>
````

## 4. Assigning the reads
In this final step, the database and reads index are compared and the reads assigned to a taxon depending on the k-mers
that match to the reference database and the selected assignment algorithm.

````shell
java -jar diamer.jar --assignreads [optional arguments] <path to database index> <path to reads index> <output path>
````

# Output files
DIAMER produces four different output files:
1) `raw_assignments.tsv`
   * the first row holds the number of remaining rows
   * one row per read
   * tab-separated columns: read header, space separared list of k-mer matches
   * each entry in the space separated list has the format taxId:k-mer count
   * Similar to the default Kraken 2 output file
    ````
    @4d4262d4-c...	2:38 131567:37 1280:9 1239:3 1:3 2249226:2 2053627:2 ...
    @cd4133d1-f...	131567:22 1:8 1613:3 2:2 4567:2 203682:1 3379134:1 ...
    ...
    ````
2) `per_read_assignment.tsv`
   * the first row holds the number of data rows
   * the second row contains column names
     * first column for a running index of the reads (starting at 0)
     * followed by one column per assignment algorithm
   * from row 3 on: one row per read
     * cell format for assignment algorithms: (rank) label (taxId)
   ````
   1160526
   ReadID	OVO (0.20) read count	OVO (0.20) read count (norm. kmers)	OVA (1.00) read count	OVA (1.00) read count (norm. kmers)	...
   0	(superkingdom) Bacteria (2)	(no rank) cellular organisms (131567)	(species) Staphylococcus aureus (1280)	(species) Photobacterium kishitanii (318456)	...
   ...
   ````