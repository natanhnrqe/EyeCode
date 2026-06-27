package com.eyecode.editor.v2.syntax;

import com.eyecode.editor.v2.EditorDocument;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class JavaSyntaxAnalyzer implements SyntaxAnalyzer {

    private static final Set<String> JAVA_KEYWORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native", "new", "package",
            "private", "protected", "public", "return", "short", "static", "strictfp", "super",
            "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
            "volatile", "while", "var", "yield", "record", "sealed", "permits", "non-sealed"
    );

    private static final Set<String> BOOLEAN_LITERALS = Set.of("true", "false", "null");

    @Override
    public SyntaxSnapshot analyze(EditorDocument document) {
        List<SyntaxToken> tokens = new ArrayList<>();
        String text = document.getText();
        int offset = 0;
        int length = text.length();

        while (offset < length) {
            char current = text.charAt(offset);
            int start = offset;

            if (current == '/' && offset + 1 < length && text.charAt(offset + 1) == '/') {
                offset += 2;
                while (offset < length && text.charAt(offset) != '\n') {
                    offset++;
                }
                tokens.add(token(TokenType.COMMENT, start, offset, text));
            } else if (current == '/' && offset + 1 < length && text.charAt(offset + 1) == '*') {
                offset += 2;
                while (offset < length) {
                    if (text.charAt(offset) == '*' && offset + 1 < length && text.charAt(offset + 1) == '/') {
                        offset += 2;
                        break;
                    }
                    offset++;
                }
                tokens.add(token(TokenType.COMMENT, start, offset, text));
            } else if (current == '"') {
                offset++;
                while (offset < length) {
                    char ch = text.charAt(offset);
                    if (ch == '\\' && offset + 1 < length) {
                        offset += 2;
                        continue;
                    }
                    if (ch == '"') {
                        offset++;
                        break;
                    }
                    offset++;
                }
                tokens.add(token(TokenType.STRING, start, offset, text));
            } else if (current == '\'') {
                offset++;
                while (offset < length) {
                    char ch = text.charAt(offset);
                    if (ch == '\\' && offset + 1 < length) {
                        offset += 2;
                        continue;
                    }
                    if (ch == '\'') {
                        offset++;
                        break;
                    }
                    offset++;
                }
                tokens.add(token(TokenType.STRING, start, offset, text));
            } else if (Character.isWhitespace(current)) {
                while (offset < length && Character.isWhitespace(text.charAt(offset))) {
                    offset++;
                }
                tokens.add(token(TokenType.WHITESPACE, start, offset, text));
            } else if (Character.isDigit(current)) {
                offset++;
                while (offset < length && (Character.isJavaIdentifierPart(text.charAt(offset))
                        || text.charAt(offset) == '.'
                        || text.charAt(offset) == '_'
                        || text.charAt(offset) == 'x'
                        || text.charAt(offset) == 'X'
                        || text.charAt(offset) == 'b'
                        || text.charAt(offset) == 'B'
                        || text.charAt(offset) == 'l'
                        || text.charAt(offset) == 'L'
                        || text.charAt(offset) == 'f'
                        || text.charAt(offset) == 'F'
                        || text.charAt(offset) == 'd'
                        || text.charAt(offset) == 'D')) {
                    offset++;
                }
                tokens.add(token(TokenType.NUMBER, start, offset, text));
            } else if (current == '@' && offset + 1 < length && Character.isJavaIdentifierStart(text.charAt(offset + 1))) {
                offset++;
                while (offset < length && Character.isJavaIdentifierPart(text.charAt(offset))) {
                    offset++;
                }
                tokens.add(token(TokenType.ANNOTATION, start, offset, text));
            } else if (Character.isJavaIdentifierStart(current)) {
                offset++;
                while (offset < length && Character.isJavaIdentifierPart(text.charAt(offset))) {
                    offset++;
                }
                String word = text.substring(start, offset);
                TokenType type = JAVA_KEYWORDS.contains(word)
                        ? TokenType.KEYWORD
                        : BOOLEAN_LITERALS.contains(word)
                        ? TokenType.KEYWORD
                        : TokenType.IDENTIFIER;
                tokens.add(new SyntaxToken(type, start, offset, word));
            } else if (isOperator(current)) {
                offset++;
                while (offset < length && isOperator(text.charAt(offset))) {
                    offset++;
                }
                tokens.add(token(TokenType.OPERATOR, start, offset, text));
            } else if (isSeparator(current)) {
                offset++;
                tokens.add(token(TokenType.SEPARATOR, start, offset, text));
            } else {
                offset++;
                tokens.add(token(TokenType.UNKNOWN, start, offset, text));
            }
        }

        return new SyntaxSnapshot(tokens);
    }

    private boolean isOperator(char ch) {
        return switch (ch) {
            case '+', '-', '*', '/', '%', '=', '!', '<', '>', '&', '|', '^', '~', '?', ':', '.' -> true;
            default -> false;
        };
    }

    private boolean isSeparator(char ch) {
        return switch (ch) {
            case '(', ')', '{', '}', '[', ']', ',', ';' -> true;
            default -> false;
        };
    }

    private SyntaxToken token(TokenType type, int start, int end, String text) {
        return new SyntaxToken(type, start, end, text.substring(start, end));
    }
}
