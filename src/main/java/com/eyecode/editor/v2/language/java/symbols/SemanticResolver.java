package com.eyecode.editor.v2.language.java.symbols;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SemanticResolver {

    private final List<ProjectSymbol> symbols;
    private final Map<String, ProjectSymbol> globalsByName;
    private final Map<String, List<ProjectSymbol>> membersByOwner;

    public SemanticResolver(List<ProjectSymbol> symbols) {
        this.symbols = symbols == null ? List.of() : List.copyOf(symbols);
        this.globalsByName = new LinkedHashMap<>();
        this.membersByOwner = new LinkedHashMap<>();
        indexSymbols();
    }

    public ProjectSymbol resolveClass(String name) {
        ProjectSymbol symbol = resolveByName(name);
        if (symbol == null) return null;
        return isType(symbol) ? symbol : null;
    }

    public ProjectSymbol resolveMethod(String owner, String name) {
        return resolveMember(owner, name, SymbolKind.METHOD);
    }

    public ProjectSymbol resolveField(String owner, String name) {
        return resolveMember(owner, name, SymbolKind.FIELD);
    }

    public ProjectSymbol resolveVariable(String name) {
        for (ProjectSymbol symbol : symbols) {
            if ((symbol.getKind() == SymbolKind.LOCAL_VARIABLE || symbol.getKind() == SymbolKind.PARAMETER)
                    && symbol.getName().equals(name)) {
                return symbol;
            }
        }
        return null;
    }

    public ProjectSymbol resolveType(ProjectSymbol symbol) {
        if (symbol == null || symbol.getType() == null || symbol.getType().isBlank()) {
            return null;
        }
        return resolveClass(stripGenericType(symbol.getType()));
    }

    public List<ProjectSymbol> resolveMembers(ProjectSymbol symbol) {
        if (symbol == null) {
            return Collections.emptyList();
        }

        ProjectSymbol type = isType(symbol) ? symbol : resolveType(symbol);
        if (type == null) {
            return Collections.emptyList();
        }

        return membersByOwner.getOrDefault(type.getName(), Collections.emptyList());
    }

    public ProjectSymbol resolveByName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return globalsByName.get(name);
    }

    public List<ProjectSymbol> resolveVisibleSymbols() {
        return symbols;
    }

    public ProjectSymbol resolve(String name, Scope scope) {
        if (name == null || name.isBlank()) {
            return null;
        }

        Scope current = scope;
        while (current != null) {
            ProjectSymbol symbol = findInScope(name, current);
            if (symbol != null) {
                return symbol;
            }
            current = current.getParent();
        }

        return resolveByName(name);
    }

    public List<ProjectSymbol> resolveVisibleSymbols(Scope scope) {
        if (scope == null) {
            return resolveVisibleSymbols();
        }

        Map<String, ProjectSymbol> visible = new LinkedHashMap<>();
        Scope current = scope;
        while (current != null) {
            for (ProjectSymbol symbol : current.getSymbols()) {
                visible.putIfAbsent(symbol.getName(), symbol);
            }

            ProjectSymbol owner = current.getOwner();
            if (owner != null && isType(owner)) {
                addInheritedMembers(owner, visible);
            }
            current = current.getParent();
        }

        for (ProjectSymbol symbol : symbols) {
            if (isGlobal(symbol)) {
                visible.putIfAbsent(symbol.getName(), symbol);
            }
        }

        return List.copyOf(visible.values());
    }

    private void indexSymbols() {
        for (ProjectSymbol symbol : symbols) {
            if (symbol.getName() != null && isGlobal(symbol)) {
                globalsByName.putIfAbsent(symbol.getName(), symbol);
            }
            if (symbol.getOwner() != null) {
                membersByOwner.computeIfAbsent(symbol.getOwner(), key -> new ArrayList<>()).add(symbol);
            }
        }
    }

    private ProjectSymbol resolveMember(String owner, String name, SymbolKind kind) {
        if (owner == null || name == null) {
            return null;
        }
        for (ProjectSymbol symbol : membersByOwner.getOrDefault(owner, Collections.emptyList())) {
            if (symbol.getKind() == kind && symbol.getName().equals(name)) {
                return symbol;
            }
        }
        return null;
    }

    private ProjectSymbol findInScope(String name, Scope scope) {
        for (ProjectSymbol symbol : scope.getSymbols()) {
            if (symbol.getName().equals(name)) {
                return symbol;
            }
        }
        return null;
    }

    private void addInheritedMembers(ProjectSymbol owner, Map<String, ProjectSymbol> visible) {
        ProjectSymbol superType = resolveType(owner);
        if (superType == null) {
            return;
        }
        for (ProjectSymbol member : resolveMembers(superType)) {
            visible.putIfAbsent(member.getName(), member);
        }
    }

    private boolean isGlobal(ProjectSymbol symbol) {
        return isType(symbol) || symbol.getOwner() == null;
    }

    private boolean isType(ProjectSymbol symbol) {
        return symbol.getKind() == SymbolKind.CLASS
                || symbol.getKind() == SymbolKind.INTERFACE
                || symbol.getKind() == SymbolKind.ENUM
                || symbol.getKind() == SymbolKind.RECORD;
    }

    private String stripGenericType(String type) {
        int genericStart = type.indexOf('<');
        if (genericStart >= 0) {
            return type.substring(0, genericStart);
        }
        int arrayStart = type.indexOf('[');
        if (arrayStart >= 0) {
            return type.substring(0, arrayStart);
        }
        return type;
    }
}
