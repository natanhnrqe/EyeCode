package com.eyecode.editor.v2.language.java.lexer;

import java.util.Set;

public final class JavaKeywordRegistry {

    private static final Set<String> KEYWORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch",
            "char", "class", "const", "continue", "default", "do", "double",
            "else", "enum", "extends", "final", "finally", "float", "for", "goto",
            "if", "implements", "import", "instanceof", "int", "interface", "long",
            "native", "new", "package", "private", "protected", "public", "record",
            "return", "sealed", "short", "static", "strictfp", "super", "switch",
            "synchronized", "this", "throw", "throws", "transient", "try", "void",
            "volatile", "while", "yield", "var", "permits", "non-sealed"
    );

    private JavaKeywordRegistry() {
    }

    public static boolean isKeyword(String text) {
        return text != null && KEYWORDS.contains(text);
    }
}
