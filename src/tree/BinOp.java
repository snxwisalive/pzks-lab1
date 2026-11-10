package tree;

public record BinOp(char op, Node left, Node right) implements Node {
    @Override
    public String toString() { return "(" + left + op + right + ")"; }
}