package com.eyecode.editor.v2.completion;

import com.eyecode.editor.v2.language.LanguageContext;

public final class CompletionContextResolver {

    private CompletionContextResolver() {
    }

    public static CompletionContextKind resolve(LanguageContext context) {
        String text = context.getDocument().getText();
        int offset = Math.max(0, Math.min(context.getDocument().offsetOf(context.getCaret()), text.length()));
        int prefixStart = offset;
        while (prefixStart > 0 && Character.isJavaIdentifierPart(text.charAt(prefixStart - 1))) {
            prefixStart--;
        }
        if (prefixStart > 0 && text.charAt(prefixStart - 1) == '.') {
            return CompletionContextKind.MEMBER_ACCESS;
        }
        int lineStart = Math.max(0, text.lastIndexOf('\n', Math.max(0, offset - 1)) + 1);
        if (text.substring(lineStart, prefixStart).trim().startsWith("import")) {
            return CompletionContextKind.IMPORT;
        }
        if (prefixStart < offset && Character.isUpperCase(text.charAt(prefixStart))) {
            return CompletionContextKind.TYPE;
        }
        return CompletionContextKind.IDENTIFIER;
    }
}
