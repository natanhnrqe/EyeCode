package com.eyecode.editor.v2.completion.project;

import com.eyecode.editor.v2.language.LanguageContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ProjectCompletionContextResolver {

    private ProjectCompletionContextResolver() {
    }

    public static String resolveOwnerType(LanguageContext context) {
        String text = context.getDocument().getText();
        int offset = offsetForPosition(context);
        int safeOffset = Math.max(0, Math.min(offset, text.length()));

        if (safeOffset < 1 || safeOffset > text.length()) return null;
        if (text.charAt(safeOffset - 1) != '.') return null;

        int identEnd = safeOffset - 1;
        int identStart = identEnd;
        while (identStart > 0 && Character.isJavaIdentifierPart(text.charAt(identStart - 1))) {
            identStart--;
        }

        if (identStart >= identEnd) return null;
        String variableName = text.substring(identStart, identEnd);

        return resolveType(text, variableName);
    }

    private static String resolveType(String text, String variableName) {
        Pattern pattern = Pattern.compile(
                "\\b([A-Z]\\w*(?:<[^>]*>)?)\\s+" + Pattern.quote(variableName) + "\\s*(?:[=;])",
                Pattern.MULTILINE
        );
        Matcher matcher = pattern.matcher(text);
        String type = null;
        while (matcher.find()) {
            type = matcher.group(1);
            int lt = type.indexOf('<');
            if (lt >= 0) type = type.substring(0, lt);
        }
        return type;
    }

    private static int offsetForPosition(LanguageContext context) {
        String text = context.getDocument().getText();
        int line = 0;
        int column = 0;
        for (int offset = 0; offset < text.length(); offset++) {
            if (line == context.getCaret().line() && column == context.getCaret().column()) {
                return offset;
            }
            char current = text.charAt(offset);
            if (current == '\n') {
                line++;
                column = 0;
            } else {
                column++;
            }
        }
        return text.length();
    }
}
