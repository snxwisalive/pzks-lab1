package tree;

public record Call(String name, Node arg) implements Node {
    @Override
    public String toString() { return name + "(" + arg + ")"; }
}