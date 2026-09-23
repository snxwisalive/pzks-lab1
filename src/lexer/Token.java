package lexer;

public record Token(TokenType type, String text, int position) {
    @Override
    public String toString() {
        return "%s('%s' @%d)".formatted(type, text, position);
    }
}