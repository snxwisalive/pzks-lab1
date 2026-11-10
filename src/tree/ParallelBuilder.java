package tree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class ParallelBuilder {

    private record Entry(Node node, boolean positive, int height, int seq) {}

    public static Node build(Node n) {
        if (n instanceof Chain c) return buildChain(c);
        return n; // leaf
    }

    private static Node buildChain(Chain c) {
        char plus = c.kind();                        // '+' or '*'
        char minus = (plus == '+') ? '-' : '/';

        PriorityQueue<Entry> pq = new PriorityQueue<>(
                Comparator.comparingInt(Entry::height).thenComparingInt(Entry::seq));
        int seq = 0;

        List<Node> posList = new ArrayList<>(c.pos());
        if (posList.isEmpty()) posList.add(new Leaf(plus == '+' ? "0" : "1"));

        for (Node x : posList) {
            Node t = build(x);
            pq.add(new Entry(t, true, height(t), seq++));
        }
        for (Node x : c.neg()) {
            Node t = build(x);
            pq.add(new Entry(t, false, height(t), seq++));
        }

        while (pq.size() > 1) {
            Entry a = pq.poll();
            Entry b = pq.poll();
            Node node;
            boolean positive;
            if (a.positive() == b.positive()) {
                node = new BinOp(plus, a.node(), b.node());
                positive = a.positive();
            } else {
                Entry p = a.positive() ? a : b;
                Entry q = a.positive() ? b : a;
                node = new BinOp(minus, p.node(), q.node());
                positive = true;
            }
            int h = 1 + Math.max(a.height(), b.height());
            pq.add(new Entry(node, positive, h, seq++));
        }
        return pq.poll().node();
    }

    public static int height(Node n) {
        if (n instanceof Leaf) return 0;
        if (n instanceof BinOp b) return 1 + Math.max(height(b.left()), height(b.right()));
        throw new IllegalArgumentException("Chain must be built first");
    }
}