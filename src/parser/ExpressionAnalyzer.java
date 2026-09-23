package parser;

import lexer.Lexer;
import lexer.Token;
import lexer.TokenType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class ExpressionAnalyzer {

    private final String source;
    private List<Token> tokens;
    private final List<AnalysisError> errors = new ArrayList<>();

    public ExpressionAnalyzer(String source) {
        this.source = source;
    }

    // performs a complete (lexical + syntactic) analysis and returns a list of all errors.
    public List<AnalysisError> analyze() {
        Lexer lexer = new Lexer(source);
        this.tokens = lexer.tokenize();

        for (String msg : lexer.getLexicalErrors()) {
            // lexer returns a msg in "Position N:..." format, parse it backwards.
            int idx = msg.indexOf(':');
            int position = 0;
            try {
                position = Integer.parseInt(msg.replaceAll("\\D+(\\d+).*", "$1"));
            } catch (NumberFormatException ignored) {
                // 0 if it couldn't parse
            }
            errors.add(new AnalysisError(position, msg.substring(idx + 2)));
        }

        runFsm();

        errors.sort((a, b) -> Integer.compare(a.position(), b.position()));
        return errors;
    }

    private void error(int pos, String message) {
        errors.add(new AnalysisError(pos, message));
    }

    // the main loop of a finite-state machine
    private void runFsm() {
        // ignore an empty expression - parses separately
        boolean onlyEof = tokens.size() == 1 && tokens.get(0).type() == TokenType.EOF;
        if (onlyEof) {
            error(0, "вираз порожній");
            return;
        }

        State state = State.START;
        Deque<Integer> parenStack = new ArrayDeque<>(); // позиції відкриваючих дужок
        String pendingFuncName = null;

        int i = 0;
        while (i < tokens.size()) {
            Token tok = tokens.get(i);
            TokenType t = tok.type();
            int p = tok.position();

            if (t == TokenType.EOF) {
                switch (state) {
                    case START -> error(p, "an expression cannot end without any operands.");
                    case AFTER_OPERATOR -> error(p, "an expression cannot end with an operator");
                    case AFTER_LPAREN -> error(p, "an expression cannot end with an opening parenthesis");
                    case AFTER_FUNC -> error(p, "after function's name '%s' is expecting '('".formatted(pendingFuncName));
                    case AFTER_OPERAND -> { /* proper completion */ }
                }
                while (!parenStack.isEmpty()) {
                    error(parenStack.pop(), "the opening bracket has no corresponding closing bracket");
                }
                break;
            }

            switch (state) {
                case START -> {
                    switch (t) {
                        case NUMBER, IDENTIFIER, INVALID_NUMBER, INVALID_IDENTIFIER, UNKNOWN -> state = State.AFTER_OPERAND;
                        case FUNCTION -> { pendingFuncName = tok.text(); state = State.AFTER_FUNC; }
                        case LPAREN -> { parenStack.push(p); state = State.AFTER_LPAREN; }
                        case PLUS, MINUS -> state = State.AFTER_OPERATOR; // унарний знак на початку - дозволено
                        case STAR, SLASH, CARET ->
                                error(p, "an expression cannot begin with an operation '%s'".formatted(tok.text()));
                        case RPAREN ->
                                error(p, "an expression cannot begin with a closing parenthesis ')'");
                        default -> { }
                    }
                    if ((t == TokenType.STAR || t == TokenType.SLASH || t == TokenType.CARET)) {
                        state = State.AFTER_OPERATOR; // resume: we believe that an operand should follow
                    } else if (t == TokenType.RPAREN) {
                        // resume: The symbol is ignored; we remain in the START state
                        state = State.START;
                    }
                }

                case AFTER_FUNC -> {
                    if (t == TokenType.LPAREN) {
                        parenStack.push(p);
                        state = State.AFTER_LPAREN;
                    } else {
                        error(p, "after function's name '%s' is expecting opening parenthesis '('".formatted(pendingFuncName));
                        // we process this same token again, but now in the START state,
                        // without advancing the pointer—this is what error recovery entails.
                        state = State.START;
                        continue;
                    }
                }

                case AFTER_OPERAND -> {
                    switch (t) {
                        case NUMBER, IDENTIFIER, INVALID_NUMBER, INVALID_IDENTIFIER -> {
                            error(p, "missing operator before '%s'".formatted(tok.text()));
                            state = State.AFTER_OPERAND;
                        }
                        case UNKNOWN -> state = State.AFTER_OPERAND; // символ уже позначено лексером
                        case FUNCTION -> {
                            error(p, "no operator before the function call '%s'".formatted(tok.text()));
                            pendingFuncName = tok.text();
                            state = State.AFTER_FUNC;
                        }
                        case LPAREN -> {
                            error(p, "there is no operator before the opening parenthesis '('");
                            parenStack.push(p);
                            state = State.AFTER_LPAREN;
                        }
                        case RPAREN -> {
                            if (parenStack.isEmpty()) {
                                error(p, "an extra closing parenthesis ‘)’ without a corresponding opening one");
                            } else {
                                parenStack.pop();
                            }
                            state = State.AFTER_OPERAND;
                        }
                        case PLUS, MINUS, STAR, SLASH, CARET -> state = State.AFTER_OPERATOR;
                        default -> { }
                    }
                }

                case AFTER_OPERATOR -> {
                    switch (t) {
                        case NUMBER, IDENTIFIER, INVALID_NUMBER, INVALID_IDENTIFIER, UNKNOWN -> state = State.AFTER_OPERAND;
                        case FUNCTION -> { pendingFuncName = tok.text(); state = State.AFTER_FUNC; }
                        case LPAREN -> { parenStack.push(p); state = State.AFTER_LPAREN; }
                        case PLUS, MINUS -> {
                            // the second symbol in a row is a double operation (a unary operation is allowed only once)
                            error(p, "double operation: the ‘%s’ symbol immediately follows another operator".formatted(tok.text()));
                            state = State.AFTER_OPERATOR;
                        }
                        case STAR, SLASH, CARET -> {
                            error(p, "double operator: ‘%s’ cannot immediately follow another operator".formatted(tok.text()));
                            state = State.AFTER_OPERATOR;
                        }
                        case RPAREN -> {
                            error(p, "There is no operand before the closing parenthesis ')'");
                            if (!parenStack.isEmpty()) parenStack.pop();
                            state = State.AFTER_OPERAND;
                        }
                        default -> { }
                    }
                }

                case AFTER_LPAREN -> {
                    switch (t) {
                        case NUMBER, IDENTIFIER, INVALID_NUMBER, INVALID_IDENTIFIER, UNKNOWN -> state = State.AFTER_OPERAND;
                        case FUNCTION -> { pendingFuncName = tok.text(); state = State.AFTER_FUNC; }
                        case LPAREN -> { parenStack.push(p); state = State.AFTER_LPAREN; }
                        case PLUS, MINUS -> state = State.AFTER_OPERATOR; // a unary operator at the beginning of a sub-expression
                        case STAR, SLASH, CARET ->
                                error(p, "the operation ‘%s’ cannot follow immediately after the opening parenthesis '('".formatted(tok.text()));
                        case RPAREN -> {
                            error(p, "empty parentheses ‘()’ are not allowed");
                            if (!parenStack.isEmpty()) parenStack.pop();
                        }
                        default -> { }
                    }
                    if (t == TokenType.STAR || t == TokenType.SLASH || t == TokenType.CARET) {
                        state = State.AFTER_OPERATOR; // resume
                    } else if (t == TokenType.RPAREN) {
                        state = State.AFTER_OPERAND; // we treat “()” as a (false) operand and move on
                    }
                }
            }

            i++;
        }
    }
}