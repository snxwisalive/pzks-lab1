package sim;

import tree.*;

import java.util.*;

public class FormComparison {

    public record Row(int id, String text, int height, int operations,
                      int time, int t1, double speedup, double efficiency) {}

    public static List<Row> evaluate(List<FactorSearch.Form> forms, VliwConfig cfg) {
        List<Row> rows = new ArrayList<>();
        for (FactorSearch.Form f : forms) {
            Node par = ParallelBuilder.build(f.node());
            List<Task> tasks;
            try {
                tasks = TaskGraph.build(par, cfg);
            } catch (IllegalArgumentException e) {
                continue; // model doesn't consider forms with function calls
            }
            if (tasks.isEmpty()) continue;
            VliwScheduler.Result r = new VliwScheduler(cfg).run(tasks);
            LevelPrinter.Result lv = LevelPrinter.analyze(par);
            rows.add(new Row(f.id(), Infix.of(f.node()), lv.height(), lv.opCount(),
                    r.time(), r.sequentialTime(), r.speedup(), r.efficiency()));
        }
        return rows;
    }

    private static final Comparator<Row> BY_TIME = Comparator
            .comparingInt(Row::time)
            .thenComparingInt(Row::t1)
            .thenComparingInt(Row::id);

    public static void print(List<Row> rows, int top) {
        if (rows.isEmpty()) {
            System.out.println("Form comparison: no forms to evaluate");
            return;
        }
        List<Row> sorted = new ArrayList<>(rows);
        sorted.sort(BY_TIME);
        Row best = sorted.get(0);

        System.out.println("Form comparison (sorted by time T, " + rows.size() + " forms):");
        System.out.println(String.format("  %-4s %-5s %-5s %-6s %-6s %-6s %-4s  %s",
                "#", "T", "T1", "S", "E", "height", "ops", "form"));
        int shown = 0;
        for (Row r : sorted) {
            if (shown++ >= top) break;
            String text = r.text().length() > 70 ? r.text().substring(0, 67) + "..." : r.text();
            System.out.println(String.format(Locale.ROOT, "%s %-4d %-5d %-5d %-6.2f %-6.2f %-6d %-4d  %s",
                    r == best ? "*" : " ", r.id(), r.time(), r.t1(), r.speedup(),
                    r.efficiency(), r.height(), r.operations(), text));
        }
        if (rows.size() > top) System.out.println("  ... " + (rows.size() - top) + " more forms");

        Row initial = rows.stream().filter(r -> r.id() == 0).findFirst().orElse(null);
        System.out.println(String.format(Locale.ROOT,
                "Best form: #%d, T = %d, S = %.2f, E = %.2f: %s",
                best.id(), best.time(), best.speedup(), best.efficiency(), best.text()));
        if (initial != null && best.id() != initial.id()) {
            System.out.println("Time: " + initial.time() + " -> " + best.time()
                    + " (reduced by " + (initial.time() - best.time()) + ")");
        } else if (initial != null) {
            System.out.println("The initial form is already the best.");
        }
    }
}