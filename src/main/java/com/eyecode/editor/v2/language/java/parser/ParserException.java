package com.eyecode.editor.v2.language.java.parser;

public final class ParserException extends RuntimeException {

    private final int line;
    private final int column;
    private final String foundToken;
    private final String expectedToken;

    public ParserException(String message, int line, int column, String foundToken, String expectedToken) {
        super(message);
        this.line = line;
        this.column = column;
        this.foundToken = foundToken;
        this.expectedToken = expectedToken;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public String getFoundToken() {
        return foundToken;
    }

    public String getExpectedToken() {
        return expectedToken;
    }

    @Override
    public String getMessage() {
        return super.getMessage()
                + " [line=" + line + ", column=" + column
                + ", expected=" + expectedToken
                + ", found=" + foundToken + "]";
    }
}
