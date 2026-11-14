package tree;

public sealed interface Node permits Leaf, BinOp, Chain, Call { }