package tree;

public class Infix {

    public static String of(Node n) {
        if (n instanceof Leaf l) return l.name();
        if (n instanceof Call c) return c.name() + "(" + of(c.arg()) + ")";
        if (n instanceof BinOp b) return "(" + of(b.left()) + b.op() + of(b.right()) + ")";

        Chain c = (Chain) n;
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        if (c.kind() == '+') {
            for (Node x : c.pos()) {
                if (!first) sb.append(" + ");
                sb.append(paren(x, false));
                first = false;
            }
            for (Node x : c.neg()) {
                sb.append(first ? "-" : " - ").append(paren(x, false));
                first = false;
            }
        } else {
            if (c.pos().isEmpty()) sb.append("1");
            for (Node x : c.pos()) {
                if (!first) sb.append("*");
                sb.append(paren(x, false));
                first = false;
            }
            for (Node x : c.neg()) sb.append("/").append(paren(x, true));
        }
        return sb.toString();
    }

    // parentheses are needed around the inner sum; the divisor also requires parentheses around the inner product.
    private static String paren(Node x, boolean divisor) {
        boolean need = x instanceof Chain ch && (ch.kind() == '+' || divisor);
        return need ? "(" + of(x) + ")" : of(x);
    }
}