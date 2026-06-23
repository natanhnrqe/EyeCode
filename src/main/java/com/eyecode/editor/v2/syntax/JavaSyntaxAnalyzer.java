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
            "volatile", "while"
    );

    @Override
    public SyntaxSnapshot analyze(EditorDocument document) {
        List<SyntaxToken> tokens = new ArrayList<>();
        String text = document.getText();
        int offset = 0;

        while (offset < text.length()) {
            char current = text.charAt(offset);
            int start = offset;

            if (Character.isWhitespace(current)) {
                while (offset < text.length() && Character.isWhitespace(text.charAt(offset))) {
                    offset++;
                }
                tokens.add(token(TokenType.WHITESPACE, start, offset, text));
            } else if (Character.isJavaIdentifierStart(current)) {
                offset++;
                while (offset < text.length() && Character.isJavaIdentifierPart(text.charAt(offset))) {
                    offset++;
                }
                String word = text.substring(start, offset);
                TokenType type = JAVA_KEYWORDS.contains(word) ? TokenType.KEYWORD : TokenType.IDENTIFIER;
                tokens.add(new SyntaxToken(type, start, offset, word));
            } else {
                offset++;
                tokens.add(token(TokenType.UNKNOWN, start, offset, text));
            }
        }

        return new SyntaxSnapshot(tokens);
    }

    private SyntaxToken token(TokenType type, int start, int end, String text) {
        return new SyntaxToken(type, start, end, text.substring(start, end));
    }
}
