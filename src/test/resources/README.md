# Test dataset
Description of shared sequence parts between the test database and dataset and the corresponding expected k-mer hits
and LCAs.

## db.fsa
* 1,423 amino acids total
0) 2 kmers
   * one from tax1/tax2 -> 0 and one from tax3/4/5
1) 168 kmers
   * one also occures in taxon 2 (-> 167 unique kmers map to taxon1)
2) 285 kmers
   * -> 284 (one occures in taxon1)
3) 93 kmers
   * -> 92 (one maps to tax0)
4) 207 kmers + 243 kmers -> 450
   * -> 449 (one maps to tax0)
5) 329 kmers
   * -> 328 (one maps to tax0)
6) 243 kmers
   * does not occure in taxonomy
* total kmers 1,339
* ``YEITVYQLSADDLRS`` 2 times (tax1, tax2) -> tax0
* ``LEDNIRRVIADIRPQ`` 3 times (tax3, 4, 5) -> tax0
* different kmers: 1336

## readAssignment.fq
* read1
  * length 503
  * kmers 918
* read6
  * length 272
  * kmers 456
* read2
  * length 390
  * kmers 692
* read3
  * length 235
  * kmers 382
* read4
  * length 282
  * kmers 476
* read5
  * length 278
  * kmers 468
* total kmers 3392

## Matching kmers
* read4
  * tax1
    * ECPLREENLHSLRKLYQYGARVCYLMRSSIECD
    * reverse translated: GAATGCCCGCTGCGCGAAGAAAACCTGCATAGCCTGCGCAAACTGTATCAGTATGGCGCGCGCGTGTGCTATCTGATGCGCAGCAGCATTGAATGCGAT
    * reverse complement (in read): ATCGCATTCAATGCTGCTGCGCATCAGATAGCACACGCGCGCGCCATACTGATACAGTTTGCGCAGGCTATGCAGGTTTTCTTCGCGCAGCGGGCATTC
    * stop codon TAG in position 30 of reverse translate (reading frame 3, irrelevant)
    * 19 kmer
    * NVFIAKVLKTI
    * AACGTGTTTATTGCGAAAGTGCTGAAAACCATT
    * 0 kmer
    * NVFIAKVLKTINNSAC
    * AACGTGTTTATTGCGAAAGTGCTGAAAACCATTAACAACAGCGCGTGC
    * stop codon TAA in position 33 - 35
    * stop codon TGA in position 55 (not in reading frame 1)
    * 2 -> 0 kmer because of stop codon
    * total 21
  * tax5
    * DAGGQLVVIDEIHTP
    * GATGCGGGCGGCCAGCTGGTGGTGATTGATGAAATTCATACCCCG
    * 3 occurrences of stop codon TGA, but none in reading frame 1
    * 1 kmer
* read1
  * tax2
    * ISDDDTAALGGGKSKQARSDRGPEDFSSVVKNRLQSYSRTGQACDRCKVRKIRCDALAEGCSHCINLNLECYVTDRVTGRTERRGYLQQLERE
    * ATTAGCGATGATGATACCGCGGCGCTGGGCGGCGGCAAAAGCAAACAGGCGCGCAGCGATCGCGGCCCGGAAGATTTTAGCAGCGTGGTGAAAAACCGCCTGCAGAGCTATAGCCGCACCGGCCAGGCGTGCGATCGCTGCAAAGTGCGCAAAATTCGCTGCGATGCGCTGGCGGAAGGCTGCAGCCATTGCATTAACCTGAACCTGGAATGCTATGTGACCGATCGCGTGACCGGCCGCACCGAACGCCGCGGCTATCTGCAGCAGCTGGAACGCGAA
    * stop codon TAA in position 195 - 197 (reading frame 3, irrelevant
    * stop codon TAG in position 3, 78 and 111 (none in reading frame 1, irrelevant)
    * stop codon TGA in position 9, 12, 89, 200, 218, 230 (none in reading frame 1)
    * 79 kmer
* read3
  * tax1/tax2 -> node0
    * YEITVYQLSADDLRS 602126021100201
    * reverse translated: TATGAAATTACCGTGTATCAGCTGAGCGCGGATGATCTGCGCAGC
    * reverse complement (in reads): GCTGCGCAGATCATCCGCGCTCAGCTGATACACGGTAATTTCATA
    * stop codon TGA in position 3, 23, 33 (not in reading frame 1)
    * 1 kmer each
  * tax1 -> node1
    * RKYEITVYQLSADDLR
    * reverse translated: CGCAAATATGAAATTACCGTGTATCAGCTGAGCGCGGATGATCTGCGC
    * reverse complement (in reads): GCGCAGATCATCCGCGCTCAGCTGATACACGGTAATTTCATATCAGCG (second last codon is stop codon (as reverse complement))
    * stop codon TGA in position 9, 28, 39 (28 in reading frame 1!)
    * 2 -> 0 kmer because of stop codon
* read5
  * tax3/4/5 -> node0
    * LEDNIRRVIADIRPQ
    * CTGGAAGATAACATTCGCCGCGTGATTGCGGATATTCGCCCGCAG
    * stop codon TAA in position 9 - 11 (reading frame 3, irrelevant)
    * stop codon TGA in position 23 (not in reading frame 1)
    * 1 kmer each