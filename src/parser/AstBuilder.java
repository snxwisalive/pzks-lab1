package parser;

import lexer.Token;
import lexer.TokenType;
import tree.BinOp;
import tree.Call;
import tree.Leaf;
import tree.Node;

import java.util.List;

public class AstBuilder {
    private final List<Token> tokens;
    private int pos = 0;

    public AstBuilder(List<Token> tokens) {
        this.tokens = tokens;
    }

    public Node build() {
        Node root = parseExpr();
        if (peek().type() != TokenType.EOF) {
            throw new IllegalArgumentException("Unexpected token: " + peek());
        }
        return root;
    }

    // expr := term (('+' | '-') term)*
    private Node parseExpr() {
        Node left = parseTerm();
        while (peek().type() == TokenType.PLUS || peek().type() == TokenType.MINUS) {
            char op = advance().text().charAt(0);
            Node right = parseTerm();
            left = new BinOp(op, left, right);
        }
        return left;
    }

    // term := factor (('*' | '/') factor)*
    private Node parseTerm() {
        Node left = parseFactor();
        while (peek().type() == TokenType.STAR || peek().type() == TokenType.SLASH) {
            char op = advance().text().charAt(0);
            Node right = parseFactor();
            left = new BinOp(op, left, right);
        }
        return left;
    }

    // factor := NUMBER | IDENTIFIER | FUNCTION '(' expr ')' | '(' expr ')'
    private Node parseFactor() {
        Token t = advance();
        switch (t.type()) {
            case NUMBER, IDENTIFIER:
                return new Leaf(t.text());
            case LPAREN:
                Node inner = parseExpr();
                if (advance().type() != TokenType.RPAREN) {
                    throw new IllegalArgumentException("Expected ')'");
                }
                return inner;
            case FUNCTION:
                if (advance().type() != TokenType.LPAREN) {
                    throw new IllegalArgumentException("Expected '(' after " + t.text());
                }
                Node arg = parseExpr();
                if (advance().type() != TokenType.RPAREN) {
                    throw new IllegalArgumentException("Expected ')'");
                }
                return new Call(t.text().toLowerCase(), arg);
            default:
                throw new IllegalArgumentException("Unsupported token: " + t);
        }
    }

    private Token peek()    { return tokens.get(pos); }
    private Token advance() { return tokens.get(pos++); }
}