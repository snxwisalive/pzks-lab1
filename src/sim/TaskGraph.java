package sim;

import tree.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TaskGraph {

    public static List<Task> build(Node parallel, VliwConfig cfg) {
        LevelPrinter.Result r = LevelPrinter.analyze(parallel);
        List<Task> tasks = new ArrayList<>();
        for (List<Node> level : r.levels()) {
            for (Node op : level) {
                if (!(op instanceof BinOp b)) {
                    throw new IllegalArgumentException("Function calls are not supported in the VLIW model");
                }
                int id = Integer.parseInt(r.names().get(b).substring(1));
                List<Integer> parents = new ArrayList<>();
                for (Node child : new Node[]{b.left(), b.right()}) {
                    String name = r.names().get(child);
                    if (name != null) parents.add(Integer.parseInt(name.substring(1)));
                }
                tasks.add(new Task(id, b.op(), cfg.duration.get(b.op()), parents));
            }
        }
        tasks.sort(Comparator.comparingInt(Task::id));
        return tasks;
    }

    public static String describe(List<Task> tasks) {
        StringBuilder sb = new StringBuilder();
        for (Task t : tasks) {
            sb.append(t.id()).append("[").append(t.op()).append(", w=").append(t.weight()).append("]");
            if (!t.parents().isEmpty()) sb.append(" <- ").append(t.parents());
            sb.append("; ");
        }
        return sb.toString().trim();
    }
}