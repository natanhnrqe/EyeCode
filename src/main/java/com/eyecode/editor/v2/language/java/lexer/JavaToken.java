package com.eyecode.editor.v2.language.java.lexer;

public final class JavaToken {

    private final JavaTokenType type;
    private final String lexeme;
    private final int startOffset;
    private final int endOffset;
    private final int line;
    private final int column;

    public JavaToken(JavaTokenType type, String lexeme, int startOffset, int endOffset, int line, int column) {
        this.type = type;
        this.lexeme = lexeme;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.line = line;
        this.column = column;
    }

    public JavaTokenType getType() {
        return type;
    }

    public String getLexeme() {
        return lexeme;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public int getLength() {
        return endOffset - startOffset;
    }
}
