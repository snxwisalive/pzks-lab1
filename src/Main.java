import lexer.Lexer;
import lexer.Token;
import lexer.TokenType;
import parser.AnalysisError;
import parser.ExpressionAnalyzer;

import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        if (args.length > 0) {
            String expr = String.join(" ", args);
            analyzeAndPrint(expr);
            return;
        }

        System.out.println("=== Demonstration of the lexical and syntactic analyzer in action ===\n");
        for (String expr : DEMO_EXPRESSIONS) {
            analyzeAndPrint(expr);
            System.out.println();
        }

        System.out.println("=== Interactive mode (an empty line indicates exit) ===");
        try (Scanner sc = new Scanner(System.in)) {
            while (true) {
                System.out.print("Enter an expression> ");
                if (!sc.hasNextLine()) break;
                String line = sc.nextLine();
                if (line.isBlank()) break;
                analyzeAndPrint(line);
                System.out.println();
            }
        }
    }

    private static void analyzeAndPrint(String expr) {
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
        } else {
            System.out.println("Result: errors found - " + errors.size());
            for (AnalysisError e : errors) {
                System.out.println("  ✖ " + e);
            }
        }
    }

    private static final String[] DEMO_EXPRESSIONS = {
            // correct
            "3 + 4 * (5 - 2)",
            "-a + sin(x) / sqrt(4)",
            "(a + b) * (c - d)",

            // errors at the beginning of the expression
            ") + 3",
            "* 5 + 2",
            "/ 5 - 1",

            // errors at the end of the expression
            "3 + 4 *",
            "a - b +",
            "(3 + 4",

            // errors within an expression (duplicate operations, parentheses without operators)
            "3 + * 4",
            "5 -- 3",
            "3(4 + 5)",
            "(3 + 4)(5 + 6)",
            "(3 + * 4)",

            // errors involving parentheses
            "((3 + 4)",
            "3 + 4))",
            "()",
            "sin()",

            // errors in names / numbers / functions
            "3.14.15 + 2",
            "12abc + x",
            "sn(x) + 1",
            "2 # 3"
    };
}