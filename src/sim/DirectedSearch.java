package sim;

import tree.*;

import java.util.*;

public class DirectedSearch {

    private record Cand(Node node, int time, int t1, int parent, String label) {}

    public static void run(Node start, VliwConfig cfg, int beam, int maxDepth) {
        List<Cand> all = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        Cand first = evaluate(start, cfg, -1, "start");
        if (first == null) {
            System.out.println("Directed search: the expression cannot be evaluated by the VLIW model");
            return;
        }
        all.add(first);
        seen.add(Simplifier.key(start));

        Comparator<Integer> byQuality = Comparator
                .<Integer>comparingInt(i -> all.get(i).time())
                .thenComparingInt(i -> all.get(i).t1());

        List<Integer> frontier = List.of(0);
        int best = 0;
        int stale = 0;
        int depthReached = 0;

        for (int depth = 1; depth <= maxDepth; depth++) {
            List<Integer> next = new ArrayList<>();
            for (int i : frontier) {
                for (FactorForms.Step s : FactorSearch.neighbors(all.get(i).node())) {
                    if (!seen.add(Simplifier.key(s.form()))) continue;
                    Cand c = evaluate(s.form(), cfg, i, s.label());
                    if (c == null) continue;
                    all.add(c);
                    next.add(all.size() - 1);
                }
            }
            if (next.isEmpty()) break;
            next.sort(byQuality);
            if (next.size() > beam) next = new ArrayList<>(next.subList(0, beam));
            frontier = next;
            depthReached = depth;

            int levelBest = next.get(0);
            if (byQuality.compare(levelBest, best) < 0) { best = levelBest; stale = 0; }
            else stale++;
            if (stale >= 2) break;
        }

        print(all, best, depthReached, beam, cfg);
    }

    private static Cand evaluate(Node form, VliwConfig cfg, int parent, String label) {
        Node par = ParallelBuilder.build(form);
        List<Task> tasks;
        try {
            tasks = TaskGraph.build(par, cfg);
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (tasks.isEmpty()) return null;
        VliwScheduler.Result r = new VliwScheduler(cfg).run(tasks);
        return new Cand(form, r.time(), r.sequentialTime(), parent, label);
    }

    private static void print(List<Cand> all, int best, int depth, int beam, VliwConfig cfg) {
        LinkedList<Integer> path = new LinkedList<>();
        for (int i = best; i >= 0; i = all.get(i).parent()) path.addFirst(i);

        System.out.println("Directed search (beam " + beam + "): evaluated " + all.size()
                + " forms, depth " + depth);
        int step = 0;
        for (int i : path) {
            Cand c = all.get(i);
            String text = Infix.of(c.node());
            if (text.length() > 80) text = text.substring(0, 77) + "...";
            System.out.println("  " + (step == 0 ? "start" : "step " + step + " (" + c.label() + ")")
                    + ": T = " + c.time() + ", T1 = " + c.t1() + "  " + text);
            step++;
        }
        Cand a = all.get(0), b = all.get(best);
        System.out.println(String.format(Locale.ROOT,
                "Directed search result: T %d -> %d, S %.2f -> %.2f, E %.2f -> %.2f",
                a.time(), b.time(),
                a.t1() / (double) a.time(), b.t1() / (double) b.time(),
                a.t1() / (double) a.time() / cfg.processors,
                b.t1() / (double) b.time() / cfg.processors));
    }
}