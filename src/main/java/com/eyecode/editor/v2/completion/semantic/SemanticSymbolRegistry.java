package com.eyecode.editor.v2.completion.semantic;

import com.eyecode.editor.v2.completion.CompletionItemKind;

import java.util.ArrayList;
import java.util.List;

public final class SemanticSymbolRegistry {

    private final List<SemanticSymbol> symbols = new ArrayList<>();

    public SemanticSymbolRegistry() {
        registerClasses();
        registerMethods();
    }

    public List<SemanticSymbol> getSymbols() {
        return List.copyOf(symbols);
    }

    public void register(SemanticSymbol symbol) {
        if (symbol != null) {
            symbols.add(symbol);
        }
    }

    private void registerClasses() {
        registerClass("String");
        registerClass("Object");
        registerClass("System");
        registerClass("Math");
        registerClass("Integer");
        registerClass("Long");
        registerClass("Double");
        registerClass("Boolean");
        registerClass("ArrayList");
        registerClass("LinkedList");
        registerClass("HashMap");
        registerClass("HashSet");
        registerClass("List");
        registerClass("Map");
        registerClass("Set");
    }

    private void registerMethods() {
        registerMethod("print");
        registerMethod("println");
        registerMethod("printf");
        registerMethod("length");
        registerMethod("substring");
        registerMethod("charAt");
        registerMethod("add");
        registerMethod("remove");
        registerMethod("clear");
        registerMethod("contains");
        registerMethod("put");
        registerMethod("get");
        registerMethod("keySet");
        registerMethod("values");
    }

    private void registerClass(String name) {
        register(new SemanticSymbol(name, CompletionItemKind.CLASS, "Java class"));
    }

    private void registerMethod(String name) {
        register(new SemanticSymbol(name, CompletionItemKind.METHOD, "Java method"));
    }
}
