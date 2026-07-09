package com.eyecode.editor.v2.project;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionItemKind;
import com.eyecode.editor.v2.language.java.lexer.JavaLexer;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.model.JavaClassModel;
import com.eyecode.editor.v2.language.java.model.JavaConstructorModel;
import com.eyecode.editor.v2.language.java.model.JavaFieldModel;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.editor.v2.language.java.model.JavaMethodModel;
import com.eyecode.editor.v2.language.java.model.JavaParameterModel;
import com.eyecode.editor.v2.language.java.model.JavaVariableModel;
import com.eyecode.editor.v2.language.java.model.TypeKind;
import com.eyecode.editor.v2.language.java.parser.JavaParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class ProjectIndexer {

    private final Path projectRoot;

    public ProjectIndexer(Path projectRoot) {
        this.projectRoot = projectRoot;
    }

    public void index(ProjectSymbolIndex index) {
        if (projectRoot == null || !Files.isDirectory(projectRoot)) return;

        index.clear();

        indexSourceDir(projectRoot.resolve("src/main/java"), index);
        indexSourceDir(projectRoot.resolve("src/test/java"), index);
    }

    private void indexSourceDir(Path dir, ProjectSymbolIndex index) {
        if (!Files.isDirectory(dir)) return;

        try (Stream<Path> walk = Files.walk(dir)) {
            walk.filter(p -> p.toString().endsWith(".java"))
                .forEach(file -> indexFile(file, index));
        } catch (IOException ignored) {
        }
    }

    private void indexFile(Path file, ProjectSymbolIndex index) {
        try {
            String source = Files.readString(file);
            JavaLexer lexer = new JavaLexer();
            JavaTokenStream stream = new JavaTokenStream(lexer.tokenize(source));
            JavaParser parser = new JavaParser(stream);
            JavaFileModel model = parser.parse();

            for (CompletionItem item : buildCompletionItems(model)) {
                index.add(item);
            }
        } catch (Exception ignored) {
        }
    }

    private List<CompletionItem> buildCompletionItems(JavaFileModel model) {
        List<CompletionItem> items = new ArrayList<>();
        for (JavaClassModel type : model.getTypes()) {
            indexType(type, items);
        }
        return items;
    }

    private void indexType(JavaClassModel type, List<CompletionItem> items) {
        items.add(CompletionItem.builder(type.getName(), type.getName(), toKind(type.getKind()))
                .detail(type.getName())
                .category(type.getKind().name().charAt(0) + type.getKind().name().substring(1).toLowerCase())
                .build());

        for (JavaFieldModel field : type.getFields()) {
            items.add(CompletionItem.builder(field.getName(), field.getName(), CompletionItemKind.FIELD)
                    .returnType(field.getType())
                    .owner(type.getName())
                    .category("Project Field")
                    .priority(35)
                    .build());
        }

        for (JavaConstructorModel ctor : type.getConstructors()) {
            items.add(CompletionItem.builder(ctor.getName(), ctor.getName() + "()", CompletionItemKind.CONSTRUCTOR)
                    .signature("(...)")
                    .owner(type.getName())
                    .category("Constructor")
                    .priority(40)
                    .build());
        }

        for (JavaMethodModel method : type.getMethods()) {
            items.add(CompletionItem.builder(method.getName(), method.getName() + "()", CompletionItemKind.METHOD)
                    .signature("(...)")
                    .returnType(method.getReturnType())
                    .owner(type.getName())
                    .category("Project Method")
                    .priority(40)
                    .build());

            for (JavaParameterModel param : method.getParameters()) {
                items.add(CompletionItem.builder(param.getName(), param.getName(), CompletionItemKind.VARIABLE)
                        .returnType(param.getType())
                        .owner(method.getName())
                        .category("Parameter")
                        .priority(45)
                        .build());
            }

            for (JavaVariableModel var : method.getLocalVariables()) {
                items.add(CompletionItem.builder(var.getName(), var.getName(), CompletionItemKind.VARIABLE)
                        .returnType(var.getType())
                        .owner(method.getName())
                        .category("Local Variable")
                        .priority(45)
                        .build());
            }
        }

        for (JavaClassModel nested : type.getNestedTypes()) {
            indexType(nested, items);
        }
    }

    private CompletionItemKind toKind(TypeKind kind) {
        return switch (kind) {
            case CLASS -> CompletionItemKind.CLASS;
            case INTERFACE -> CompletionItemKind.INTERFACE;
            case ENUM -> CompletionItemKind.ENUM;
            case RECORD -> CompletionItemKind.RECORD;
        };
    }
}
