import lexer.Lexer;
import lexer.Token;
import lexer.TokenType;
import parser.AnalysisError;
import parser.AstBuilder;
import parser.ExpressionAnalyzer;
import tree.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {
    private static int treeCounter = 0;

    public static void main(String[] args) {
        if (args.length > 0) {
            String expr = String.join(" ", args);
            analyzeAndPrint(expr, false);
            return;
        }

        System.out.println("=== Demonstration of the lexical and syntactic analyzer in action ===\n");
        for (String expr : DEMO_EXPRESSIONS) {
            analyzeAndPrint(expr, false);
            System.out.println();
        }

        System.out.println("=== Interactive mode (an empty line indicates exit) ===");
        boolean simplify = false;
        try (Scanner sc = new Scanner(System.in)) {
            while (true) {
                System.out.print("Enter an expression [simplify: " + (simplify ? "on" : "off") + "]> ");
                if (!sc.hasNextLine()) break;
                String line = sc.nextLine();
                if (line.isBlank()) break;
                if (line.startsWith(":s")) {
                    simplify = line.contains("on");
                    System.out.println("Simplification: " + (simplify ? "ON" : "OFF"));
                    continue;
                }
                analyzeAndPrint(line, simplify);
                System.out.println();
            }
        }
    }

    private static void analyzeAndPrint(String expr, boolean simplify) {
        System.out.println("Expression: " + expr);

        Lexer lexer = new Lexer(expr);
        List<Token> tokens = lexer.tokenize();
        System.out.print("Tokens: ");
        System.out.println(tokens.stream()
                .filter(t -> t.type() != TokenType.EOF)
                .map(Token::toString)
                .reduce((a, b) -> a + "  " + b)
                .orElse("(no tokens)"));

        ExpressionAnalyzer analyzer = new ExpressionAnalyzer(expr);
        List<AnalysisError> errors = analyzer.analyze();

        if (errors.isEmpty()) {
            System.out.println("Result: The expression is correct ✔");
            try {
                Node ast = new AstBuilder(tokens).build();
                LevelPrinter.Result before = LevelPrinter.analyze(ast);

                System.out.println("AST: " + ast);

                Map<String, Double> vars = new HashMap<>();
                for (Token t : tokens) {
                    if (t.type() == TokenType.IDENTIFIER) {
                        vars.putIfAbsent(t.text(), 1.5 + 0.7 * vars.size());
                    }
                }

                Node flat = Flattener.flatten(ast);
                System.out.println("Flat: " + flat);

                if (simplify) {
                    boolean bad = false;
                    for (Node term : ZeroDivisionChecker.terms(ast)) {
                        Node s = Simplifier.simplify(Flattener.flatten(term));
                        if (ZeroDivisionChecker.hasZeroDivisor(s)) {
                            System.out.println("  ✖ Division by zero in term: " + term + "   (simplifies to " + s + ")");
                            bad = true;
                        }
                    }
                    if (bad) {
                        System.out.println("Tree not built: fix the terms above.");
                        return;
                    }
                    flat = Simplifier.simplify(flat);
                    System.out.println("Simplified: " + flat);
                }

                Node parallel = ParallelBuilder.build(flat);
                System.out.println("Parallel: " + parallel);
                LevelPrinter.Result res = LevelPrinter.analyze(parallel);
                LevelPrinter.print(res);

                System.out.println("Commutative regrouping: height " + before.height() + " -> " + res.height()
                        + ", width " + before.width() + " -> " + res.width()
                        + (res.height() < before.height()
                        ? "  (reduced by " + (before.height() - res.height()) + ")"
                        : "  (no gain)"));

                TreeRenderer.savePng(parallel, res, "out/tree_" + (++treeCounter) + ".png");

                double v1 = Evaluator.eval(ast, vars);
                double v2 = Evaluator.eval(parallel, vars);
                boolean same = Math.abs(v1 - v2) <= 1e-9 * Math.max(1, Math.abs(v1));
                System.out.println("Value: " + v1 + ", parallel: " + v2 + (same ? "  OK" : "  MISMATCH"));
            } catch (IllegalArgumentException e) {
                System.out.println("AST: not built (" + e.getMessage() + ")");
            }
        } else {
            System.out.println("Result: errors found - " + errors.size());
            for (AnalysisError e : errors) {
                System.out.println("  ✖ " + e);
            }
        }
    }

    private static boolean containsCall(Node n) {
        if (n instanceof Call) return true;
        if (n instanceof BinOp b) return containsCall(b.left()) || containsCall(b.right());
        return false;
    }

    private static final String[] DEMO_EXPRESSIONS = {
            // K1-K5: commutative law tests
            "a-b*c+k",
            "A - B*c - L*k*2 + D*t/d*y - H + Q*3 - J*(w-1)/r + P",
            "A-B*c-J*(d*t*j-u*t+c*r-1+w-k/q+m*(n-k*s+z*(y+u*p-y/r-5)+x+t/2))/r+P",
            "a + b*c + d + e*f*g + h*i + j*(k + L + m*(n-p*q+r) - s*t)",
            "exp(sin(a+b/2-pi)+a*cos(a*pi+b*pi/3-w+k*t)-5+log(2.72)/T-1)+2048+a+b*c+log(t-1)-2*log(Q)-8*d/dt*exp(t/2+H)-sin(a)/cos(a)"
    };
}