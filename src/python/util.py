import pandas as pd
from threading import Thread

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
                                                                                  "normalized kmer count"]],
                                       axis=1).sum(axis=0)
    true_assigned = pd.DataFrame(true_assigned, columns=["true assigned reads"])
    true_assigned["assignment algorithm"] = true_assigned.index
    return true_assigned


class Node:
    def __init__(self, id: int, label: str, rank: str, parent=None):
        self.id = id
        self.rank = rank
        self.label = label
        self.parent = parent
        self.children = []

    def add_child(self, child):
        self.children.append(child)


class Tree:
    def __init__(self, path: str):
        self.nodes = {}
        with open(path, 'r') as f:
            f.readline()  # skip header
            parent_id, node_id, rank, label, kmers_in_database = f.readline().split('\t')  # root
            node_id = int(node_id)
            self.nodes[node_id] = Node(node_id, label, rank)
            for line in f:
                parent_id, node_id, rank, label, kmers_in_database = line.strip().split('\t')
                parent_id = int(parent_id)
                node_id = int(node_id)
                parent = self.nodes[parent_id]
                node = Node(node_id, label, rank, parent)
                parent.add_child(node)
                self.nodes[node_id] = node

    def get_label(self, id: int):
        if self.nodes.keys().__contains__(id):
            return self.nodes[id].label
        return None

    def get_id(self, name: str):
        for node in self.nodes.values():
            if node.label == name:
                return node.id
        return None

    def get_rank(self, id: int):
        if self.nodes.keys().__contains__(id):
            return self.nodes[id].rank
        return None


def read_kraken_assignment(path: str, target: dict):
    def split_kraken_assignment(assignment_string: str):
        assignments = {}
        for assignment in assignment_string.split():
            if assignment == "-:-":
                continue
            taxon_id, count = map(int, assignment.split(":"))
            assignments[taxon_id] = assignments.get(taxon_id, 0) + count
        return " ".join(f"{x}:{y}" for x, y in sorted(assignments.items(), key=lambda item: item[1], reverse=True))

    df = pd.read_csv(path, sep="\t", header=None,
                     names=["tag", "readId", "kraken2_assignment", "sequence_length", "assignments"], usecols=[1, 2, 3, 4], converters={"assignments": split_kraken_assignment})
    target["kraken"] = df

def read_raw_assignment(path: str, target: dict):
    df = pd.read_csv(path, sep="\t", header=None, skiprows=[0], names=["readId", "assignments"], usecols=[1])
    target["raw"] = df

def read_per_read_assignment(path: str, target: dict):
    df = pd.read_csv(path, sep="\t", skiprows=[0], header=0,  usecols=[1], converters={"assignments": lambda x: int(x.split(" ")[-1][1:-1])})
    target["per_read"] = df

def compare_with_kraken(path_kraken: str, path_raw_assignment: str, path_per_read_assignment):
    target = {}
    print("Start reading kraken assignment")
    kraken_assignment_processor = Thread(target=read_kraken_assignment, args=(path_kraken, target))
    kraken_assignment_processor.start()
    print("Start reading raw assignment")
    raw_assignment_processor = Thread(target=read_raw_assignment, args=(path_raw_assignment, target))
    raw_assignment_processor.start()
    print("Start reading per read assignment")
    per_read_assignment_processor = Thread(target=read_per_read_assignment, args=(path_per_read_assignment, target))
    per_read_assignment_processor.start()

    print("Joining raw assignment")
    raw_assignment_processor.join()
    print("Joining per read assignment")
    per_read_assignment_processor.join()
    print("Joining kraken assignment")
    kraken_assignment_processor.join()

    return target