package com.eyecode.editor.v2.completion.semantic;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionItemKind;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JavaSourceParser {

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^\\s*package\\s+([a-zA-Z_]\\w*(?:\\.[a-zA-Z_]\\w*)*)\\s*;");
    private static final Pattern TYPE_PATTERN = Pattern.compile("^\\s*(?:public\\s+|private\\s+|protected\\s+)?(?:static\\s+)?(?:final\\s+)?(?:abstract\\s+)?(?:sealed\\s+)?(?:non-sealed\\s+)?(class|interface|enum|record)\\s+([A-Za-z_]\\w*).*\\{?\\s*$");
    private static final Pattern METHOD_PATTERN = Pattern.compile("^\\s*(?:public\\s+|private\\s+|protected\\s+)?(?:static\\s+)?(?:final\\s+)?(?:abstract\\s+)?(?:synchronized\\s+)?(?:<[^>]+>\\s*)?([\\w.?,[\\]]+)\\s+([a-z_]\\w*)\\s*\\(([^)]*)\\)");
    private static final Pattern FIELD_OR_VAR_PATTERN = Pattern.compile("^\\s*(?:public\\s+|private\\s+|protected\\s+)?(?:static\\s+)?(?:final\\s+)?(?:volatile\\s+)?(?:transient\\s+)?([\\w.?,[\\]]+)\\s+([a-z_]\\w*)\\s*(?:=|;|,)");
    private static final Pattern FOR_VAR_PATTERN = Pattern.compile("for\\s*\\(\\s*([\\w.?]+)\\s+([a-z_]\\w*)\\s*:");
    private static final Pattern CATCH_VAR_PATTERN = Pattern.compile("catch\\s*\\(\\s*([\\w.?]+)\\s+([a-z_]\\w*)\\s*\\)");
    private static final Pattern TRY_RESOURCE_PATTERN = Pattern.compile("try\\s*\\(\\s*([\\w.?]+)\\s+([a-z_]\\w*)");

    private final List<CompletionItem> symbols = new ArrayList<>();
    private final List<ParsedType> types = new ArrayList<>();
    private final List<ParsedVariable> locals = new ArrayList<>();
    private final List<ParsedMethod> methods = new ArrayList<>();
    private final List<ParsedField> fields = new ArrayList<>();

    public record ParsedType(String name, CompletionItemKind kind, String packageName) {}
    public record ParsedMethod(String name, String returnType, String owner, List<String> paramTypes, List<String> paramNames) {}
    public record ParsedField(String name, String type, String owner) {}
    public record ParsedVariable(String name, String type, String scopeContext) {}

    public JavaSourceParser(String source) {
        parse(source);
    }

    public List<CompletionItem> getSymbols() { return List.copyOf(symbols); }
    public List<ParsedType> getTypes() { return List.copyOf(types); }
    public List<ParsedVariable> getLocals() { return List.copyOf(locals); }
    public List<ParsedMethod> getMethods() { return List.copyOf(methods); }
    public List<ParsedField> getFields() { return List.copyOf(fields); }

    private void parse(String source) {
        if (source == null || source.isBlank()) return;

        String pkg = "";
        List<String> typeStack = new ArrayList<>();
        int[] braceDepth = {0};
        boolean[] inMethodBody = {false};
        String[] currentMethodName = {null};
        String[] currentMethodReturnType = {null};

        String stripped = stripBlockComments(source);
        String[] lines = stripped.split("\n", -1);

        for (int lineIdx = 0; lineIdx < lines.length; lineIdx++) {
            String raw = lines[lineIdx];
            int lineCommentIdx = raw.indexOf("//");
            String line = (lineCommentIdx >= 0 ? raw.substring(0, lineCommentIdx) : raw).trim();

            if (line.isEmpty()) continue;

            // Track brace depth
            for (int i = 0; i < raw.length(); i++) {
                char c = raw.charAt(i);
                if (c == '{') {
                    braceDepth[0]++;
                    // If we just entered a method body (depth 2+ and not a type body)
                    if (braceDepth[0] >= 2 && !typeStack.isEmpty()) {
                        inMethodBody[0] = true;
                    }
                } else if (c == '}') {
                    braceDepth[0]--;
                    if (braceDepth[0] <= 1) {
                        inMethodBody[0] = false;
                        currentMethodName[0] = null;
                        currentMethodReturnType[0] = null;
                    }
                    if (braceDepth[0] < 0) braceDepth[0] = 0;
                }
            }

            // Package
            Matcher pm = PACKAGE_PATTERN.matcher(line);
            if (pm.matches()) {
                pkg = pm.group(1);
                continue;
            }

            // Type declaration (only at depth 0 or 1)
            if (braceDepth[0] <= 1) {
                Matcher tm = TYPE_PATTERN.matcher(line);
                if (tm.matches()) {
                    String kind = tm.group(1);
                    String name = tm.group(2);
                    CompletionItemKind itemKind = switch (kind) {
                        case "interface" -> CompletionItemKind.INTERFACE;
                        case "enum" -> CompletionItemKind.ENUM;
                        case "record" -> CompletionItemKind.RECORD;
                        default -> CompletionItemKind.CLASS;
                    };

                    if (braceDepth[0] == 0) {
                        typeStack.clear();
                    }
                    typeStack.add(name);

                    String detail = pkg.isEmpty() ? name : pkg + "." + name;
                    String owner = pkg.isEmpty() ? "(default package)" : pkg;

                    types.add(new ParsedType(name, itemKind, pkg));
                    symbols.add(CompletionItem.builder(name, name, itemKind)
                            .detail(detail)
                            .owner(owner)
                            .category(kind.substring(0, 1).toUpperCase() + kind.substring(1))
                            .documentation("User-defined " + kind + " declared in " + detail)
                            .build());

                    // Reset method tracking since we're now in a type
                    inMethodBody[0] = false;
                    currentMethodName[0] = null;
                    continue;
                }
            }

            // Method declaration (inside a type body, depth >= 1)
            Matcher mm = METHOD_PATTERN.matcher(line);
            if (mm.matches() && !typeStack.isEmpty() && braceDepth[0] >= 1) {
                String returnType = mm.group(1).trim();
                String methodName = mm.group(2);
                String paramsStr = mm.group(3).trim();
                String owner = typeStack.get(typeStack.size() - 1);

                List<String> paramTypes = new ArrayList<>();
                List<String> paramNames = new ArrayList<>();
                if (!paramsStr.isEmpty()) {
                    String[] params = paramsStr.split(",");
                    for (String param : params) {
                        param = param.trim();
                        if (param.isEmpty()) continue;
                        String[] parts = param.split("\\s+");
                        if (parts.length >= 2) {
                            paramTypes.add(parts[0]);
                            paramNames.add(parts[parts.length - 1].replace("...", ""));
                        }
                    }
                }

                methods.add(new ParsedMethod(methodName, returnType, owner, paramTypes, paramNames));

                // Register variables for each parameter
                for (int i = 0; i < paramNames.size(); i++) {
                    String paramName = paramNames.get(i);
                    locals.add(new ParsedVariable(paramName, paramTypes.get(i), "param:" + owner + "." + methodName));
                    symbols.add(CompletionItem.builder(paramName, paramName, CompletionItemKind.VARIABLE)
                            .detail("parameter of " + owner + "." + methodName)
                            .signature(owner + "." + methodName + "(" + paramsStr + ")")
                            .returnType(paramTypes.get(i))
                            .owner(owner)
                            .category("Parameter")
                            .documentation("Parameter " + paramName + ": " + paramTypes.get(i))
                            .build());
                }

                // Register the method itself
                String detail = owner + "." + methodName;
                symbols.add(CompletionItem.builder(methodName, methodName, CompletionItemKind.METHOD)
                        .detail(detail)
                        .signature(owner + "." + methodName + "(" + paramsStr + ")")
                        .returnType(returnType)
                        .owner(owner)
                        .category("Method")
                        .documentation("Method " + methodName + " in " + owner)
                        .build());

                currentMethodName[0] = methodName;
                currentMethodReturnType[0] = returnType;
                continue;
            }

            // Field declaration (inside a type, depth >= 1)
            Matcher fm = FIELD_OR_VAR_PATTERN.matcher(line);
            if (fm.matches() && !typeStack.isEmpty() && braceDepth[0] >= 1 && !inMethodBody[0]) {
                String type = fm.group(1).trim();
                String fieldName = fm.group(2);
                String owner = typeStack.get(typeStack.size() - 1);

                fields.add(new ParsedField(fieldName, type, owner));
                symbols.add(CompletionItem.builder(fieldName, fieldName, CompletionItemKind.FIELD)
                        .detail(owner + "." + fieldName)
                        .signature(owner + "." + fieldName)
                        .returnType(type)
                        .owner(owner)
                        .category("Field")
                        .documentation("Field " + fieldName + ": " + type + " in " + owner)
                        .build());
                continue;
            }

            // Local variable declaration (inside method body)
            if (inMethodBody[0]) {
                // Try local var pattern
                Matcher lvm = FIELD_OR_VAR_PATTERN.matcher(line);
                if (lvm.matches() && !typeStack.isEmpty()) {
                    String type = lvm.group(1).trim();
                    String varName = lvm.group(2);
                    if (isLocalVarType(type) && varName.length() > 0) {
                        String context = typeStack.get(typeStack.size() - 1);
                        if (currentMethodName[0] != null) {
                            context = context + "." + currentMethodName[0];
                        }
                        locals.add(new ParsedVariable(varName, type, context));
                        symbols.add(CompletionItem.builder(varName, varName, CompletionItemKind.VARIABLE)
                                .detail(varName + ": " + type)
                                .returnType(type)
                                .owner(context)
                                .category("Variable")
                                .documentation("Local variable " + varName + ": " + type)
                                .build());
                    }
                }

                // For-each variable
                Matcher fvm = FOR_VAR_PATTERN.matcher(line);
                if (fvm.find()) {
                    String type = fvm.group(1).trim();
                    String varName = fvm.group(2);
                    locals.add(new ParsedVariable(varName, type, "foreach"));
                    symbols.add(CompletionItem.builder(varName, varName, CompletionItemKind.VARIABLE)
                            .detail(varName + ": " + type)
                            .returnType(type)
                            .category("Variable")
                            .documentation("Loop variable " + varName + ": " + type)
                            .build());
                }

                // Catch variable
                Matcher cvm = CATCH_VAR_PATTERN.matcher(line);
                if (cvm.find()) {
                    String type = cvm.group(1).trim();
                    String varName = cvm.group(2);
                    locals.add(new ParsedVariable(varName, type, "catch"));
                    symbols.add(CompletionItem.builder(varName, varName, CompletionItemKind.VARIABLE)
                            .detail(varName + ": " + type)
                            .returnType(type)
                            .category("Variable")
                            .documentation("Exception variable " + varName + ": " + type)
                            .build());
                }

                // Try-with-resources variable
                Matcher trm = TRY_RESOURCE_PATTERN.matcher(line);
                if (trm.find()) {
                    String type = trm.group(1).trim();
                    String varName = trm.group(2);
                    locals.add(new ParsedVariable(varName, type, "try-resource"));
                    symbols.add(CompletionItem.builder(varName, varName, CompletionItemKind.VARIABLE)
                            .detail(varName + ": " + type)
                            .returnType(type)
                            .category("Variable")
                            .documentation("Try-with-resources variable " + varName + ": " + type)
                            .build());
                }
            }
        }
    }

    private static boolean isLocalVarType(String type) {
        if (type == null || type.isEmpty()) return false;
        String t = type.trim();
        if (t.equals("import") || t.equals("package") || t.equals("return")) return false;
        if (t.equals("if") || t.equals("while") || t.equals("for")) return false;
        if (t.equals("catch") || t.equals("try") || t.equals("switch")) return false;
        if (t.startsWith("@")) return false;
        if (t.startsWith("//") || t.startsWith("/*")) return false;
        return !t.isEmpty() && Character.isJavaIdentifierStart(t.charAt(0));
    }

    private static String stripBlockComments(String source) {
        StringBuilder sb = new StringBuilder(source.length());
        int i = 0;
        while (i < source.length()) {
            if (i + 1 < source.length() && source.charAt(i) == '/' && source.charAt(i + 1) == '*') {
                int end = source.indexOf("*/", i + 2);
                if (end < 0) break;
                // Replace with newlines to preserve line numbering
                for (int j = i; j <= end + 1; j++) {
                    if (source.charAt(j) == '\n') {
                        sb.append('\n');
                    }
                }
                i = end + 2;
            } else {
                sb.append(source.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }
}
