package com.eyecode.editor.v2.project;

import com.eyecode.editor.v2.language.java.symbols.ProjectSymbol;
import com.eyecode.editor.v2.language.java.symbols.SemanticResolver;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ProjectSymbolIndex {

    private final List<ProjectSymbol> symbols = new ArrayList<>();
    private final Map<Path, List<ProjectSymbol>> symbolsByFile = new ConcurrentHashMap<>();

    public void clear() {
        symbols.clear();
        symbolsByFile.clear();
    }

    public void add(ProjectSymbol symbol) {
        if (symbol == null || symbol.getName() == null) return;
        symbols.add(symbol);
        symbolsByFile
                .computeIfAbsent(symbol.getSourceFile(), k -> new ArrayList<>())
                .add(symbol);
    }

    public void addAll(List<ProjectSymbol> fileSymbols) {
        if (fileSymbols == null || fileSymbols.isEmpty()) return;
        for (ProjectSymbol symbol : fileSymbols) {
            add(symbol);
        }
    }

    public void removeFile(Path file) {
        if (file == null) return;
        List<ProjectSymbol> removed = symbolsByFile.remove(file);
        if (removed == null) return;
        symbols.removeAll(removed);
    }

    public void replaceFile(Path file, List<ProjectSymbol> newSymbols) {
        removeFile(file);
        if (newSymbols == null) return;
        for (ProjectSymbol symbol : newSymbols) {
            add(symbol);
        }
    }

    public List<ProjectSymbol> getAll() {
        return Collections.unmodifiableList(symbols);
    }

    public List<ProjectSymbol> getForFile(Path file) {
        List<ProjectSymbol> fileSymbols = symbolsByFile.get(file);
        return fileSymbols == null
                ? List.of()
                : Collections.unmodifiableList(fileSymbols);
    }

    public SemanticResolver resolver() {
        return new SemanticResolver(symbols);
    }

    public int size() {
        return symbols.size();
    }
}
