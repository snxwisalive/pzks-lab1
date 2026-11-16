package tree;

import java.util.*;

public class FactorSearch {

    public record Form(int id, Node node, int parent, String label) {}

    public static List<Form> explore(Node start, int limit) {
        List<Form> all = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        all.add(new Form(0, start, -1, "start"));
        seen.add(Simplifier.key(start));

        for (int i = 0; i < all.size() && all.size() < limit; i++) {
            Form cur = all.get(i);
            for (FactorForms.Step s : neighbors(cur.node())) {
                if (all.size() >= limit) break;
                if (seen.add(Simplifier.key(s.form()))) {
                    all.add(new Form(all.size(), s.form(), cur.id(), s.label()));
                }
            }
        }
        return all;
    }

    // all forms obtained by a single expansion: at the top level and within nested operands
    private static List<FactorForms.Step> neighbors(Node n) {
        List<FactorForms.Step> out = new ArrayList<>(FactorForms.factorOnce(n));
        if (n instanceof Chain c) {
            for (int i = 0; i < c.pos().size(); i++) {
                for (FactorForms.Step s : neighbors(c.pos().get(i))) {
                    out.add(new FactorForms.Step(replace(c, false, i, s.form()), s.label()));
                }
            }
            for (int i = 0; i < c.neg().size(); i++) {
                for (FactorForms.Step s : neighbors(c.neg().get(i))) {
                    out.add(new FactorForms.Step(replace(c, true, i, s.form()), s.label()));
                }
            }
        } else if (n instanceof Call call) {
            for (FactorForms.Step s : neighbors(call.arg())) {
                out.add(new FactorForms.Step(new Call(call.name(), s.form()), s.label()));
            }
        }
        return out;
    }

    // inserts a new operand into the chain; merges a chain of the same type with its parent
    private static Node replace(Chain c, boolean inNeg, int i, Node repl) {
        List<Node> pos = new ArrayList<>(c.pos());
        List<Node> neg = new ArrayList<>(c.neg());
        (inNeg ? neg : pos).remove(i);
        if (repl instanceof Chain r && r.kind() == c.kind()) {
            (inNeg ? neg : pos).addAll(r.pos());
            (inNeg ? pos : neg).addAll(r.neg());
        } else {
            (inNeg ? neg : pos).add(repl);
        }
        return new Chain(c.kind(), pos, neg);
    }
}