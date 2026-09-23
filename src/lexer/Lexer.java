package lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Lexer {
    public static final Set<String> KNOWN_FUNCTIONS = Set.of(
            "sin", "cos", "tan", "cot", "sqrt", "log", "ln", "exp", "abs",
            "asin", "acos", "atan"
    );

    private final String src;
    private int pos = 0;

    private final List<Token> tokens = new ArrayList<>();
    private final List<String> lexicalErrors = new ArrayList<>();

    public Lexer(String src) {
        this.src = src == null ? "" : src;
    }

    // parses the entire line and returns a list of tokens
    public List<Token> tokenize() {
        while (pos < src.length()) {
            char c = src.charAt(pos);

            if (Character.isWhitespace(c)) {
                pos++;
                continue;
            }

            int start = pos;

            if (Character.isDigit(c) || c == '.') {
                lexNumber(start);
            } else if (Character.isLetter(c) || c == '_') {
                lexIdentifier(start);
            } else {
                lexSymbol(c, start);
            }
        }
        tokens.add(new Token(TokenType.EOF, "", src.length()));
        return tokens;
    }

    private void lexSymbol(char c, int start) {
        switch (c) {
            case '+' -> tokens.add(new Token(TokenType.PLUS, "+", start));
            case '-' -> tokens.add(new Token(TokenType.MINUS, "-", start));
            case '*' -> tokens.add(new Token(TokenType.STAR, "*", start));
            case '/' -> tokens.add(new Token(TokenType.SLASH, "/", start));
            case '^' -> tokens.add(new Token(TokenType.CARET, "^", start));
            case '(' -> tokens.add(new Token(TokenType.LPAREN, "(", start));
            case ')' -> tokens.add(new Token(TokenType.RPAREN, ")", start));
            default -> {
                tokens.add(new Token(TokenType.UNKNOWN, String.valueOf(c), start));
                lexicalErrors.add("Position %d: unknown symbol '%s'".formatted(start, c));
            }
        }
        pos++;
    }

    private void lexNumber(int start) {
        StringBuilder sb = new StringBuilder();
        int dotCount = 0;

        while (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.')) {
            if (src.charAt(pos) == '.') dotCount++;
            sb.append(src.charAt(pos));
            pos++;
        }

        // if letters follow numbers immediately without a space or operator, it's an error,
        // not separate tokens “number” + “name” (e.g., ‘12abc’, “3x”).
        if (pos < src.length() && (Character.isLetter(src.charAt(pos)) || src.charAt(pos) == '_')) {
            while (pos < src.length() && (Character.isLetterOrDigit(src.charAt(pos)) || src.charAt(pos) == '_')) {
                sb.append(src.charAt(pos));
                pos++;
            }
            String text = sb.toString();
            tokens.add(new Token(TokenType.INVALID_IDENTIFIER, text, start));
            lexicalErrors.add("Position %d: incorrect name '%s' (a number cannot be directly aligned with letters)"
                    .formatted(start, text));
            return;
        }

        String text = sb.toString();
        boolean malformed = dotCount > 1 || text.equals(".");
        if (malformed) {
            tokens.add(new Token(TokenType.INVALID_NUMBER, text, start));
            lexicalErrors.add("Position %d: incorrect number format '%s'".formatted(start, text));
        } else {
            tokens.add(new Token(TokenType.NUMBER, text, start));
        }
    }

    private void lexIdentifier(int start) {
        StringBuilder sb = new StringBuilder();
        while (pos < src.length() && (Character.isLetterOrDigit(src.charAt(pos)) || src.charAt(pos) == '_')) {
            sb.append(src.charAt(pos));
            pos++;
        }
        String name = sb.toString();

        int look = pos;
        while (look < src.length() && Character.isWhitespace(src.charAt(look))) look++;
        boolean followedByParen = look < src.length() && src.charAt(look) == '(';

        if (followedByParen) {
            tokens.add(new Token(TokenType.FUNCTION, name, start));
            if (!KNOWN_FUNCTIONS.contains(name.toLowerCase())) {
                String hint = suggest(name);
                lexicalErrors.add("Position %d: unknown function '%s'.%s"
                        .formatted(start, name, hint == null ? "" : " Maybe you meant '" + hint + "'?"));
            }
        } else {
            tokens.add(new Token(TokenType.IDENTIFIER, name, start));
        }
    }

    // suggests closest known function (if distance is not big)
    private String suggest(String name) {
        String best = null;
        int bestDist = Integer.MAX_VALUE;
        String lower = name.toLowerCase();
        for (String f : KNOWN_FUNCTIONS) {
            int d = levenshtein(lower, f);
            if (d < bestDist) {
                bestDist = d;
                best = f;
            }
        }
        return bestDist <= 2 ? best : null;
    }

    private int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[a.length()][b.length()];
    }

    // errors
    public List<String> getLexicalErrors() {
        return lexicalErrors;
    }
}