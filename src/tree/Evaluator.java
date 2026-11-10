package tree;

import java.util.Map;

public class Evaluator {

    public static double eval(Node n, Map<String, Double> vars) {
        if (n instanceof Leaf l) {
            try {
                return Double.parseDouble(l.name());
            } catch (NumberFormatException e) {
                Double v = vars.get(l.name());
                if (v == null) throw new IllegalArgumentException("No value for variable " + l.name());
                return v;
            }
        }
        if (n instanceof BinOp b) {
            double x = eval(b.left(), vars);
            double y = eval(b.right(), vars);
            return switch (b.op()) {
                case '+' -> x + y;
                case '-' -> x - y;
                case '*' -> x * y;
                case '/' -> x / y;
                default -> throw new IllegalArgumentException("Unknown operator " + b.op());
            };
        }
        Chain c = (Chain) n;
        boolean sum = c.kind() == '+';
        double p = sum ? 0 : 1;
        double q = sum ? 0 : 1;
        for (Node x : c.pos()) {
            double v = eval(x, vars);
            p = sum ? p + v : p * v;
        }
        for (Node x : c.neg()) {
            double v = eval(x, vars);
            q = sum ? q + v : q * v;
        }
        return sum ? p - q : p / q;
    }
}