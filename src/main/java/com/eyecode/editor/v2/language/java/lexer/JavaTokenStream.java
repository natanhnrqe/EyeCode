package com.eyecode.editor.v2.language.java.lexer;

import com.eyecode.editor.v2.language.java.parser.ParserException;

import java.util.List;

public final class JavaTokenStream {

    private final List<JavaToken> tokens;
    private int position;

    public JavaTokenStream(List<JavaToken> tokens) {
        this.tokens = tokens;
        this.position = 0;
    }

    public JavaToken peek() {
        return tokens.get(position);
    }

    public JavaToken peek(int offset) {
        int index = position + offset;
        if (index < 0) {
            index = 0;
        }
        if (index >= tokens.size()) {
            index = tokens.size() - 1;
        }
        return tokens.get(index);
    }

    public JavaToken consume() {
        JavaToken token = tokens.get(position);
        if (position < tokens.size() - 1) {
            position++;
        }
        return token;
    }

    public boolean match(JavaTokenType type) {
        if (peek().getType() == type) {
            consume();
            return true;
        }
        return false;
    }

    public boolean match(JavaTokenType type, String lexeme) {
        JavaToken current = peek();
        if (current.getType() == type && current.getLexeme().equals(lexeme)) {
            consume();
            return true;
        }
        return false;
    }

    public JavaToken expect(JavaTokenType type) {
        JavaToken current = peek();
        if (current.getType() != type) {
            throw new ParserException(
                    "Unexpected token: expected " + type,
                    current.getLine(),
                    current.getColumn(),
                    current.getLexeme(),
                    type.name()
            );
        }
        return consume();
    }

    public JavaToken expect(JavaTokenType type, String lexeme) {
        JavaToken current = peek();
        if (current.getType() != type || !current.getLexeme().equals(lexeme)) {
            throw new ParserException(
                    "Unexpected token: expected " + type + " \"" + lexeme + "\"",
                    current.getLine(),
                    current.getColumn(),
                    current.getLexeme(),
                    type.name() + " \"" + lexeme + "\""
            );
        }
        return consume();
    }

    public boolean isEOF() {
        return peek().getType() == JavaTokenType.EOF;
    }

    public boolean hasNext() {
        return !isEOF();
    }

    public JavaToken current() {
        return tokens.get(position);
    }

    public JavaToken previous() {
        if (position > 0) {
            return tokens.get(position - 1);
        }
        return tokens.get(0);
    }

    public int mark() {
        return position;
    }

    public void reset(int mark) {
        this.position = mark;
    }

    public int position() {
        return position;
    }

    public int size() {
        return tokens.size();
    }
}
