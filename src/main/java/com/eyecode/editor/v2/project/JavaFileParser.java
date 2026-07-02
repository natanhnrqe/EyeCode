package com.eyecode.editor.v2.project;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionItemKind;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JavaFileParser {

    private static final Pattern TYPE_PATTERN = Pattern.compile(
            "\\b(class|interface|enum|record)\\s+([A-Za-z_]\\w*)" +
            "(?:\\s*<[^>]+>)?(?:\\s+(?:extends|implements|permits)\\s+[^{]+)?"
    );

    public List<CompletionItem> parse(String source) {
        List<CompletionItem> items = new ArrayList<>();
        if (source == null || source.isBlank()) return items;

        String cleaned = stripCommentsAndStrings(source);
        Matcher matcher = TYPE_PATTERN.matcher(cleaned);

        while (matcher.find()) {
            String kind = matcher.group(1);
            String name = matcher.group(2);

            if (!isValidDeclaration(cleaned, matcher.start())) continue;

            CompletionItemKind itemKind = switch (kind) {
                case "interface" -> CompletionItemKind.INTERFACE;
                case "enum" -> CompletionItemKind.ENUM;
                case "record" -> CompletionItemKind.RECORD;
                default -> CompletionItemKind.CLASS;
            };

            items.add(CompletionItem.builder(name, name, itemKind)
                    .detail(name)
                    .category(kind.substring(0, 1).toUpperCase() + kind.substring(1))
                    .build());
        }

        return items;
    }

    private boolean isValidDeclaration(String text, int matchStart) {
        if (matchStart <= 0) return true;
        char before = text.charAt(matchStart - 1);
        return before == ' ' || before == '\t' || before == '\n' || before == '\r'
                || before == '{' || before == ';' || before == '(' || before == ')';
    }

    private String stripCommentsAndStrings(String source) {
        StringBuilder sb = new StringBuilder(source.length());
        int i = 0;
        while (i < source.length()) {
            if (i + 1 < source.length() && source.charAt(i) == '/' && source.charAt(i + 1) == '*') {
                int end = source.indexOf("*/", i + 2);
                if (end < 0) break;
                i = end + 2;
                continue;
            }
            if (i + 1 < source.length() && source.charAt(i) == '/' && source.charAt(i + 1) == '/') {
                int end = source.indexOf('\n', i + 2);
                if (end < 0) break;
                i = end + 1;
                continue;
            }
            if (source.charAt(i) == '"') {
                sb.append(' ');
                i++;
                while (i < source.length() && source.charAt(i) != '"') {
                    if (source.charAt(i) == '\\') i++;
                    i++;
                }
                if (i < source.length()) i++;
                continue;
            }
            if (source.charAt(i) == '\'') {
                sb.append(' ');
                i++;
                while (i < source.length() && source.charAt(i) != '\'') {
                    if (source.charAt(i) == '\\') i++;
                    i++;
                }
                if (i < source.length()) i++;
                continue;
            }
            sb.append(source.charAt(i));
            i++;
        }
        return sb.toString();
    }
}
