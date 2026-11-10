package tree;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class LevelPrinter {

    public record Result(List<List<BinOp>> levels, Map<Node, String> names) {
        public int height() { return levels.size(); }
        public int width() { return levels.stream().mapToInt(List::size).max().orElse(0); }
        public int opCount() { return levels.stream().mapToInt(List::size).sum(); }
    }

    public static Result analyze(Node root) {
        Map<BinOp, Integer> lvl = new IdentityHashMap<>();
        int h = compute(root, lvl);

        List<List<BinOp>> levels = new ArrayList<>();
        for (int i = 0; i < h; i++) levels.add(new ArrayList<>());
        collect(root, lvl, levels);

        Map<Node, String> names = new IdentityHashMap<>();
        int k = 1;
        for (List<BinOp> level : levels) {
            for (BinOp b : level) names.put(b, "t" + k++);
        }
        return new Result(levels, names);
    }

    private static int compute(Node n, Map<BinOp, Integer> lvl) {
        if (n instanceof BinOp b) {
            int v = 1 + Math.max(compute(b.left(), lvl), compute(b.right(), lvl));
            lvl.put(b, v);
            return v;
        }
        return 0;
    }

    private static void collect(Node n, Map<BinOp, Integer> lvl, List<List<BinOp>> levels) {
        if (n instanceof BinOp b) {
            collect(b.left(), lvl, levels);
            collect(b.right(), lvl, levels);
            levels.get(lvl.get(b) - 1).add(b);
        }
    }

    private static String name(Node n, Map<Node, String> names) {
        return n instanceof Leaf l ? l.name() : names.get(n);
    }

    public static void print(Result r) {
        for (int i = 0; i < r.levels().size(); i++) {
            System.out.println("Level " + (i + 1) + ":");
            for (BinOp b : r.levels().get(i)) {
                System.out.println("  " + r.names().get(b) + " = "
                        + name(b.left(), r.names()) + " " + b.op() + " " + name(b.right(), r.names()));
            }
        }
        System.out.println("Height: " + r.height() + ", width: " + r.width() + ", operations: " + r.opCount());
    }
}