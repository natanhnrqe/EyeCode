package com.eyecode.editor.v2.completion.insert;

import com.eyecode.editor.v2.language.LanguageContext;

public final class CompletionPrefixResolver {

    public String resolve(LanguageContext context) {
        return resolvePrefix(context);
    }

    public static String resolvePrefix(LanguageContext context) {
        int offset = context.getDocument().offsetOf(context.getCaret());
        String text = context.getDocument().getText();
        int safeOffset = Math.max(0, Math.min(offset, text.length()));
        int start = safeOffset;

        while (start > 0 && Character.isJavaIdentifierPart(text.charAt(start - 1))) {
            start--;
        }

        return text.substring(start, safeOffset);
    }

    public static boolean isQualifiedContext(LanguageContext context) {
        int offset = context.getDocument().offsetOf(context.getCaret());
        String text = context.getDocument().getText();
        int safeOffset = Math.max(0, Math.min(offset, text.length()));
        int start = safeOffset;

        while (start > 0 && Character.isJavaIdentifierPart(text.charAt(start - 1))) {
            start--;
        }

        if (start <= 0 || text.charAt(start - 1) != '.') {
            return false;
        }

        int qualifierEnd = start - 1;
        int qualifierStart = qualifierEnd;
        while (qualifierStart > 0 && Character.isJavaIdentifierPart(text.charAt(qualifierStart - 1))) {
            qualifierStart--;
        }
        return qualifierStart < qualifierEnd;
    }
}
