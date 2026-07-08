package com.eyecode.editor.v2.language.java.lexer;

import java.util.Set;

public final class JavaOperatorRegistry {

    private static final Set<String> OPERATORS = Set.of(
            "+", "-", "*", "/", "%",
            "=", ">", "<", "!", "~", "?", ":",
            "==", "<=", ">=", "!=", "&&", "||",
            "++", "--",
            "+=", "-=", "*=", "/=", "%=",
            "&=", "|=", "^=", "<<=", ">>=", ">>>=",
            "&", "|", "^",
            "<<", ">>", ">>>",
            "->", "::"
    );

    private JavaOperatorRegistry() {
    }

    public static boolean isOperator(String text) {
        return text != null && OPERATORS.contains(text);
    }

    public static boolean isOperatorStart(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '%'
                || c == '=' || c == '>' || c == '<' || c == '!' || c == '~'
                || c == '?' || c == ':' || c == '&' || c == '|' || c == '^';
    }
}
