package tree;

import java.util.*;

public class FactorForms {

    public record Step(Node form, String label) {}

    public static List<Step> factorOnce(Node n) {
        List<Step> out = new ArrayList<>();
        if (!(n instanceof Chain c) || c.kind() != '+') return out;

        List<Node> terms = new ArrayList<>();
        List<Boolean> signs = new ArrayList<>();
        for (Node x : c.pos()) { terms.add(x); signs.add(true); }
        for (Node x : c.neg()) { terms.add(x); signs.add(false); }

        //  the key “M:...” is a common factor, and “D:...” is a common divisor
        Map<String, List<Integer>> where = new LinkedHashMap<>();
        Map<String, Node> sample = new HashMap<>();
        for (int i = 0; i < terms.size(); i++) {
            Set<String> seen = new HashSet<>();
            for (Node f : multipliers(terms.get(i))) register("M:", f, i, seen, where, sample);
            for (Node d : divisors(terms.get(i))) register("D:", d, i, seen, where, sample);
        }

        for (Map.Entry<String, List<Integer>> e : where.entrySet()) {
            List<Integer> idx = e.getValue();
            if (idx.size() < 2) continue;
            String k = e.getKey();
            boolean divisor = k.startsWith("D:");
            Node common = sample.get(k);

            // sign of the factored-out term: plus, if there is a positive number among the terms in the group
            boolean anyPos = idx.stream().anyMatch(signs::get);

            List<Node> inPos = new ArrayList<>();
            List<Node> inNeg = new ArrayList<>();
            for (int i : idx) {
                Node rest = without(terms.get(i), k);
                add(rest, signs.get(i) == anyPos, inPos, inNeg);
            }
            Node inner = new Chain('+', inPos, inNeg);
            Node factored = divisor
                    ? new Chain('*', new ArrayList<>(List.of(inner)), new ArrayList<>(List.of(common)))
                    : new Chain('*', new ArrayList<>(List.of(common, inner)), new ArrayList<>());

            List<Node> outPos = new ArrayList<>();
            List<Node> outNeg = new ArrayList<>();
            for (int i = 0; i < terms.size(); i++) {
                if (!idx.contains(i)) (signs.get(i) ? outPos : outNeg).add(terms.get(i));
            }
            (anyPos ? outPos : outNeg).add(factored);

            Node form = (outPos.size() == 1 && outNeg.isEmpty())
                    ? outPos.get(0)
                    : new Chain('+', outPos, outNeg);
            out.add(new Step(form, (divisor ? "divide by " : "factor ") + common));
        }
        return out;
    }

    private static void register(String prefix, Node f, int i, Set<String> seen,
                                 Map<String, List<Integer>> where, Map<String, Node> sample) {
        String k = prefix + Simplifier.key(f);
        if (seen.add(k)) {
            where.computeIfAbsent(k, z -> new ArrayList<>()).add(i);
            sample.putIfAbsent(k, f);
        }
    }

    // terms of the sum: list of products or the term itself
    private static List<Node> multipliers(Node term) {
        if (term instanceof Chain c && c.kind() == '*') return c.pos();
        return List.of(term);
    }

    // divisors of the addend: neg-list of the product
    private static List<Node> divisors(Node term) {
        if (term instanceof Chain c && c.kind() == '*') return c.neg();
        return List.of();
    }

    // term missing one instance of a multiplier or divisor
    private static Node without(Node term, String k) {
        if (!(term instanceof Chain c && c.kind() == '*')) return new Leaf("1");
        boolean divisor = k.startsWith("D:");
        String key = k.substring(2);
        List<Node> pos = new ArrayList<>(c.pos());
        List<Node> neg = new ArrayList<>(c.neg());
        List<Node> target = divisor ? neg : pos;
        for (int i = 0; i < target.size(); i++) {
            if (Simplifier.key(target.get(i)).equals(key)) { target.remove(i); break; }
        }
        if (pos.isEmpty() && neg.isEmpty()) return new Leaf("1");
        if (pos.size() == 1 && neg.isEmpty()) return pos.get(0);
        return new Chain('*', pos, neg);
    }

    // adds the operand to the sum; the inner sum is merged with the outer one
    private static void add(Node x, boolean positive, List<Node> pos, List<Node> neg) {
        if (x instanceof Chain c && c.kind() == '+') {
            for (Node p : c.pos()) add(p, positive, pos, neg);
            for (Node q : c.neg()) add(q, !positive, pos, neg);
        } else {
            (positive ? pos : neg).add(x);
        }
    }
}