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

        System.out.println("=== Simplification tests ===\n");
        for (String expr : SIMPLIFY_EXPRESSIONS) {
            analyzeAndPrint(expr, true);
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

                System.out.println("AST: " + ast);

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
                TreeRenderer.savePng(parallel, res, "out/tree_" + (++treeCounter) + ".png");

                Map<String, Double> vars = new HashMap<>();
                for (Token t : tokens) {
                    if (t.type() == TokenType.IDENTIFIER) {
                        vars.putIfAbsent(t.text(), 1.5 + 0.7 * vars.size());
                    }
                }
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

    private static final String[] DEMO_EXPRESSIONS = {
            // M1-M4: mandatory
            "a+b+c+d+e+f+g+h",
            "a-b-c-d-e-f-g-h",
            "a/b/c/d/e/f/g/h",
            "a*(b-4) - 2*b*c - c*d - a*c*d/e/f/g - g-h-i-j",

            // P1-P2: parentheses
            "a+(b+c+d+(e+f)+g)+h",
            "a-((b-c-d)-(e-f)-g)-h",

            // E1-E3: expected values 0.125, -35, 39
            "5040/8/7/6/5/4/3/2",
            "10-9-8-7-6-5-4-3-2-1",
            "64-(32-16)-8-(4-2-1)"
    };

    private static final String[] SIMPLIFY_EXPRESSIONS = {
            "0-(0-i)/1.0 + 0 - 0*k*h + 2 - 4.8/2 + 1*e/2",
            "a*2/0 + b/(b+b*0-1*b) - 1/(c*2*4.76*(1-2+1))"
    };
}