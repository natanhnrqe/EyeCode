package com.eyecode.editor.v2.language.java.lexer;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.language.Token;
import com.eyecode.language.java.JavaTokenType;

import java.util.ArrayList;
import java.util.List;

public final class JavaLexer {

    public List<Token> tokenize(String source) {
        List<Token> tokens = new ArrayList<>();
        if (source == null || source.isEmpty()) {
            tokens.add(new Token(JavaTokenType.EOF, TextRange.of(0, 0), ""));
            return tokens;
        }

        int pos = 0;
        int len = source.length();

        while (pos < len) {
            char c = source.charAt(pos);

            if (isWhitespace(c)) {
                int start = pos;
                while (pos < len && isWhitespace(source.charAt(pos))) {
                    pos++;
                }
                tokens.add(token(JavaTokenType.WHITESPACE, start, pos, source));
                continue;
            }

            if (c == '/' && pos + 1 < len && source.charAt(pos + 1) == '/') {
                int start = pos;
                pos += 2;
                while (pos < len && source.charAt(pos) != '\n') {
                    pos++;
                }
                tokens.add(token(JavaTokenType.COMMENT, start, pos, source));
                continue;
            }

            if (c == '/' && pos + 1 < len && source.charAt(pos + 1) == '*') {
                int start = pos;
                pos += 2;
                while (pos < len) {
                    if (source.charAt(pos) == '*' && pos + 1 < len && source.charAt(pos + 1) == '/') {
                        pos += 2;
                        break;
                    }
                    pos++;
                }
                tokens.add(token(JavaTokenType.COMMENT, start, pos, source));
                continue;
            }

            if (c == '"') {
                int start = pos;
                pos++;
                while (pos < len && source.charAt(pos) != '"') {
                    if (source.charAt(pos) == '\\' && pos + 1 < len) {
                        pos += 2;
                    } else {
                        pos++;
                    }
                }
                if (pos < len) {
                    pos++;
                }
                tokens.add(token(JavaTokenType.STRING, start, pos, source));
                continue;
            }

            if (c == '\'') {
                int start = pos;
                pos++;
                while (pos < len && source.charAt(pos) != '\'') {
                    if (source.charAt(pos) == '\\' && pos + 1 < len) {
                        pos += 2;
                    } else {
                        pos++;
                    }
                }
                if (pos < len) {
                    pos++;
                }
                tokens.add(token(JavaTokenType.CHARACTER, start, pos, source));
                continue;
            }

            if (isDigit(c)) {
                int start = pos;
                pos = scanNumber(source, pos, len);
                tokens.add(token(JavaTokenType.NUMBER, start, pos, source));
                continue;
            }

            if (isJavaIdentifierStart(c)) {
                int start = pos;
                while (pos < len && isJavaIdentifierPart(source.charAt(pos))) {
                    pos++;
                }
                String lexeme = source.substring(start, pos);
                JavaTokenType type;
                if (lexeme.equals("true") || lexeme.equals("false")) {
                    type = JavaTokenType.BOOLEAN_LITERAL;
                } else if (lexeme.equals("null")) {
                    type = JavaTokenType.NULL_LITERAL;
                } else {
                    type = JavaKeywordRegistry.isKeyword(lexeme)
                            ? JavaTokenType.KEYWORD : JavaTokenType.IDENTIFIER;
                }
                tokens.add(token(type, start, pos, source));
                continue;
            }

            if (c == '@') {
                tokens.add(token(JavaTokenType.AT, pos, pos + 1, source));
                pos++;
                continue;
            }

            if (JavaSeparatorRegistry.isSeparator(c)) {
                tokens.add(token(JavaTokenType.SEPARATOR, pos, pos + 1, source));
                pos++;
                continue;
            }

            if (JavaOperatorRegistry.isOperatorStart(c)) {
                int start = pos;
                int consumed = scanOperator(source, pos, len);
                if (consumed > 0) {
                    pos += consumed;
                    tokens.add(token(JavaTokenType.OPERATOR, start, pos, source));
                    continue;
                }
            }

            tokens.add(token(JavaTokenType.ERROR, pos, pos + 1, source));
            pos++;
        }

        tokens.add(new Token(JavaTokenType.EOF, TextRange.of(pos, pos), ""));
        return tokens;
    }

    private static Token token(JavaTokenType type, int start, int end, String source) {
        return new Token(type, TextRange.of(start, end), source.substring(start, end));
    }

    private int scanNumber(String source, int pos, int len) {
        char c = source.charAt(pos);

        if (c == '0' && pos + 1 < len) {
            char next = source.charAt(pos + 1);
            if (next == 'x' || next == 'X') {
                pos += 2;
                while (pos < len && (isHexDigit(source.charAt(pos)) || source.charAt(pos) == '_')) {
                    pos++;
                }
                if (pos < len) {
                    char suffix = source.charAt(pos);
                    if (suffix == 'L' || suffix == 'l') pos++;
                }
                return pos;
            }
            if (next == 'b' || next == 'B') {
                pos += 2;
                while (pos < len && (source.charAt(pos) == '0' || source.charAt(pos) == '1' || source.charAt(pos) == '_')) {
                    pos++;
                }
                if (pos < len) {
                    char suffix = source.charAt(pos);
                    if (suffix == 'L' || suffix == 'l') pos++;
                }
                return pos;
            }
        }

        while (pos < len && (isDigit(source.charAt(pos)) || source.charAt(pos) == '_')) {
            pos++;
        }

        if (pos < len && source.charAt(pos) == '.'
                && pos + 1 < len && isDigit(source.charAt(pos + 1))) {
            pos++;
            while (pos < len && (isDigit(source.charAt(pos)) || source.charAt(pos) == '_')) {
                pos++;
            }
        }

        if (pos < len && (source.charAt(pos) == 'e' || source.charAt(pos) == 'E')) {
            pos++;
            if (pos < len && (source.charAt(pos) == '+' || source.charAt(pos) == '-')) {
                pos++;
            }
            while (pos < len && (isDigit(source.charAt(pos)) || source.charAt(pos) == '_')) {
                pos++;
            }
        }

        if (pos < len) {
            char suffix = source.charAt(pos);
            if (suffix == 'L' || suffix == 'l' || suffix == 'F' || suffix == 'f' || suffix == 'D' || suffix == 'd') {
                pos++;
            }
        }

        return pos;
    }

    private int scanOperator(String source, int pos, int len) {
        if (pos + 3 < len) {
            String four = source.substring(pos, pos + 4);
            if (JavaOperatorRegistry.isOperator(four)) return 4;
        }
        if (pos + 2 < len) {
            String three = source.substring(pos, pos + 3);
            if (JavaOperatorRegistry.isOperator(three)) return 3;
        }
        if (pos + 1 < len) {
            String two = source.substring(pos, pos + 2);
            if (JavaOperatorRegistry.isOperator(two)) return 2;
        }
        String one = String.valueOf(source.charAt(pos));
        if (JavaOperatorRegistry.isOperator(one)) return 1;
        return 0;
    }

    private boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f';
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private boolean isJavaIdentifierStart(char c) {
        return Character.isJavaIdentifierStart(c);
    }

    private boolean isJavaIdentifierPart(char c) {
        return Character.isJavaIdentifierPart(c);
    }
}
