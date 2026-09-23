package lexer;

public enum TokenType {
    NUMBER,             // 123, 3.14
    IDENTIFIER,         // var name: x, y1, temp_value, aboba
    FUNCTION,           // function name ending with '(' : sin, cos, sqrt ...
    PLUS, MINUS, STAR, SLASH, CARET,
    LPAREN, RPAREN,
    UNKNOWN,            // a symbol that does not belong to any alphabet of the language of expressions
    INVALID_NUMBER,     // incorrect number format: 3.14.15
    INVALID_IDENTIFIER, // a number aligned with text without an operator: 12abc
    EOF                 // an end of an input line
}