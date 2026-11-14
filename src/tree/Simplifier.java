package tree;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class Simplifier {

    public static Node simplify(Node n) {
        if (n instanceof Call call) {
            return new Call(call.name(), simplify(call.arg()));
        }

        if (!(n instanceof Chain c)) return n;
        char kind = c.kind();
        boolean sum = kind == '+';
        double neutral = sum ? 0 : 1;

        // 1. we simplify the operands and combine chains of the same type
        List<Node> pos = new ArrayList<>();
        List<Node> neg = new ArrayList<>();
        collect(c.pos(), false, kind, pos, neg);
        collect(c.neg(), true, kind, pos, neg);

        // 2. we simplify the numbers separately in the numerator and denominator
        double pc = neutral, nc = neutral;
        List<Node> pRest = new ArrayList<>();
        List<Node> nRest = new ArrayList<>();
        for (Node x : pos) {
            if (isNum(x)) pc = sum ? pc + val(x) : pc * val(x);
            else pRest.add(x);
        }
        for (Node x : neg) {
            if (isNum(x)) nc = sum ? nc + val(x) : nc * val(x);
            else nRest.add(x);
        }

        cancel(pRest, nRest);

        if (sum) {
            double k = pc - nc;
            if (k != 0) {
                if (k > 0 || pRest.isEmpty()) pRest.add(new Leaf(fmt(k)));
                else nRest.add(new Leaf(fmt(-k)));
            }
        } else {
            if (nc == 0) return new Chain(kind, pos, neg);   // zero dividing
            if (pc == 0) return new Leaf("0");
            if (pRest.isEmpty() && nRest.isEmpty()) return new Leaf(fmt(pc / nc));
            if (pc != 1) pRest.add(new Leaf(fmt(pc)));
            if (nc != 1) nRest.add(new Leaf(fmt(nc)));
        }

        // 3. we collapse the degenerate chains
        if (pRest.isEmpty() && nRest.isEmpty()) return new Leaf(fmt(neutral));
        if (pRest.size() == 1 && nRest.isEmpty()) return pRest.get(0);
        return new Chain(kind, pRest, nRest);
    }

    private static void collect(List<Node> src, boolean flipped, char kind,
                                List<Node> pos, List<Node> neg) {
        for (Node x : src) {
            Node s = simplify(x);
            if (s instanceof Chain ch && ch.kind() == kind) {
                (flipped ? neg : pos).addAll(ch.pos());
                (flipped ? pos : neg).addAll(ch.neg());
            } else {
                (flipped ? neg : pos).add(s);
            }
        }
    }

    private static boolean isNum(Node n) {
        if (!(n instanceof Leaf l)) return false;
        char ch = l.name().charAt(0);
        return Character.isDigit(ch) || ch == '.' || ch == '-';
    }

    private static double val(Node n) {
        return Double.parseDouble(((Leaf) n).name());
    }

    private static String fmt(double v) {
        if (Double.isFinite(v)) {
            v = BigDecimal.valueOf(v).round(new MathContext(15)).doubleValue();
        }
        if (v == Math.rint(v) && Math.abs(v) < 1e15) return String.valueOf((long) v);
        return String.valueOf(v);
    }

    private static void cancel(List<Node> p, List<Node> n) {
        for (Iterator<Node> it = p.iterator(); it.hasNext(); ) {
            String k = key(it.next());
            for (int j = 0; j < n.size(); j++) {
                if (key(n.get(j)).equals(k)) {
                    n.remove(j);
                    it.remove();
                    break;
                }
            }
        }
    }

    private static String key(Node n) {
        if (n instanceof Leaf l) return l.name();
        if (n instanceof Chain c) {
            return c.kind() + "[" + sortedKeys(c.pos()) + "|" + sortedKeys(c.neg()) + "]";
        }
        return n.toString();
    }

    private static String sortedKeys(List<Node> list) {
        return list.stream().map(Simplifier::key).sorted().collect(Collectors.joining(","));
    }
}