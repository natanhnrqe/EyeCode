package com.eyecode.editor.v2.language.java.symbols;

import java.util.List;

public final class ScopeBuilder {

    public Scope build(List<ProjectSymbol> symbols) {
        Scope fileScope = new Scope(null, null);
        if (symbols == null || symbols.isEmpty()) {
            return fileScope;
        }

        for (ProjectSymbol symbol : symbols) {
            if (isType(symbol)) {
                Scope classScope = fileScope.addChild(symbol);
                classScope.addSymbol(symbol);
                addMembers(symbol, classScope, symbols);
            } else if (symbol.getOwner() == null) {
                fileScope.addSymbol(symbol);
            }
        }

        return fileScope;
    }

    private void addMembers(ProjectSymbol owner, Scope classScope, List<ProjectSymbol> symbols) {
        for (ProjectSymbol symbol : symbols) {
            if (!owner.getName().equals(symbol.getOwner())) {
                continue;
            }
            classScope.addSymbol(symbol);
            if (symbol.getKind() == SymbolKind.METHOD || symbol.getKind() == SymbolKind.CONSTRUCTOR) {
                addMethodScope(symbol, classScope, symbols);
            }
        }
    }

    private void addMethodScope(ProjectSymbol method, Scope classScope, List<ProjectSymbol> symbols) {
        Scope methodScope = classScope.addChild(method);
        methodScope.addSymbol(method);

        for (ProjectSymbol symbol : symbols) {
            if (method.getName().equals(symbol.getOwner())
                    && (symbol.getKind() == SymbolKind.PARAMETER || symbol.getKind() == SymbolKind.LOCAL_VARIABLE)) {
                methodScope.addSymbol(symbol);
            }
        }
    }

    private boolean isType(ProjectSymbol symbol) {
        return symbol.getKind() == SymbolKind.CLASS
                || symbol.getKind() == SymbolKind.INTERFACE
                || symbol.getKind() == SymbolKind.ENUM
                || symbol.getKind() == SymbolKind.RECORD;
    }
}
