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

        if (safeOffset < 1) {
            return null;
        }

        int pos = safeOffset;
        while (pos > 0 && Character.isJavaIdentifierPart(text.charAt(pos - 1))) {
            pos--;
        }

        if (pos < 1 || text.charAt(pos - 1) != '.') {
            return null;
        }

        int dotPos = pos - 1;
        int varEnd = dotPos;
        int varStart = varEnd;
        while (varStart > 0 && Character.isJavaIdentifierPart(text.charAt(varStart - 1))) {
            varStart--;
        }

        if (varStart >= varEnd) {
            return null;
        }

        String variableName = text.substring(varStart, varEnd);
        return resolveType(text, variableName);
    }

    private static String resolveType(String text, String variableName) {
        String regex = "\\b([A-Z]\\w*(?:<[^>]*>)?)\\s+" + Pattern.quote(variableName) + "\\s*(?:[=;])";
        Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
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
