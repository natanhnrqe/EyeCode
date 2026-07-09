package com.eyecode.editor.v2.project;

import com.eyecode.editor.v2.language.java.symbols.ProjectSymbol;
import com.eyecode.editor.v2.language.java.symbols.SemanticResolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ProjectSymbolIndex {

    private final List<ProjectSymbol> symbols = new ArrayList<>();

    public void clear() {
        symbols.clear();
    }

    public void add(ProjectSymbol symbol) {
        if (symbol != null && symbol.getName() != null) {
            symbols.add(symbol);
        }
    }

    public List<ProjectSymbol> getAll() {
        return Collections.unmodifiableList(symbols);
    }

    public SemanticResolver resolver() {
        return new SemanticResolver(symbols);
    }

    public int size() {
        return symbols.size();
    }
}
