package com.eyecode.language.java;

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
