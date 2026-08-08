package com.eyecode.editor.v2.language.java.lexer;

import com.eyecode.editor.intelligence.document.LineMap;
import com.eyecode.editor.v2.language.java.parser.ParserException;
import com.eyecode.language.Token;
import com.eyecode.language.java.JavaTokenType;

import java.util.List;

public final class JavaTokenStream {

    private final List<Token> tokens;
    private final LineMap lineMap;
    private int position;

    public JavaTokenStream(List<Token> tokens, String source) {
        this.tokens = tokens;
        this.lineMap = LineMap.of(source == null ? "" : source);
        this.position = 0;
    }

    public Token peek() {
        return tokens.get(position);
    }

    public Token peek(int offset) {
        int index = position + offset;
        if (index < 0) {
            index = 0;
        }
        if (index >= tokens.size()) {
            index = tokens.size() - 1;
        }
        return tokens.get(index);
    }

    public Token consume() {
        Token token = tokens.get(position);
        if (position < tokens.size() - 1) {
            position++;
        }
        return token;
    }

    public boolean match(JavaTokenType type) {
        if (peek().type() == type) {
            consume();
            return true;
        }
        return false;
    }

    public boolean match(JavaTokenType type, String text) {
        Token current = peek();
        if (current.type() == type && current.text().equals(text)) {
            consume();
            return true;
        }
        return false;
    }

    public Token expect(JavaTokenType type) {
        Token current = peek();
        if (current.type() != type) {
            throw new ParserException(
                    "Unexpected token: expected " + type,
                    lineOf(current),
                    columnOf(current),
                    current.text(),
                    type.name()
            );
        }
        return consume();
    }

    public Token expect(JavaTokenType type, String text) {
        Token current = peek();
        if (current.type() != type || !current.text().equals(text)) {
            throw new ParserException(
                    "Unexpected token: expected " + type + " \"" + text + "\"",
                    lineOf(current),
                    columnOf(current),
                    current.text(),
                    type.name() + " \"" + text + "\""
            );
        }
        return consume();
    }

    public boolean isEOF() {
        return peek().type() == JavaTokenType.EOF;
    }

    public boolean hasNext() {
        return !isEOF();
    }

    public Token current() {
        return tokens.get(position);
    }

    public Token previous() {
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

    private int lineOf(Token token) {
        return lineMap.lineOfOffset(token.startOffset());
    }

    private int columnOf(Token token) {
        return lineMap.columnOfOffset(token.startOffset());
    }
}
