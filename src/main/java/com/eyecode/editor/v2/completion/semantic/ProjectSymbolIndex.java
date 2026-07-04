package com.eyecode.editor.v2.completion.semantic;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionItemKind;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class ProjectSymbolIndex {

    private final Map<String, CompletionItem> symbols = new LinkedHashMap<>();
    private final Map<String, String> variableTypes = new LinkedHashMap<>();
    private final Map<String, List<CompletionItem>> membersByType = new LinkedHashMap<>();
    private Path projectRoot;
    private boolean indexed;

    public ProjectSymbolIndex() {
        this(null);
    }

    public ProjectSymbolIndex(Path projectRoot) {
        this.projectRoot = projectRoot;
    }

    public void setProjectRoot(Path root) {
        if (root != null && !root.equals(this.projectRoot)) {
            this.projectRoot = root;
            this.indexed = false;
        }
    }

    public Path getProjectRoot() {
        return projectRoot;
    }

    public void reindex() {
        symbols.clear();
        variableTypes.clear();
        membersByType.clear();
        indexed = true;

        if (projectRoot == null || !Files.isDirectory(projectRoot)) return;

        List<Path> javaFiles = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(projectRoot)) {
            walk.filter(p -> p.toString().endsWith(".java"))
                 .filter(p -> {
                     String abs = p.toAbsolutePath().toString().replace('\\', '/');
                     return abs.contains("/src/main/java/") || abs.contains("/src/test/java/")
                             || abs.contains("\\src\\main\\java\\") || abs.contains("\\src\\test\\java\\");
                 })
                 .forEach(javaFiles::add);
        } catch (IOException ignored) {
        }

        if (javaFiles.isEmpty()) {
            try (Stream<Path> walk = Files.walk(projectRoot)) {
                walk.filter(p -> p.toString().endsWith(".java"))
                     .limit(200)
                     .forEach(javaFiles::add);
            } catch (IOException ignored) {
            }
        }

        for (Path file : javaFiles) {
            try {
                String source = Files.readString(file);
                JavaSourceParser parser = new JavaSourceParser(source);
                for (CompletionItem item : parser.getSymbols()) {
                    String key = item.getLabel() + "\u0000" + item.getKind();
                    symbols.putIfAbsent(key, item);
                }
                for (JavaSourceParser.ParsedVariable var : parser.getLocals()) {
                    variableTypes.putIfAbsent(var.name(), var.type());
                }
                for (JavaSourceParser.ParsedField field : parser.getFields()) {
                    variableTypes.putIfAbsent(field.name(), field.type());
                    membersByType.computeIfAbsent(field.owner(), k -> new ArrayList<>()).add(
                            CompletionItem.builder(field.name(), field.name(), CompletionItemKind.FIELD)
                                    .detail(field.owner() + "." + field.name())
                                    .signature(field.owner() + "." + field.name())
                                    .returnType(field.type())
                                    .owner(field.owner())
                                    .category("Field")
                                    .documentation("Field " + field.name() + ": " + field.type())
                                    .build());
                }
                for (JavaSourceParser.ParsedMethod method : parser.getMethods()) {
                    membersByType.computeIfAbsent(method.owner(), k -> new ArrayList<>()).add(
                            CompletionItem.builder(method.name(), method.name(), CompletionItemKind.METHOD)
                                    .detail(method.owner() + "." + method.name())
                                    .signature(method.owner() + "." + method.name() + "(" + String.join(", ", method.paramTypes()) + ")")
                                    .returnType(method.returnType())
                                    .owner(method.owner())
                                    .category("Method")
                                    .documentation("Method " + method.name() + " in " + method.owner())
                                    .build());
                }
            } catch (Exception ignored) {
            }
        }
    }

    public List<CompletionItem> findByPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) return Collections.emptyList();
        if (!indexed) reindex();

        List<CompletionItem> result = new ArrayList<>();
        for (CompletionItem item : symbols.values()) {
            if (item.getLabel().startsWith(prefix)) {
                result.add(item);
            }
        }
        return result;
    }

    public List<CompletionItem> getMembers(String typeName) {
        if (!indexed) reindex();
        return membersByType.getOrDefault(typeName, Collections.emptyList());
    }

    public String resolveVariableType(String variableName) {
        if (!indexed) reindex();
        return variableTypes.get(variableName);
    }

    public List<CompletionItem> getAll() {
        if (!indexed) reindex();
        return List.copyOf(symbols.values());
    }

    public int size() {
        if (!indexed) reindex();
        return symbols.size();
    }

    public static Path findProjectRoot(Path sourceFile) {
        if (sourceFile == null) return null;
        Path current = sourceFile.getParent();
        if (current == null) return null;
        while (current != null) {
            if (Files.exists(current.resolve("pom.xml"))
                    || Files.exists(current.resolve("build.gradle"))
                    || Files.exists(current.resolve(".git"))
                    || Files.exists(current.resolve("settings.gradle"))
                    || Files.exists(current.resolve("settings.gradle.kts"))) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }
}
