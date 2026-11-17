package sim;

import java.util.*;

public class VliwScheduler {

    public record Result(int time, int sequentialTime, double speedup, double efficiency,
                         double[] cef, List<Map<Integer, String>> procCells,
                         List<Map<Integer, String>> bankCells) {}

    private record Option(int proc, int start, List<Integer> order, int reads,
                          int writeTick, int writeBank, int rootBank) {}

    private final VliwConfig cfg;
    private final BitSet[] procBusy;
    private final BitSet[] bankBusy;
    private final List<Map<Integer, String>> procCells = new ArrayList<>();
    private final List<Map<Integer, String>> bankCells = new ArrayList<>();
    private final Map<Integer, Integer> taskProc = new HashMap<>();
    private final Map<Integer, Integer> writeTick = new HashMap<>();
    private final Map<Integer, Integer> writeBank = new HashMap<>();

    public VliwScheduler(VliwConfig cfg) {
        this.cfg = cfg;
        procBusy = new BitSet[cfg.processors];
        bankBusy = new BitSet[cfg.banks];
        for (int i = 0; i < cfg.processors; i++) { procBusy[i] = new BitSet(); procCells.add(new TreeMap<>()); }
        for (int i = 0; i < cfg.banks; i++) { bankBusy[i] = new BitSet(); bankCells.add(new TreeMap<>()); }
    }

    public Result run(List<Task> tasks) {
        for (Task t : tasks) {
            Option best = null;
            for (int p = 0; p < cfg.processors; p++) {
                Option o = tryPlace(t, p);
                if (best == null || better(o, best)) best = o;
            }
            commit(t, best);
        }

        int time = 0, sumWeights = 0, roots = 0;
        for (Task t : tasks) {
            time = Math.max(time, writeTick.get(t.id()));
            sumWeights += t.weight();
            if (t.parents().isEmpty()) roots++;
        }
        int t1 = sumWeights + tasks.size() + roots;
        double speedup = time == 0 ? 0 : (double) t1 / time;
        double[] cef = new double[cfg.processors];
        for (int p = 0; p < cfg.processors; p++) {
            cef[p] = time == 0 ? 0 : procBusy[p].cardinality() / (double) time;
        }
        return new Result(time, t1, speedup, speedup / cfg.processors, cef, procCells, bankCells);
    }

    private boolean better(Option a, Option b) {
        if (a.writeTick() != b.writeTick()) return a.writeTick() < b.writeTick();
        if (a.reads() != b.reads()) return a.reads() < b.reads();
        return a.proc() < b.proc();
    }

    private Option tryPlace(Task t, int p) {
        List<Integer> remote = new ArrayList<>();
        int minStart = 1;
        for (int q : t.parents()) {
            if (taskProc.get(q) == p) minStart = Math.max(minStart, writeTick.get(q) + 1);
            else remote.add(q);
        }
        List<List<Integer>> orders = permutations(remote);
        for (int s = minStart; s < 100000; s++) {
            for (List<Integer> order : orders) {
                Option o = tryAt(t, p, s, order);
                if (o != null) return o;
            }
        }
        throw new IllegalStateException("No placement found for task " + t.id());
    }

    private Option tryAt(Task t, int p, int s, List<Integer> order) {
        boolean root = t.parents().isEmpty();
        int k = root ? 1 : order.size();
        int len = k + t.weight() + 1;
        if (!procBusy[p].get(s, s + len).isEmpty()) return null;

        int rootBank = -1;
        if (root) {
            rootBank = freeBank(s);
            if (rootBank < 0) return null;
        } else {
            for (int i = 0; i < k; i++) {
                int q = order.get(i);
                int tick = s + i;
                if (tick <= writeTick.get(q)) return null;
                if (bankBusy[writeBank.get(q)].get(tick)) return null;
            }
        }
        int wt = s + k + t.weight();
        int wb = freeBank(wt);
        if (wb < 0) return null;
        return new Option(p, s, order, k, wt, wb, rootBank);
    }

    private int freeBank(int tick) {
        for (int b = 0; b < cfg.banks; b++) if (!bankBusy[b].get(tick)) return b;
        return -1;
    }

    private void commit(Task t, Option o) {
        int p = o.proc(), s = o.start(), k = o.reads(), w = t.weight();
        procBusy[p].set(s, s + k + w + 1);
        if (t.parents().isEmpty()) {
            mark(p, o.rootBank(), s, "R [*" + t.id() + "]");
        } else {
            for (int i = 0; i < k; i++) {
                int q = o.order().get(i);
                mark(p, writeBank.get(q), s + i, "R [" + q + "]");
            }
        }
        for (int i = 0; i < w; i++) procCells.get(p).put(s + k + i, "[" + t.id() + "]");
        mark(p, o.writeBank(), o.writeTick(), "W [" + t.id() + "]");
        taskProc.put(t.id(), p);
        writeTick.put(t.id(), o.writeTick());
        writeBank.put(t.id(), o.writeBank());
    }

    private void mark(int p, int bank, int tick, String label) {
        procCells.get(p).put(tick, label);
        bankBusy[bank].set(tick);
        bankCells.get(bank).put(tick, label);
    }

    private static List<List<Integer>> permutations(List<Integer> items) {
        List<List<Integer>> out = new ArrayList<>();
        if (items.isEmpty()) { out.add(new ArrayList<>()); return out; }
        for (int i = 0; i < items.size(); i++) {
            List<Integer> rest = new ArrayList<>(items);
            Integer head = rest.remove(i);
            for (List<Integer> tail : permutations(rest)) {
                List<Integer> perm = new ArrayList<>();
                perm.add(head);
                perm.addAll(tail);
                out.add(perm);
            }
        }
        return out;
    }

    public static void print(Result r, VliwConfig cfg) {
        if (r.time() == 0) { System.out.println("VLIW: no operations"); return; }
        StringBuilder sb = new StringBuilder(String.format("%-4s", "T"));
        for (int p = 0; p < cfg.processors; p++) sb.append(String.format("%-9s", "P" + (p + 1)));
        sb.append(" ");
        for (int b = 0; b < cfg.banks; b++) sb.append(String.format("%-9s", "B" + (b + 1)));
        System.out.println(sb);
        for (int t = 1; t <= r.time(); t++) {
            sb = new StringBuilder(String.format("%-4d", t));
            for (int p = 0; p < cfg.processors; p++) sb.append(String.format("%-9s", r.procCells().get(p).getOrDefault(t, ".")));
            sb.append(" ");
            for (int b = 0; b < cfg.banks; b++) sb.append(String.format("%-9s", r.bankCells().get(b).getOrDefault(t, ".")));
            System.out.println(sb);
        }
        sb = new StringBuilder(String.format("%-4s", "Cef"));
        for (double c : r.cef()) sb.append(String.format(Locale.ROOT, "%-9.2f", c));
        System.out.println(sb);
        System.out.println(String.format(Locale.ROOT,
                "T = %d, T1 = %d, speedup = %.2f, efficiency = %.2f",
                r.time(), r.sequentialTime(), r.speedup(), r.efficiency()));
    }
}