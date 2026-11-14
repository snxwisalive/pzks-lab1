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

        if (n instanceof Call call) {
            double x = eval(call.arg(), vars);
            return switch (call.name()) {
                case "sin" -> Math.sin(x);
                case "cos" -> Math.cos(x);
                case "tan" -> Math.tan(x);
                case "cot" -> 1.0 / Math.tan(x);
                case "sqrt" -> Math.sqrt(x);
                case "log", "ln" -> Math.log(x);
                case "exp" -> Math.exp(x);
                case "abs" -> Math.abs(x);
                case "asin" -> Math.asin(x);
                case "acos" -> Math.acos(x);
                case "atan" -> Math.atan(x);
                default -> throw new IllegalArgumentException("Unknown function " + call.name());
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