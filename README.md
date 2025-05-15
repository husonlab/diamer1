# DIAMER
The Double-Indexed k-mer Taxonomic Classifier DIAMER is a bioinformatics tool
to taxonomically classify long reads of next generation sequencing technologies
by comparison to a protein reference database.

This tool was created as part of the master thesis by Noel Kubach in 2025.

# Download
A precompiled .jar file can be downloaded [here](out/artifacts/diamer_jar/diamer.jar).

# Requirenments
DIAMER needs a Java Runtime Environment (version >= 23).

At least 16 GB of RAM and about 500 GB of free disk space are recommended.

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
   ReadID	OVO (0.20) kmer count	OVO (0.20) norm. kmers	OVA (1.00) kmer count	OVA (1.00) norm. kmers	...
   0	(superkingdom) Bacteria (2)	(no rank) cellular organisms (131567)	(species) Staphylococcus aureus (1280)	(species) Photobacterium kishitanii (318456)	...
   ...
   ````
3) `per_taxon_assignment.tsv`
   * one row per taxon in the reference taxonomy
     * the first row contains column names
   * some columns to identify taxa
     * taxId
     * rank
     * label
   * four columns with k-mer counts
     * k-mer count that a taxon has in the reference database
     * k-mer count the taxon has in the reads
     * the cumulative k-mer count of the reads
     * a normalized k-mer count with (k-mer count reads)/(k-mer count db)
   * one column per algorithm (with threshold and weight)
   ````
   node id	rank	label	kmers in database	kmer count	kmer count (cumulative)	OVA (1.00) kmer count	OVA (1.00) kmer count (cumulative)	OVA (1.00) norm. kmer count	OVA (1.00) norm. kmer count (cumulative)	norm. kmer count ...
   1	no rank	root	76325325	45559852	626282761	2926	1132107	12478	1132107	1949 ...
   ...
   ````

## Syntax of Algorithm columns
Multiple of the DIAMER output files contain columns where the results of different algorithms are listed.
The column names contain the name of the algorithm used, the threshold parameter and the weights used.
Additionally, there can be a `(cumulative)` flag to discriminate between raw and accumulated values.

Example: `OVA (1.00) norm. kmer count (cumulative)`
* _one-vs-all_ algorithm
* threshold: 1
* the algorithm used normalized k-mer counts as weights for the subtree
* the value in this column is accumulated over the taxonomic tree

# Syntax & Options
The argument syntax of diamer follows this pattern:
````shell
java [-Xmx<RAM>] -jar diamer.jar <computation task> [options] [input] [output]
````
The memory to use can be specified by the JVM parameter `-Xmx<int>g` in GB.
Depending on the computation task, some options are mandatory.
The number of input and output parameters is task-dependent too.
## Computation Task (mandatory)
DIAMER needs to know which task to perform. This has to be indicated with either of these five flags:
1) `--preprocess`
   * Preprocesses the reference database
   * syntax:
     * `--preprocess [options] <database input> <database output> <mapping file> [further mapping files ...]`
   * mandatory options:
     * `-no`, `-na` for the taxonomic tree
   * database input:
     * reference database in FASTA format
   * database output:
     * output file (will be gzipped)
   * mapping file(s)
     * paths to mapping files (NCBI or MEGAN)
2) `--indexdb`
   * Index a preprocessed reference database
   * syntax:
     * `--indexdb [options] <database input> <index folder>`
   * mandatory options:
     * `-no`, `-na` for the taxonomic tree
   * optional options:
     * `-t`, `--threads` number of threads to use
     * `-b`, `--buckets` number of buckets per iteration
     * `--keep-in-memory` cache input sequence in memory
     * `--mask` specify mask for k-mers (default: `1111111111111`)
     * `--alphabet` supply a custom reduced amino acid alphabet (default: `[L][A][GC][VWUBIZO*][SH][EMX][TY][RQ][DN][IF][PK]`)
     * `--filtering` filter k-mers (default: `c 3`)
     * `--debug` debug mode
     * `--statistics` collect additional statistics (might not work)
     * `--only-standard-ranks` reduce the input taxonomy to the 8 NCBI standard ranks (might not work)
   * database input:
     * preprocessed reference database
   * index folder:
     * path to a folder where the database index will be stored
3) `--indexreads`
   * Generate an index of the reads
   * syntax:
     * `--indexreads [options] <reads FASTQ> <index folder>`
   * optional options:
     * `-t`, `--threads` number of threads to use
     * `-b`, `--buckets` number of buckets per iteration
     * `--keep-in-memory` cache input sequence in memory
     * `--mask` specify mask for k-mers (default: `1111111111111`)
     * `--alphabet` supply a custom reduced amino acid alphabet (default: `[L][A][GC][VWUBIZO*][SH][EMX][TY][RQ][DN][IF][PK]`)
     * `--filtering` filter k-mers (default: `c 3`, recomended `c 0` -> no filtering (faster))
     * `--debug` debug mode
     * `--statistics` collect additional statistics (might not work)
   * reads FASTQ:
     * FASTQ file with the DNA reads
   * index folder:
     * path to the folder where the reads index will be stored
4) `--assignreads`
   * Assign reads to taxa
   * syntax:
     * `--assignreads [options] <reference DB index> <reads index> <output folder>`
   * optional options:
     * `-t`, `--threads` number of threads to use
     * `-b`, `--buckets` number of buckets to process in parallel (will be equal to the number of threads if unspecified)
     * `--debug` debug mode
     * `--statistics` collect additional statistics (might not work)
   * reference DB index
     * path to the folder of the reference database index
   * reads index
     * path to the folder of the reads index
   * output folder
     * path to the folder where the result files will be stored
5) `--analyze-db-index`
   * Calculate some statistics on the database index (might be broken)
   * syntax:
     * `--analyze-db-index <reference DB index> <output folder>`
   * reference database index:
     * path to the index of the reference database
   * output folder:
     * path to a folder where output files will be stored

## Available Options
* `-t`, `--threads`
  * number of threads to use
* `-b`, `--buckets`
  * index generation: number of buckets per iteration
  * read classificationto: number of buckets to be processed in parallel. Cannot exceed the number of threads in this case.
* `--keep-in-memory`
  * cache input sequence in memory
  * the memory that is required for caching the sequence is not considered in the estimation of how many buckets
  to process in parallel. Manually setting `-b` is recommended.
* `--mask`
  * specify a mask for k-mer extraction during indexing
  * default: `1111111111111`
  * spaces can be used to mask amino acids: `111010110100110111`
* `--alphabet`
  * supply a custom reduced amino acid alphabet
  * default: `[L][A][GC][VWUBIZO*][SH][EMX][TY][RQ][DN][IF][PK]`
  * the parser is case sensitive! If your input sequences contain lower case letters, they should be included in the
  alphabet as well.
  * all undefined characters (including `*`) will be interpreted as the end of a sequence
  and split sequences at this position
* `--filtering`
  * filter k-mers during indexing
  * default: `c 3`
  * for read indexing `c 0` (no filtering) is recommended, since it is much faster
  * complexity filtering:
    * syntax: `--filtering c <number>`
    * only keeps k-mers with a complexity higher than `number`
  * probability filtering:
    * syntax: `--filtering p <number>`
    * only keeps k-mers with a probability lower than `number`
    * e.g., `--filtering p 1e-12`
  * complexity maximizer
    * use the minimizer concept to only keep the k-mer with maximal complexity within a window of size `number`
    * syntax: `--filtering cm <number>`
    * the window size cannot be smaller than the k-mer length
  * probability minimizer
    * use the minimizer concept to only keep k-mers with a low probability within a window of size `number`
    * syntax: `--filtering pm <number>`
    * the window size cannot be smaller than the k-mer length