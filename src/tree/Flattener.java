package tree;

import java.util.ArrayList;
import java.util.List;

public class Flattener {

    public static Node flatten(Node node) {
        if (node instanceof BinOp b) {
            char kind = group(b.op());
            List<Node> pos = new ArrayList<>();
            List<Node> neg = new ArrayList<>();
            collect(b, kind, true, pos, neg);
            return new Chain(kind, pos, neg);
        }

        if (node instanceof Call c) {
            return new Call(c.name(), flatten(c.arg()));
        }

        return node; // leaf
    }

    // collects operands of the same priority, tracking the sign
    private static void collect(Node n, char kind, boolean positive,
                                List<Node> pos, List<Node> neg) {
        if (n instanceof BinOp b && group(b.op()) == kind) {
            collect(b.left(), kind, positive, pos, neg);
            boolean inverts = b.op() == '-' || b.op() == '/';
            collect(b.right(), kind, inverts ? !positive : positive, pos, neg);
        } else {
            Node operand = flatten(n); // nested chain with a different priority or a leaf
            (positive ? pos : neg).add(operand);
        }
    }

    private static char group(char op) {
        return (op == '+' || op == '-') ? '+' : '*';
    }
}