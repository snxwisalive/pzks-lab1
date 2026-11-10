package tree;

public record Leaf(String name) implements Node {
    @Override
    public String toString() { return name; }
}
