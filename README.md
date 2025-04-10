# Major bugfixes, that require to recalculate test results:
* 24.02.2025:
  * Fixed error in read length threshold that would prevent translated protein sequences < 45 amino acids from being used
* 28.02.2025:
  * Fixed bug in Compressed4BitSequence that did not properly store -1 values.

mbac26
2024

````shell
-Dcom.sun.management.jmxremote.port=9000 -Dcom.sun.management.jmxremote.rmi.port=9001 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Djava.net.preferIPv4Stack=true -Djava.rmi.server.hostname=127.0.0.1
ssh -J kubach@sshgw.cs.uni-tuebingen.de kubach@kim.cs.uni-tuebingen.de -L 9000:localhost:9000 -L 9001:localhost:9001 -N
service:jmx:rmi://localhost:9001/jndi/rmi://localhost:9000/jmxrmi
````

| Index                              | Size (gb) |
|------------------------------------|-----------|
| nr90 DIAMOND                       | 195       |
| nr90 DIAMOND c>4                   | 181       |
| nr90 DIAMOND c>5                   | 127       |
| nr90 DIAMOND k<1e-11               | 171       |
| nr90 Etchebest                     | 268       |
| nr90 kraken2                       | 269       |
| nr90 Solis 11                      | 261       |
| nr90 Solis 15                      | 269       |
| nr90 Solis 15, l = 15              | 135       |
| nr90 Solis 15, probability, l = 15 | 169       |
| nr90 Solis 15, probability, l = 17 | 133       |
| nr90 Solis 15, probability, l = 20 | 106       |
| nr90 Solis 15, probability, l = 27 | 75        |
| nr90 Solis 15, complexity, l = 14  | 163       |
| nr90 Solis 15, complexity, l = 15  | 122       |
| nr90 Solis 15, complexity, l = 17  | 85        |
| nr90 Solis 15, complexity, l = 20  | 60        |
| nr90 Segmasker                     | 171       |
| nr Etchebest                       | 581       |
| nr DIAMOND Solis15                 | 581       |
| nr DIAMOND                         | 381       |
| nr DIAMOND p<1e-11                 | 347       |


# Changes since last meeting:
* Added spaced seeds
* Added support for the megan mapping file
  * pro: almost no memory required
  * con: takes much more time (~24 h)
  * con: Only find taxids for about 50% of the nr sequenceRecords in there
* Added OVO algorithm from Julias master thesis

* Changed the way how to extract kmers
  1. reading over file multiple times
  2. options to keep sequenceRecords in memory
  3. keep kmers (feasible?) or 20 letter encoding in memory to safe time

# Spaced Seeds
* -: 111111111111111
* longspaced: 11111101101100111000100001
* [Zhang et al.](https://doi.org/10.1093/bioinformatics/bth037): 11110010101011001101111

# Alphabets
* DIAMOND: [BDEKNOQRXZ][AST][IJLV][G][P][F][Y][CU][H][M][W]
* Base11Uniform: [L][A][GC][VWUBIZO][SH][EMX][TY][RQ][DN][IF][PK]
* Etchebest et. al: [G][P][IV][FYW][A][LMJ][EQRKZOX][NDB][HS][T][CU]
* Solis: [ILMVJ][NPQSXB][DEZ][A][HR][G][T][KO][WY][F][CU]
* kraken2 (15): [ILJ][NQSXB][DEZ][A][MV][G][T][R][P][KO][F][Y][H][W][CU]
* all 20: [LXJ][A][G][V][S][EZ][T][R][DB][I][P][KO][F][N][Q][Y][M][H][W][CU]

# Future:
* Refactor SequenceSupplier
* Set up benchmarking framework to test different assignment algorithms 

# Next time:
* Properly test new NCBIReader.preprocessNRBuffered

# Process:
## 1. Annotating nr database with taxon ids
* ~ 600 gb is enough (05.04.2025)
````shell
java -Xmx1400g -jar diamer2.jar --preprocess -no ../../data/ncbi/taxdmp/nodes.dmp -na ../../data/ncbi/taxdmp/names.dmp -d ../../data/ncbi/nr.fsa -o /beegfs/HPCscratch/noel/preprocessed_nr/nr_preprocessed.fsa --mappings="/beegfs/HPCscratch/noel/dead_prot.accession2taxid.gz,1,2;/beegfs/HPCscratch/noel/prot.accession2taxid.FULL.gz,0,1"
````
### 1.1 Reading nodes and names
### 1.2 reading prot.accession2taxid map
* ConcurrentHashMap:
  * Requires about 192 GB of RAM (for 1,367,645,996 accessions)
  * took about 12m
### 1.3 Annotating nr
* Running over the database
* Add the taxon id of the LCA of all accessions that can be found in the taxonomy
* Skip entries, that do not have an accession in the accession2taxid map
  * accession2taxid: 325,908,528 entries were skipped
  * accession2taxid.FULL + dead_prot.accession2taxid: 583,290 were skipped (811,611,461 remaining)
## 2. Creating database k-mer index
### 2.1 Reading nodes, and names
### 2.2 Indexing
* Indexing whole DB start 21.11.2024 15:35
  * end 22.11.2024 3:35 (12h)
  * ~ 350GB
  * bucket size ~ 13.660.000
  * ~ 3h/128 buckets
* Indexing start 27.11.2024
  * ~45 min per run (64 buckets)
```shell
java -Xmx600g -jar diamer2.jar --indexdb -t 64 -b 128 -no ../../data/ncbi/taxdmp/nodes.dmp -na ../../data/ncbi/taxdmp/names.dmp -d /beegfs/HPCscratch/noel/nr_taxid_full.fsa -o /beegfs/HPCscratch/noel/dbindex/

java -Xmx12g -jar .\diamer2.jar --indexdb -t 16 -b 8 -no F:\Studium\Master\semester5\thesis\data\NCBI\taxdmp\nodes.dmp -na F:\Studium\Master\semester5\thesis\data\NCBI\taxdmp\names.dmp -d F:\Studium\Master\semester5\thesis\data\NCBI\100\nr100_preprocessed.fsa -o F:\Studium\Master\semester5\thesis\data\NCBI\100\index_longspaced
java -Xmx12g -jar .\diamer2.jar --indexreads -t 16 -b 8 -d F:\Studium\Master\semester5\thesis\data\test_dataset\Zymo-GridION-EVEN-3Peaks-R103-merged.fq -o F:\Studium\Master\semester5\thesis\data\test_dataset\index_longspaced
java -Xmx12g -jar .\diamer2.jar --assignreads -t 16 -no F:\Studium\Master\semester5\thesis\data\NCBI\taxdmp\nodes.dmp -na F:\Studium\Master\semester5\thesis\data\NCBI\taxdmp\names.dmp -d F:\Studium\Master\semester5\thesis\data\NCBI\100\index_longspaced F:\Studium\Master\semester5\thesis\data\test_dataset\index_longspaced -o F:\Studium\Master\semester5\thesis\data\test_results
```

## 3. Read Assignment

````shell
java -jar diamer2.jar --assignreads -t 128 -no ../../data/ncbi/taxdmp/nodes.dmp -na ../../data/ncbi/taxdmp/names.dmp -d /beegfs/HPCscratch/noel/dbindex/ /beegfs/HPCscratch/noel/test_dataset/index/ -o /beegfs/HPCscratch/noel/test_dataset/read_assignments.tsv
````

## 4. Statistics
````shell
java -jar diamer2.jar --statistics -no ../../data/ncbi/taxdmp/nodes.dmp -na ../../data/ncbi/taxdmp/names.dmp -d /beegfs/HPCscratch/noel/test_dataset/read_assignments.tsv -o /beegfs/HPCscratch/noel/test_dataset/statistics/
````

* 64 buckets, 16 threads, 5gb
  * ~15 min
  * ~5 gb
* 1024 buckets, 64 threads, 50 gb
  * ~ 30 min
  * ~ 32 gb

# Time
## NR
### preprocess:
````shell
java -Xmx200g -jar ~/Documents/diamer2/diamer2.jar --preprocess -no ~/Documents/ncbi/taxdmp/nodes.dmp -na ~/Documents/ncbi/taxdmp/names.dmp nr.gz preprocess_ncbi/nr_preprocessed_ncbi.fsa ~/Documents/ncbi/taxmapping/dead_prot.accession2taxid.FULL.gz ~/Documents/ncbi/taxmapping/prot.accession2taxid.FULL.gz
````
~ 5h
### indexdb
````shell
java -Xmx200g -jar ~/Documents/diamer2/diamer2.jar --indexdb -t 16 -b 16 -no ~/Documents/ncbi/taxdmp/nodes.dmp -na ~/Documents/ncbi/taxdmp/names.dmp nr_preprocessed_ncbi.fsa index_longspaced/
````


## NR 50 Canterbury
### preprocess:
~ 30 min
### indexdb
````shell
java -Xmx50g -jar ~/Documents/diamer2/diamer2.jar --indexdb --keep-in-memory -t 16 -b 16 -no ~/Documents/ncbi/taxdmp/nodes.dmp -na ~/Documents/ncbi/taxdmp/names.dmp nr50_preprocesses.fsa index_longspaced/
````
~ 2:35h
### indexreads
````shell
java -Xmx50g -jar ~/Documents/diamer2/diamer2.jar --indexreads --keep-in-memory -t 16 -b 16 Zymo-GridION-EVEN-3Peaks-R103-merged.fq index_longspaced/
````
~ 40 GB would have been enough
~ 2 h
````shell
java -Xmx80g -jar ~/Documents/diamer2/diamer2.jar --indexreads --keep-in-memory -t 16 -b 64 Zymo-GridION-EVEN-3Peaks-R103-merged.fq index_longspaced/
````
~ 2 h
### assignreads
````shell
java -Xmx50g -jar ~/Documents/diamer2/diamer2.jar --assignreads -t 16 -no ~/Documents/ncbi/taxdmp/nodes.dmp -na ~/Documents/ncbi/taxdmp/names.dmp ../ncbi/nr50/index_longspaced ../test_dataset/index_longspaced/ read_assignment
````
~ 30 GB
~ 20 min


## Indexing Reads
* with 5GB, 16 threads and 8 buckets in one run:
  * ~ 5h
* with 200GB, 32 threads and 128 buckets in one run:
  * Started 10:45
  * Finished 11:25, but only because file system was slow for about 20 min
* Started 28.11.2024 14:05
````shell
java -Xmx100g -jar diamer2.jar --indexreads -t 32 -b 128 -d /beegfs/HPCscratch/noel/test_dataset/Zymo-GridION-EVEN-3Peaks-R103-merged.fq -o /beegfs/HPCscratch/noel/test_dataset/index/ 
````

# Kraken2
## DB setup
````shell
kraken2-build --protein --download-library nr --db kraken_db
kraken2-build --threads 64 --download-taxonomy --protein --db kraken_db
kraken2-build --build --threads 64 --db kraken_db
kraken2 --db kraken_db Zymo-GridION-EVEN-3Peaks-R103-merged.fq > 
````

# Questions:

# TODO
* try to use duskmaster to mask low complexity regions
* Use higher level than sequenceRecord to store sequenceRecords in memory for all but the first run to save computation
* Bloom filter for usefull kmers
* use weight for OVO that consinders the total amino acids in the database per rank to account for database bias
  * At least for composition estimates based on the total kmer assignments that should make sense.

# Hashes

* kmer encoding: 52 bits
* taxid: 22 bits

| bucket 0 - 9 (10) | long 0 - 41 (42) | long 42 - 63 (22) |
|-------------------|------------------|-------------------|
| kmer (0 - 9)      | kmer (10 - 52)   | taxid (0 - 21)    |
| 2^10              | 2^42             | 2^22              |

# 11 Letter alphabet

| AA     | Frequency (%) |
|--------|---------------|
| KRQEDN | 31.2          |
| LIV    | 17.4          |
| S      | 8.1           |
| G      | 7.4           |
| A      | 7.4           |
| T      | 6.2           |
| P      | 5             |
| F      | 4             |
| Y      | 3.3           |
| C      | 3.3           |
| H      | 2.9           |
| M      | 1.8           |
| W      | 1.3           |
[source](https://legacy.nimbios.org//~gross/bioed/webmodules/aminoacid.htm)


# GTDB
### `gtdb_proteins_aa_reps_r220_reduced100`
* 100x reduced version of the GTDB dataset.
* every 100th fasta file was skipped during extraction.
* contains 1,131 fasta files.
  * 3,235,289 sequenceRecords
  * 980,063,546 15-mers
  * ~ 7.5 GB when stored in longs
  * 22,855,959

# NCBI dataset:
* nr.fsa: 812,194,751 sequenceRecords
* nodes.dmp: 2,613,902 taxon ids in taxonomy
* prot.accession2taxid: 1,381,601,160 entries
  * almost all taxids also occur in the taxonomy
* prot.accession2taxid.FULL: 7,482,355,539 entries
* dead_prot.accession2taxid: 158,629,501 entries

# TestClass dataset:

* 4,642,104 lines: 1,160,526 readAssignment

# Bash commands
## Extracting n sequenceRecords from a fasta file
````shell
awk '/^>/ {n++} n>1000 {exit} {print}' input.fasta > output.fasta
````
## Extracting every nth sequenceRecord
````shell
awk 'BEGIN {n=0} /^>/ {n++; if (n % 100 == 0) {p=1} else {p=0}} p' input.fasta > output.fasta
````
## Zipping folder with content
````shell
zip -r output.zip input/
````

## Source links
* [nr database](https://ftp.ncbi.nlm.nih.gov/blast/db/FASTA/)
* [taxon dumps](https://ftp.ncbi.nlm.nih.gov/pub/taxonomy/)
* [accession mapping](https://ftp.ncbi.nlm.nih.gov/pub/taxonomy/accession2taxid/)
* [test dataset](https://lomanlab.github.io/mockcommunity/r10.html)
* [Megan mapping file](https://software-ab.cs.uni-tuebingen.de/download/megan7/welcome.html)

## generate reduced NCBI database dataset
* Extracting the accessions that are part of the nr.fsa file with the [ExtractNrAcessions](src/java/org/husonlab/diamer2/reduceDatasets/ExtractNrAcessions.java) to see if all accessions in the taxon map are required to process the nr.fsa.
  * Extracted 812,194,751 accession ids.
* Reducing the accessions by 100 with the [Reduce100](src/java/org/husonlab/diamer2/reduceDatasets/Reduce100.java) script.
  * 8,121,948 accessions remaining.
* Extracted the sequenceRecords that belong to the reduced accessions from the nr.fsa flie -> nr100.fsa with the [ExtractByAccession](src/java/org/husonlab/diamer2/reduceDatasets/ExtractByAccession.java) script.
* Extracted the taxon ids that belong to the reduced accessions from the prot.accession2taxid.gz file -> prot.accession2taxid100.gz with the [ReduceAccession2Taxid](src/java/org/husonlab/diamer2/reduceDatasets/ReduceAccession2Taxid.java) script.


### k-mer counts
* 2,980,952,285 15-mers in the nr100.fsa file.
* 3,918,560 in one bucket

# Missing taxid assignments?
* prot.accession2taxid: can not find the accessions for 325,908,528 entries
  * [MEX9938374.1](https://www.ncbi.nlm.nih.gov/protein/MEX9938374.1) is missing while [KJX92028.1](https://www.ncbi.nlm.nih.gov/protein/KJX92028.1) is there - why?
* prot.accession2taxidFull.gz: 12,917 accessions (~ 0.16 %) missing
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
  * O: pyrrolysine: together with lysine K


# alternative reduced alphabet
1. L
2. A

# Benchmarking
## kmer - taxon rank mapping
* Used the annotated nr_taxid.fsa file (~ 486 million sequenceRecords)

````
[Indexer] Size of bucket 0: 10964242
[Indexer] Size of bucket 1: 10971128
[Indexer] Size of bucket 2: 10969360
[Indexer] Size of bucket 3: 10969499
[Indexer] Size of bucket 4: 10972296
[Indexer] Size of bucket 5: 10970629
[Indexer] Size of bucket 6: 10968862
[Indexer] Size of bucket 7: 10962933
[Indexer] Size of bucket 8: 10966068
[Indexer] Size of bucket 9: 10971730
[Indexer] Number of unprocessed FASTAs: 0
subsection  34
forma specialis 83212
superorder  63270
varietas  175991
subfamily 296583
cohort  45691
section 18640
forma 44613
parvorder 20053
superkingdom  2591119
isolate 45143
subkingdom  45118
superclass  6783
subgenus  177214
infraclass  62969
species group 242150
class 1509032
subtribe  9700
serotype  3982
order 1414593
strain  2349125
biotype 156
suborder  188899
infraorder  64962
no rank 7051012
superfamily 75574
kingdom 294031
genotype  9351
phylum  1076659
species subgroup  33841
subcohort 1149
genus 10625872
species 75837787
serogroup 69
tribe 146545
series  574
subphylum 34501
subclass  104081
family  2855692
subspecies  517114
clade 1563863
````

# Codons
````
AAA	Lys	K	Lysine
AAC	Asn	N	Asparagine
AAG	Lys	K	Lysine
AAT	Asn	N	Asparagine
ACA	Thr	T	Threonine
ACC	Thr	T	Threonine
ACG	Thr	T	Threonine
ACT	Thr	T	Threonine
AGA	Arg	R	Arginine
AGC	Ser	S	Serine
AGG	Arg	R	Arginine
AGT	Ser	S	Serine
ATA	Ile	I	Isoleucine
ATC	Ile	I	Isoleucine
ATG	Met	M	Methionine
ATT	Ile	I	Isoleucine
CAA	Gln	Q	Glutamine
CAC	His	H	Histidine
CAG	Gln	Q	Glutamine
CAT	His	H	Histidine
CCA	Pro	P	Proline
CCC	Pro	P	Proline
CCG	Pro	P	Proline
CCT	Pro	P	Proline
CGA	Arg	R	Arginine
CGC	Arg	R	Arginine
CGG	Arg	R	Arginine
CGT	Arg	R	Arginine
CTA	Leu	L	Leucine
CTC	Leu	L	Leucine
CTG	Leu	L	Leucine
CTT	Leu	L	Leucine
GAA	Glu	E	Glutamic_acid
GAC	Asp	D	Aspartic_acid
GAG	Glu	E	Glutamic_acid
GAT	Asp	D	Aspartic_acid
GCA	Ala	A	Alanine	
GCC	Ala	A	Alanine	
GCG	Ala	A	Alanine	
GCT	Ala	A	Alanine	
GGA	Gly	G	Glycine
GGC	Gly	G	Glycine
GGG	Gly	G	Glycine
GGT	Gly	G	Glycine
GTA	Val	V	Valine
GTC	Val	V	Valine
GTG	Val	V	Valine
GTT	Val	V	Valine
TAA	Stp	O	Stop
TAC	Tyr	Y	Tyrosine
TAG	Stp	O	Stop
TAT	Tyr	Y	Tyrosine
TCA	Ser	S	Serine
TCC	Ser	S	Serine
TCG	Ser	S	Serine
TCT	Ser	S	Serine
TGA	Stp	O	Stop
TGC	Cys	C	Cysteine
TGG	Trp	W	Tryptophan
TGT	Cys	C	Cysteine
TTA	Leu	L	Leucine
TTC	Phe	F	Phenylalanine
TTG	Leu	L	Leucine
TTT	Phe	F	Phenylalanine
````

# Parallelization
[Parallel Programming Tutorial](https://learning.oreilly.com/course/java-multithreading-and/9781804619377/)
## Introduction
### Thread start, join, name
* ``thread.start()`` threads have to be started.
* ``thread.join()`` if a thread is joined to the main thread, the main thread waits until the thread is finished
* ``thread.setName()`` threads can have names
* ``Thread.sleep()`` hreads can be put to sleep

### Priority and State
* ``thread.setPriority()`` can be used to define a priority for a thread to make it likelier to be scheduled.
* ``thread.getState()`` threads can have different states:
  * __NEW__ setup but not running
  * __RUNNABLE__
  * __BLOCKED__
  * __WAITING__
  * __TIMED_WAITING__
  * __TERMINATED__

### Thread Groups
* ``ThreadGroup`` Threads can be grouped
  * The priority of grouped threads can be changed simultaneously
  * Grouped threads can be interrupted all at once
  * ``new Thread(group, Runnable)`` the thread group can be specified with the instantiation of a thread.
  * Can be extended to add custom exception handling for uncaught exceptions (e.g. Runtime Exceptions).

### Daemon Threads
* ``thread.setDaemon(true)`` The JVM exits once all non-daemon threads have terminated
* The JVM will wait for all non-daemon threads before it exits (they will block the exit of a program)

### Exception handling on thread level
* ``Thread.setDefaultUncaughtExceptionHandler()`` All uncaught Runtime Exceptions can also be handled on the thread level.
* A handler can also be implemented by extending the ThreadGroup class

## Synchronization and synchronization primitives

### Synchronization with the synchronized keyword
* ``synchronized (obj) {...}`` can be used to encapsulate a code block that should run mutually exclusive on one thread.
  * The keyword will try to own the objects monitor lock.
    * If the lock is owned by another thread, the current thread places itself on the wait set of this object and waits.
    * Once the monitor lock is free, it can continue execution.
  * For this to work, the synchronized keyword has to be used on the same object for all synchronized threads.
  * This block can than only be executed by one thread at a time.
  * for all other threads, synchronized blocks on this object are blocked.
* ``obj.wait()`` can be used to release the object again from __within__ a synchronized block
  * This makes sense, if e.g. the list that is consumed by a consumer is empty.
  * The thread that called the ``wait()`` method will then wait
  * ``obj.notify()`` or ``obj.notifyAll()`` can be used on another thread to release waiting threads.
  * The waiting threads will then be released in the order they called ``obj.wait()``
  * ``obj.wait(time)`` can also be combined with an amount of time the object will wait to be notified, before continuing the execution.
  * Once a thread is notified, it still has to wait until the notifying thread releases the lock.

### ``ReentrantLock()``
* A ``ReentrantLock()`` is an implementation of a ``Lock()`` object that can be used to define a lock state across multiple threads.
* ``lockObject.lock()`` can be used to lock the object and cause other threads to wait at their ``lockObject.lock()`` statements.
* ``lockObject.unlock()`` can be used to release the lock again and allow other threads to lock it.
* The Object can be locked and unlocked multiple times by one thread (_Reentrant_).
* ``lockObject.tryLock()`` tries to lock the object (``-> true``) and does not wait if the lock fails (``-> false``)
* ``lockObject.tryLock(time)`` can be combined with a time to specify how long the thread should wait and try to get a lock.

### Conditions for Locks
* ``Condition condition = lockObject.newCondition()`` Lock objects can create new conditions, that can be used in the same way as the wait() notify() construct with the synchronize keyword.
* ``condition.await()`` can be used to pause a thread until the condition is signaled by an other thread.
* ``condition.signal()`` and ``condition.signalAll()`` can be used to let the thread continue again.
* In the contrast to ``obj.wait()`` and ``obj.notify()`` multiple conditions can be defined for one lock.

### ReadWriteLock
```java
ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
Lock readLock = readWriteLock.readLock();
Lock writeLock = readWriteLock.writeLock();
```
* ReadWrite locks can be used to distinguish between threads that only read and threads that write data.
* The writeLock will thereby have a higher priority than the readLock.

### Semaphores
* Semaphores can be used to manage a pool of a specified size (e.g. 5) among multiple threads.
* ``semaphore.acquire()`` locks one entity of the semaphore by a process.
* ``semaphore.release()`` releases the entity again.
* The maximum amount of parallel acquisitions can be defined in the instantiation of the semaphore.
* can e.g. be used to limit the total number of threads working at the same time.
* can also be used with a count of 0 during instantiation.
  * Ith ca then be used to lock a thread ``semaphore.acquire()`` until another thread releases it ``semaphore.release()``.

### CountDownLatch
* similar to a semaphore, but only counts down.
* can be used in algorithms that divide a problem in subproblems that are run on multiple threads.
* ``countDownLatch.countDown()`` can be used by a thread to indicate that it finished.
* ``countDownLatch.await()`` can be used (e.g. in the main thread) to wait until the specified number of ``countDown()`` calls accumulated.

### BarrierRelease
* can be used to stop multiple Threads at the same execution state and wait for the other threads to reach this state.
* ``cyclicBarrier.await()`` will wait until a specified (``new CyclicBarrier(4)``) number of threads reach the same state before the barrier is released and all waiting threads continue simultaneously.
* ``new CyclicBarrier(4, Runnable)`` a runnable can be supplied to be executed upon each release.

### Phaser
* similar to cyclic barrier
  * Has more flexibility than the cyclic barrier
* ``phaser.register()`` increments parties (to wait for) by one
  * A thread can register itself in the phaser
* ``phaser.arriveAndAwaitAdvance`` same as ``await()`` in cyclic barrier
* ``phaser.getPhase()`` counter that increments when the barrier is released
* ``phaser.awaitAdvance(phase)`` makes thread wait until a specific phase is reached
* ``new Phaser(phaser)`` can be used to make tree structures with phasers
* ``phaser.arriveAndDeregister()`` decreases number of parties without waiting for anything.

### Exchanger
* Can be used to exchange data between __two__ threads.
  * The type of data can be defined during initialization ``Exchanger<String>``
* ``exchanger.exchange(value)`` blocks thread until another thread reaches this statement.
  * When a pair of threads reaches the exchange method, they exchange the specified value.

### Volatile
* variables can be declared as ``voletile`` which blocks this variable from being cached, to avoid synchronization problems.
  * cached variables might not be synchronized between different threads immediately.
  * values stored only on the RAM however (``voletile``) will not suffer from this.

## Advanced Topics

### 6.2 Thread Pools
* ``ThreadPoolExecutor`` can be used to create a thread pool of a specific size and maximum size.
  * The tasks can be submitted in the form of runnable and are stored in a BlockingQueue (e.g. ``ArrayBlockingQueue``)
* ``threadPoolExecutor.execute(Runnable)`` can be used to submit tasks without a result.
* ``threadPoolExecutor.submit(Callable)`` can be used to submit a task that has to return a value.
  * Returns a Future object that wraps the result of a future operation
  * ``future.get()`` will try to get the computation result.
    * If the result is not available yet, it pauses until the result is available.
  * ``future.isDone()`` can be used to check if a result is already available
  * ``future.cancel()`` cancels the execution of the task
* ``threadPoolExecutor.shutdown()`` or ``threadPoolExecutor.shutdownNow()`` can be used to stop the thread pool
  * ``.shutdown()`` stops to accept tasks and wait until all tasks in the queue are finished
  * ``.shutdownNow()`` interrupts all threads and returns Runnables that have not been run (were in the queue)
* ``threadPoolExecutor.awaitTermination()`` can be used to wait for the stopping

#### 6.3 Different Blocking Queues
* ``ArrayBlockingQueue``
  * fixed defined size, that rejects tasks, once it is full
* ``LinkedBlockingQueue``
  * Unbounded queue, may lead to an out of memory
* ``SynchronousQueue``
  * Will directly submit or reject tasks

#### 6.4 Handling unchecked exceptions
* for ``threadPoolExecutor.submit()``, all Exceptions will be collected in the Future object and thrown while calling the ``future.get()`` method and can be caught there
* for ``.execute``, Errors will pass the ``afterExecute()`` method of a ThreadPoolExecutor, which can be overridden in a custom version to catch errors.
* the errors thrown with ``.submit()`` could also be retrieved from the Future object in the ``afterExecute()`` method.

#### 6.5 Task rejection
* If the maximum number of threads is running and the queue is full, new tasks get rejected.
* The rejection can be handled with a try-catch around the submit statement or by providing a RejectedExecutionHandler to the ThreadPoolExecutor.
  * There are predefined RejectedExecutionHandlers:
    * ``ThreadPoolExecutor.AbortPolicy()`` default
    * ``.CallerRunsPolicy()`` which runs the task on the callers thread
    * ``.DiscardPolicy()`` silently discards the rejected task
    * ``.DiscardOldestPolicy()`` discards the oldest task in the queue and accepts the new task

#### 6.6 Monitoring
* ``threadPoolExecutor.getPoolSize()`` number of threads that are currently in the thread pool
* ``.getActiveCount()`` number of active threads in the pool
* ``.getTaskCount()`` number of taks in the queue
* ``.getCompletedTaskCount()``

#### 6.7 Scheduled Thread Pool Executor
* extends the ThreadPoolExecutor
* ``.schedule(Runnable, time)`` can be used to submit a task, that should run after a specified time
* Has a unbounded queue by default
* ``.scheduleAtFixedRate(Runnable, initialDelay, period)`` to run a task periodically
* ``.schedule()`` returns a Future objects that can be canceled (have also to be removed from the queue)

#### 6.8 Fork-Join-Pool
* Has as many threads as there are threads on the hardware
* Has one queue per thread
* Once a thread does not have tasks in its pool anymore, it steals tasks from other queues
* only ``ForkJoinTasks``, e.g. ``RecursiveAction`` (has no return value) or ``RecursiveTask`` (returns something) objects can be submitted to this pool
* The thread pool has the best performance when a ForkJoinTask submits subproblems recursively.
* Is ideally used for divide and conquer algorithms.
* within a RecursiveAction implementation the ``.invokeAll(RecursiveAction, RecursiveAction)`` can be used to recursively submit subproblems.

#### 6.9 Executors
* Class with a bunch of frequently used static methods
  * ``.newFixedThreadPool`` thread pool executor with unlimited queue
  * ``.newCachedThreadPool`` pool with synchronous pool (no elements in queue, all in threads)
* Thread Factorys can be used to define how each thread of a threadpool should be created


