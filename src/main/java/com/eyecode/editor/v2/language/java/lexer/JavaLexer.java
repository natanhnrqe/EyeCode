package com.eyecode.editor.v2.language.java.lexer;

import java.util.ArrayList;
import java.util.List;

public final class JavaLexer {

    public List<JavaToken> tokenize(String source) {
        List<JavaToken> tokens = new ArrayList<>();
        if (source == null || source.isEmpty()) {
            tokens.add(new JavaToken(JavaTokenType.EOF, "", 0, 0, 0, 0));
            return tokens;
        }

        int pos = 0;
        int line = 0;
        int column = 0;
        int len = source.length();

        while (pos < len) {
            char c = source.charAt(pos);

            if (isWhitespace(c)) {
                int start = pos;
                int startLine = line;
                int startCol = column;
                while (pos < len && isWhitespace(source.charAt(pos))) {
                    if (source.charAt(pos) == '\n') {
                        line++;
                        column = 0;
                    } else {
                        column++;
                    }
                    pos++;
                }
                tokens.add(new JavaToken(JavaTokenType.WHITESPACE,
                        source.substring(start, pos), start, pos, startLine, startCol));
                continue;
            }

            if (c == '/' && pos + 1 < len && source.charAt(pos + 1) == '/') {
                int start = pos;
                int startLine = line;
                int startCol = column;
                pos += 2;
                column += 2;
                while (pos < len && source.charAt(pos) != '\n') {
                    pos++;
                    column++;
                }
                tokens.add(new JavaToken(JavaTokenType.COMMENT,
                        source.substring(start, pos), start, pos, startLine, startCol));
                continue;
            }

            if (c == '/' && pos + 1 < len && source.charAt(pos + 1) == '*') {
                int start = pos;
                int startLine = line;
                int startCol = column;
                pos += 2;
                column += 2;
                while (pos < len) {
                    if (source.charAt(pos) == '*' && pos + 1 < len && source.charAt(pos + 1) == '/') {
                        pos += 2;
                        column += 2;
                        break;
                    }
                    if (source.charAt(pos) == '\n') {
                        line++;
                        column = 0;
                    } else {
                        column++;
                    }
                    pos++;
                }
                tokens.add(new JavaToken(JavaTokenType.COMMENT,
                        source.substring(start, pos), start, pos, startLine, startCol));
                continue;
            }

            if (c == '"') {
                int start = pos;
                int startLine = line;
                int startCol = column;
                pos++;
                column++;
                while (pos < len && source.charAt(pos) != '"') {
                    if (source.charAt(pos) == '\\' && pos + 1 < len) {
                        if (source.charAt(pos + 1) == '\n') {
                            line++;
                            column = 0;
                            pos += 2;
                        } else {
                            pos += 2;
                            column += 2;
                        }
                    } else {
                        if (source.charAt(pos) == '\n') {
                            line++;
                            column = 0;
                        } else {
                            column++;
                        }
                        pos++;
                    }
                }
                if (pos < len) {
                    pos++;
                    column++;
                }
                tokens.add(new JavaToken(JavaTokenType.STRING,
                        source.substring(start, pos), start, pos, startLine, startCol));
                continue;
            }

            if (c == '\'') {
                int start = pos;
                int startLine = line;
                int startCol = column;
                pos++;
                column++;
                while (pos < len && source.charAt(pos) != '\'') {
                    if (source.charAt(pos) == '\\' && pos + 1 < len) {
                        pos += 2;
                        column += 2;
                    } else {
                        pos++;
                        column++;
                    }
                }
                if (pos < len) {
                    pos++;
                    column++;
                }
                tokens.add(new JavaToken(JavaTokenType.CHARACTER,
                        source.substring(start, pos), start, pos, startLine, startCol));
                continue;
            }

            if (isDigit(c)) {
                int start = pos;
                int startLine = line;
                int startCol = column;
                pos = scanNumber(source, pos, len);
                column += (pos - start);
                tokens.add(new JavaToken(JavaTokenType.NUMBER,
                        source.substring(start, pos), start, pos, startLine, startCol));
                continue;
            }

            if (isJavaIdentifierStart(c)) {
                int start = pos;
                int startLine = line;
                int startCol = column;
                while (pos < len && isJavaIdentifierPart(source.charAt(pos))) {
                    pos++;
                    column++;
                }
                String lexeme = source.substring(start, pos);
                JavaTokenType type = JavaKeywordRegistry.isKeyword(lexeme)
                        ? JavaTokenType.KEYWORD : JavaTokenType.IDENTIFIER;
                tokens.add(new JavaToken(type, lexeme, start, pos, startLine, startCol));
                continue;
            }

            if (JavaSeparatorRegistry.isSeparator(c)) {
                int start = pos;
                int startLine = line;
                int startCol = column;
                pos++;
                column++;
                tokens.add(new JavaToken(JavaTokenType.SEPARATOR,
                        String.valueOf(c), start, pos, startLine, startCol));
                continue;
            }

            if (JavaOperatorRegistry.isOperatorStart(c)) {
                int start = pos;
                int startLine = line;
                int startCol = column;
                int consumed = scanOperator(source, pos, len);
                if (consumed > 0) {
                    pos += consumed;
                    column += consumed;
                    tokens.add(new JavaToken(JavaTokenType.OPERATOR,
                            source.substring(start, pos), start, pos, startLine, startCol));
                    continue;
                }
            }

            tokens.add(new JavaToken(JavaTokenType.ERROR,
                    String.valueOf(c), pos, pos + 1, line, column));
            pos++;
            column++;
        }

        tokens.add(new JavaToken(JavaTokenType.EOF, "", pos, pos, line, column));
        return tokens;
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
