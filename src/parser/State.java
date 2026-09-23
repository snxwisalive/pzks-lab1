package parser;

public enum State {
    START, // start of an expression or sub-expression after "(" or binary operator
    AFTER_OPERAND, // a complete operand (a number, a name, or ‘)’) has just been recognized
    AFTER_OPERATOR, // a binary (or unary) operator has just been recognized
    AFTER_LPAREN, // the opening parenthesis ‘(’ has just been recognized
    AFTER_FUNC // the function name has just been recognized
}