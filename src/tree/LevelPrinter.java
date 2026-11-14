package tree;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class LevelPrinter {

    public record Result(List<List<Node>> levels, Map<Node, String> names) {
        public int height() { return levels.size(); }
        public int width() { return levels.stream().mapToInt(List::size).max().orElse(0); }
        public int opCount() { return levels.stream().mapToInt(List::size).sum(); }
    }

    public static Result analyze(Node root) {
        Map<Node, Integer> lvl = new IdentityHashMap<>();
        int h = compute(root, lvl);

        List<List<Node>> levels = new ArrayList<>();
        for (int i = 0; i < h; i++) levels.add(new ArrayList<>());
        collect(root, lvl, levels);

        Map<Node, String> names = new IdentityHashMap<>();
        int k = 1;
        for (List<Node> level : levels) {
            for (Node op : level) names.put(op, "t" + k++);
        }
        return new Result(levels, names);
    }

    private static int compute(Node n, Map<Node, Integer> lvl) {
        if (n instanceof BinOp b) {
            int v = 1 + Math.max(compute(b.left(), lvl), compute(b.right(), lvl));
            lvl.put(b, v);
            return v;
        }
        if (n instanceof Call c) {
            int v = 1 + compute(c.arg(), lvl);
            lvl.put(c, v);
            return v;
        }
        return 0;
    }

    private static void collect(Node n, Map<Node, Integer> lvl, List<List<Node>> levels) {
        if (n instanceof BinOp b) {
            collect(b.left(), lvl, levels);
            collect(b.right(), lvl, levels);
            levels.get(lvl.get(b) - 1).add(b);
        } else if (n instanceof Call c) {
            collect(c.arg(), lvl, levels);
            levels.get(lvl.get(c) - 1).add(c);
        }
    }

    private static String name(Node n, Map<Node, String> names) {
        return n instanceof Leaf l ? l.name() : names.get(n);
    }

    public static void print(Result r) {
        for (int i = 0; i < r.levels().size(); i++) {
            System.out.println("Level " + (i + 1) + ":");
            for (Node op : r.levels().get(i)) {
                String lhs = r.names().get(op);
                if (op instanceof BinOp b) {
                    System.out.println("  " + lhs + " = " + name(b.left(), r.names())
                            + " " + b.op() + " " + name(b.right(), r.names()));
                } else {
                    Call c = (Call) op;
                    System.out.println("  " + lhs + " = " + c.name() + "(" + name(c.arg(), r.names()) + ")");
                }
            }
        }
        System.out.println("Height: " + r.height() + ", width: " + r.width() + ", operations: " + r.opCount());
    }
}