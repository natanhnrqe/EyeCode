package com.eyecode.editor.v2.language.java.lexer;

import java.util.Set;

public final class JavaSeparatorRegistry {

    private static final Set<Character> SEPARATORS = Set.of(
            '(', ')', '{', '}', '[', ']', ';', ',', '.'
    );

    private JavaSeparatorRegistry() {
    }

    public static boolean isSeparator(char c) {
        return SEPARATORS.contains(c);
    }

    public static boolean isSeparator(String text) {
        return text != null && text.length() == 1 && SEPARATORS.contains(text.charAt(0));
    }
}
