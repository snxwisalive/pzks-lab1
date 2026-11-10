package tree;

import java.util.ArrayList;
import java.util.List;

public class ZeroDivisionChecker {

    // top-level terms: the operands of the outermost + and - operators
    public static List<Node> terms(Node ast) {
        List<Node> out = new ArrayList<>();
        split(ast, out);
        return out;
    }

    private static void split(Node n, List<Node> out) {
        if (n instanceof BinOp b && (b.op() == '+' || b.op() == '-')) {
            split(b.left(), out);
            split(b.right(), out);
        } else {
            out.add(n);
        }
    }

    // searches for a zero in the denominator at any depth of the simplified tree
    public static boolean hasZeroDivisor(Node n) {
        if (!(n instanceof Chain c)) return false;
        if (c.kind() == '*') {
            for (Node x : c.neg()) {
                if (isZero(x)) return true;
            }
        }
        for (Node x : c.pos()) {
            if (hasZeroDivisor(x)) return true;
        }
        for (Node x : c.neg()) {
            if (hasZeroDivisor(x)) return true;
        }
        return false;
    }

    private static boolean isZero(Node n) {
        if (!(n instanceof Leaf l)) return false;
        char ch = l.name().charAt(0);
        boolean number = Character.isDigit(ch) || ch == '.' || ch == '-';
        return number && Double.parseDouble(l.name()) == 0;
    }
}