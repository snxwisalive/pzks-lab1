package tree;

import java.util.List;
import java.util.stream.Collectors;

public record Chain(char kind, List<Node> pos, List<Node> neg) implements Node {

    @Override
    public String toString() {
        String p = pos.stream().map(Node::toString).collect(Collectors.joining(", "));
        if (neg.isEmpty()) {
            return kind + "[" + p + "]";
        }
        String n = neg.stream().map(Node::toString).collect(Collectors.joining(", "));
        return kind + "[" + p + " | " + n + "]";
    }
}