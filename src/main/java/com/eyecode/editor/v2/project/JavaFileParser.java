package com.eyecode.editor.v2.project;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionItemKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JavaFileParser {

    private static final Pattern TYPE_PATTERN = Pattern.compile(
            "\\b(class|interface|enum|record)\\s+([A-Za-z_]\\w*)" +
            "(?:\\s*<[^>]+>)?(?:\\s+(?:extends|implements|permits)\\s+[^{]+)?"
    );

    private static final Set<String> MODIFIERS = Set.of(
            "public", "private", "protected", "static", "final", "transient",
            "volatile", "synchronized", "strictfp", "default", "abstract", "native"
    );

    private static final Set<String> PRIMITIVE_TYPES = Set.of(
            "int", "long", "double", "float", "boolean", "char", "byte", "short"
    );

    private static final Set<String> IGNORED_STATEMENTS = Set.of(
            "if", "for", "while", "switch", "return", "throw", "try", "catch",
            "break", "continue", "this", "super", "new", "assert", "synchronized",
            "else", "do", "case", "default", "import", "package", "void",
            "null", "true", "false", "instanceof", "var"
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

            int bodyStart = findBodyStart(cleaned, matcher.end());
            if (bodyStart >= 0) {
                parseClassFields(cleaned, bodyStart, name, items);
            }
        }

        return items;
    }

    private int findBodyStart(String text, int fromPos) {
        for (int i = fromPos; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') return i + 1;
            if (!Character.isWhitespace(c) && c != ',' && c != '<' && c != '>'
                    && !Character.isLetter(c) && c != '[' && c != ']' && c != '?'
                    && c != '&' && c != '|') {
                return -1;
            }
        }
        return -1;
    }

    private void parseClassFields(String cleaned, int bodyStart, String className, List<CompletionItem> items) {
        int i = bodyStart;
        int braceDepth = 1;

        while (i < cleaned.length() && braceDepth > 0) {
            char c = cleaned.charAt(i);

            if (c == '{') { braceDepth++; i++; continue; }
            if (c == '}') { braceDepth--; i++; continue; }
            if (braceDepth != 1) { i++; continue; }

            if (Character.isWhitespace(c)) { i++; continue; }

            if (c == '@') {
                i = skipAnnotation(cleaned, i);
                continue;
            }

            int stmtStart = i;
            int sigEnd = -1;
            int methodBodyStart = -1;
            boolean hasParen = false;

            while (i < cleaned.length()) {
                char sc = cleaned.charAt(i);

                if (sc == '(') {
                    hasParen = true;
                    i = skipParenBlock(cleaned, i);
                    sigEnd = i;
                    while (i < cleaned.length() && Character.isWhitespace(cleaned.charAt(i))) i++;
                    if (i < cleaned.length() && cleaned.charAt(i) == '{') {
                        methodBodyStart = i + 1;
                        i = skipBraceBlock(cleaned, i);
                    } else if (i < cleaned.length() && cleaned.charAt(i) == ';') {
                        i++;
                    }
                    break;
                }

                if (sc == ';') {
                    i++;
                    break;
                }

                if (sc == '{') {
                    i = skipBraceBlock(cleaned, i);
                    continue;
                }

                i++;
            }

            if (!hasParen) {
                String decl = cleaned.substring(stmtStart, i).trim();
                parseFieldDeclaration(decl, className, items);
            } else if (sigEnd > 0) {
                String sig = cleaned.substring(stmtStart, sigEnd).trim();
                String methodName = parseMethodSignature(sig, className, items);
                if (methodName != null && methodBodyStart > 0) {
                    parseLocalVariablesInBlock(cleaned, methodBodyStart, methodName, items);
                }
            }
        }
    }

    private void parseFieldDeclaration(String decl, String className, List<CompletionItem> items) {
        if (decl.isEmpty() || !decl.endsWith(";")) return;
        decl = decl.substring(0, decl.length() - 1).trim();
        if (decl.isEmpty()) return;

        String[] parts = splitTopLevelCommas(decl);
        if (parts.length == 0) return;

        String typeAndName = parts[0].trim();
        String fieldType = extractType(typeAndName);

        for (String part : parts) {
            String trimmed = part.trim();
            String name = extractName(trimmed);
            if (name == null || name.isEmpty()) continue;

            items.add(CompletionItem.builder(name, name, CompletionItemKind.FIELD)
                    .returnType(fieldType)
                    .owner(className)
                    .category("Project Field")
                    .priority(35)
                    .build());
        }
    }

    private String parseMethodSignature(String sig, String className, List<CompletionItem> items) {
        if (!sig.startsWith("public")) return null;

        String stripped = sig.substring("public".length()).trim();
        while (true) {
            boolean found = false;
            for (String mod : MODIFIERS) {
                if (stripped.startsWith(mod + " ") || stripped.startsWith(mod + "\t")) {
                    stripped = stripped.substring(mod.length()).trim();
                    found = true;
                    break;
                }
            }
            if (!found) break;
        }

        int parenIdx = stripped.indexOf('(');
        if (parenIdx < 0) return null;

        String beforeParen = stripped.substring(0, parenIdx).trim();
        if (beforeParen.isEmpty()) return null;

        String[] tokens = beforeParen.split("\\s+");
        if (tokens.length == 0) return null;

        String methodName = tokens[tokens.length - 1];
        if (methodName.equals(className)) return null;

        String returnType = tokens.length > 1
                ? beforeParen.substring(0, beforeParen.length() - methodName.length()).trim()
                : "";

        items.add(CompletionItem.builder(methodName, methodName + "()", CompletionItemKind.METHOD)
                .signature("(...)")
                .returnType(returnType)
                .owner(className)
                .category("Project Method")
                .priority(40)
                .build());

        return methodName;
    }

    private void parseLocalVariablesInBlock(String cleaned, int bodyStart, String methodName, List<CompletionItem> items) {
        int i = bodyStart;
        int braceDepth = 1;

        while (i < cleaned.length() && braceDepth > 0) {
            char c = cleaned.charAt(i);

            if (c == '{') { braceDepth++; i++; continue; }
            if (c == '}') { braceDepth--; i++; continue; }
            if (braceDepth != 1) { i++; continue; }

            if (Character.isWhitespace(c)) { i++; continue; }

            int stmtStart = i;
            boolean hasParen = false;

            while (i < cleaned.length()) {
                char sc = cleaned.charAt(i);

                if (sc == '(') {
                    hasParen = true;
                    i = skipParenBlock(cleaned, i);
                    break;
                }

                if (sc == ';') {
                    i++;
                    break;
                }

                if (sc == '{') {
                    i = skipBraceBlock(cleaned, i);
                    continue;
                }

                i++;
            }

            if (!hasParen) {
                String decl = cleaned.substring(stmtStart, i).trim();
                parseLocalVariable(decl, methodName, items);
            }
        }
    }

    private void parseLocalVariable(String decl, String methodName, List<CompletionItem> items) {
        if (decl.isEmpty() || !decl.endsWith(";")) return;
        decl = decl.substring(0, decl.length() - 1).trim();
        if (decl.isEmpty()) return;

        String[] parts = splitTopLevelCommas(decl);
        if (parts.length == 0) return;

        String varType = extractLocalVarType(parts[0]);
        if (varType == null) return;

        for (String part : parts) {
            String trimmed = part.trim();
            String name = extractLocalVarName(trimmed);
            if (name == null || name.isEmpty()) continue;

            items.add(CompletionItem.builder(name, name, CompletionItemKind.VARIABLE)
                    .returnType(varType)
                    .owner(methodName)
                    .category("Local Variable")
                    .priority(45)
                    .build());
        }
    }

    private String extractLocalVarType(String part) {
        String stripped = part.trim();
        if (stripped.startsWith("final ")) stripped = stripped.substring(6).trim();

        int end = findNameEnd(stripped);
        if (end < 0) return null;

        String beforeEq = stripped.substring(0, end).trim();
        String[] tokens = beforeEq.split("\\s+");
        if (tokens.length < 2) return null;

        String first = tokens[0];
        if (IGNORED_STATEMENTS.contains(first)) return null;
        if (!PRIMITIVE_TYPES.contains(first) && Character.isLowerCase(first.charAt(0))) return null;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.length - 1; i++) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(tokens[i]);
        }
        String result = sb.toString().trim();
        return result.isEmpty() ? null : result;
    }

    private String extractLocalVarName(String part) {
        String stripped = part.trim();
        int end = findNameEnd(stripped);
        if (end < 0) return null;

        String beforeEq = stripped.substring(0, end).trim();
        String[] tokens = beforeEq.split("\\s+");
        if (tokens.length < 2) return null;

        String name = tokens[tokens.length - 1];
        if (name.isEmpty() || !Character.isJavaIdentifierStart(name.charAt(0))) return null;
        return name;
    }

    private int findNameEnd(String text) {
        int depth = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '<') depth++;
            else if (c == '>') depth--;
            else if (depth == 0 && (c == '=' || c == ',')) return i;
        }
        return text.length();
    }

    private String extractType(String decl) {
        String[] tokens = decl.split("\\s+");
        int modifierEnd = 0;
        while (modifierEnd < tokens.length && MODIFIERS.contains(tokens[modifierEnd])) {
            modifierEnd++;
        }
        if (modifierEnd >= tokens.length) return "";
        if (modifierEnd == tokens.length - 1) return tokens[modifierEnd];
        StringBuilder type = new StringBuilder();
        for (int i = modifierEnd; i < tokens.length - 1; i++) {
            if (!type.isEmpty()) type.append(' ');
            type.append(tokens[i]);
        }
        return type.toString();
    }

    private String extractName(String decl) {
        String[] tokens = decl.split("\\s+");
        if (tokens.length == 0) return null;

        int modifierEnd = 0;
        while (modifierEnd < tokens.length && MODIFIERS.contains(tokens[modifierEnd])) {
            modifierEnd++;
        }

        if (modifierEnd >= tokens.length) return null;
        String candidate = tokens[tokens.length - 1];
        int eqIdx = candidate.indexOf('=');
        if (eqIdx >= 0) candidate = candidate.substring(0, eqIdx);
        int bracketIdx = candidate.indexOf('[');
        if (bracketIdx >= 0) candidate = candidate.substring(0, bracketIdx);
        return candidate.trim();
    }

    private String[] splitTopLevelCommas(String text) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '<') depth++;
            else if (c == '>') depth--;
            else if (c == ',' && depth == 0) {
                parts.add(text.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(text.substring(start));
        return parts.toArray(new String[0]);
    }

    private int skipAnnotation(String text, int pos) {
        if (text.charAt(pos) != '@') return pos + 1;
        pos++;
        while (pos < text.length() && (Character.isJavaIdentifierPart(text.charAt(pos)) || text.charAt(pos) == '.')) {
            pos++;
        }
        if (pos < text.length() && text.charAt(pos) == '(') {
            pos = skipParenBlock(text, pos);
        }
        return pos;
    }

    private int skipParenBlock(String text, int pos) {
        if (pos >= text.length() || text.charAt(pos) != '(') return pos + 1;
        int depth = 1;
        pos++;
        while (pos < text.length() && depth > 0) {
            char c = text.charAt(pos);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            pos++;
        }
        return pos;
    }

    private int skipBraceBlock(String text, int pos) {
        if (pos >= text.length() || text.charAt(pos) != '{') return pos + 1;
        int depth = 1;
        pos++;
        while (pos < text.length() && depth > 0) {
            char c = text.charAt(pos);
            if (c == '{') depth++;
            else if (c == '}') depth--;
            pos++;
        }
        return pos;
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
