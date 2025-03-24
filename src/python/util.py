import pandas as pd


def read_per_taxon_assignment(path: str, rank: str = None) -> pd.DataFrame:
    df = pd.read_csv(path, header=0, sep="\t")
    if rank:
        df = df[(df['rank'] == rank) & (df["kmer count"] >= 1000)] \
            .sort_values(["kmer count"], ascending=False) \
            .reset_index(drop=True)
    else:
        df = df[(df["kmer count"] >= 1000)] \
            .sort_values(["kmer count"], ascending=False) \
            .reset_index(drop=True)
    return df


def true_assigned_reads(df: pd.DataFrame, true_labels: list, rank: str):
    true_assigned = df[(df['rank'] == rank) & (df["label"].isin(true_labels))]
    true_assigned = true_assigned.drop([x for x in true_assigned.columns if x in ["kmer count",
                                                                  "node id",
                                                                  "rank",
                                                                  "label",
                                                                  "kmers in database",
                                                                  "kmer count (accumulated)",
                                                                  "normalized kmer count"]], axis=1).sum(axis=0)
    true_assigned = pd.DataFrame(true_assigned, columns=["true assigned reads"])
    true_assigned["assignment algorithm"] = true_assigned.index
    return true_assigned
