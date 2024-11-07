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

# Parallelization
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

### Synchronization with the synchronized keyword
* ``synchronized (obj) {...}`` can be used to encapsulate a code block that should run mutually exclusive on one thread.
  * The keyword will try to own the objects monitor lock.
    * It the lock is owned by another thread, the current thread places itself on the wait set of this object and waits.
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

### CountDownLatch
* similar to a semaphore, but only counts down.
* can be used in algorithms that divide a problem in subproblems that are run on multiple threads.
* ``countDownLatch.countDown()`` can be used by a thread to indicate that it finished.
* ``countDownLatch.await()`` can be used (e.g. in the main thread) to wait until the specified number of ``countDown()`` calls accumulated.

### BarrierRelease
* can be used to stop multiple Threads at the same execution state and wait for the other threads to reach this state.
* ``cyclicBarrier.await()`` will wait until a specified (``new CyclicBarrier(4)``) number of threads reach the same state before the barrier is released and all waiting threads continue simultaneously.
* ``new CyclicBarrier(4, Runnable)`` a runnable can be supplied to be executed upon each release.
