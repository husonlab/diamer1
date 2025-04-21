import numpy as np
import pandas as pd
from threading import Thread

from matplotlib import pyplot as plt


def read_per_taxon_assignment(path: str, rank: str = None, kmer_threshold: int = 1000,
                              ovo_1_threshold: int = 1) -> pd.DataFrame:
    """
    Reads all taxons with a kmer count greater than 1000 from a per taxon assignment file.
    :param path: path to the file
    :param rank: if specified, only taxons with the specified rank will be returned
    :param kmer_threshold: minimum kmer count to consider a taxon
    :return: dataframe with
    """
    df = pd.read_csv(path, header=0, sep="\t", index_col="label")
    # change old naming scheme
    df.columns = [x.replace("0000", "").replace("ratio: ", "").replace("(accumulated)", "cumulative") for x in
                  df.columns]
    if rank:
        df = df[(df['rank'] == rank) & (df["kmer count"] >= kmer_threshold) &
                (df["OVO (1.00) read count cumulative"] >= ovo_1_threshold)] \
            .sort_values(["kmer count"], ascending=False)
    else:
        df = df[(df["kmer count"] >= kmer_threshold) &
                (df["OVO (1.00) read count cumulative"] >= ovo_1_threshold)] \
            .sort_values(["kmer count"], ascending=False)
    return df


def true_positives(df: pd.DataFrame, total_reads: int, true_labels: list, rank: str):
    """
    Calculate the true positive assigned reads

    The following columns will not be considered in the calculation: "kmer count", "node id", "rank", "label",
    "kmers in database", "kmer count (accumulated)", "normalized kmer count"
    :param df: dataframe with at least one column "label" and "rank"
    :param total_reads: total reads of the sample
    :param true_labels: true labels on the specified rank
    :param rank: rank to use for the evaluation
    :return: dataframe with one column with the true positive assigned reads in percentage and the used algorithm as
    index
    """
    true_assigned = df.loc[true_labels]
    true_assigned = true_assigned.drop([x for x in true_assigned.columns if x in ["kmer count",
                                                                                  "node id",
                                                                                  "rank",
                                                                                  "label",
                                                                                  "kmers in database",
                                                                                  "kmer count (accumulated)",
                                                                                  "normalized kmer count"]], axis=1)
    true_assigned = pd.DataFrame(true_assigned.sum(axis=0), columns=["true assigned reads (%)"]) / total_reads * 100
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

    def get_path_to_root(self, id: int) -> list:
        path = []
        node = self.nodes[id]
        while node:
            path.append(node)
            node = node.parent
        return path

    def get_node(self, id: int = None, label: str = None):
        if id:
            return self.nodes[id]
        if label:
            for node in self.nodes.values():
                if node.label == label:
                    return node
        return None

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


def get_true_taxons_for_all_ranks(tree: Tree, species) -> dict:
    """
    Generates a dictionary with all standard ranks as keys and a list with all taxons that belong to the defined species
    :param tree: phylogenetic tree as reference
    :param species: list of species
    :return: dictionary with all standard ranks as keys and lists with all taxons that belong to the defined species as
    values
    """
    standard_ranks = {"superkingdom", "kingdom", "phylum", "class", "order", "family", "genus", "species"}
    true_taxons = {rank: set() for rank in standard_ranks}
    for s in species:
        path = tree.get_path_to_root(tree.get_id(s))
        for node in path:
            if node.rank in standard_ranks:
                true_taxons[node.rank].add(node.label)
    return true_taxons


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
                     names=["tag", "readId", "kraken2_assignment", "sequence_length", "assignments"],
                     usecols=[1, 2, 3, 4], converters={"assignments": split_kraken_assignment})
    target["kraken"] = df


def read_raw_assignment(path: str, target: dict):
    df = pd.read_csv(path, sep="\t", header=None, skiprows=[0], names=["readId", "assignments"], usecols=[1])
    target["raw"] = df


def read_per_read_assignment(path: str, target: dict):
    df = pd.read_csv(path, sep="\t", skiprows=[0], header=0, usecols=[1],
                     converters={"assignments": lambda x: int(x.split(" ")[-1][1:-1])})
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


def plot_true_assigned_per_rank(df: pd.DataFrame, total: int = 100, title: str = "", x_axis: str = "",
                                y_axis: str = "true assigned reads (%)", ranks: int = 8):
    ranks = ["superkingdom", "kingdom", "phylum", "class", "order", "family", "genus", "species"][-ranks:]
    colors = ["#ffa9b1", "#6e9053", "#9a6dad", "#c7cc67", "#87c4ff", "#e4963a", "#539a97", "#af5d55"]
    colors = colors[-len(ranks):]
    plt.figure(figsize=(len(df), 6))
    for rank, color in zip(ranks, colors):
        plt.bar(df["label"].astype(str), df[rank] / total * 100, label=rank, color=color, width=0.8)
    plt.legend(bbox_to_anchor=(1, 1), loc="upper left")
    plt.ylim(0, 100)
    plt.yticks(np.arange(0, 101, 10))
    plt.xlabel(x_axis)
    plt.xticks(rotation=45)
    plt.ylabel(y_axis)
    plt.title(title, pad=20)
    plt.show()
