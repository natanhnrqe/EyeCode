package com.eyecode.editor.v2.language.java.symbols;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionItemKind;
import com.eyecode.editor.v2.completion.database.CompletionDatabase;

import java.util.ArrayList;
import java.util.List;

public final class StandardLibrarySymbols {

    private StandardLibrarySymbols() {
    }

    public static List<ProjectSymbol> all() {
        List<ProjectSymbol> symbols = new ArrayList<>();
        for (CompletionItem item : CompletionDatabase.getAll()) {
            symbols.add(toSymbol(item));
        }
        return symbols;
    }

    private static ProjectSymbol toSymbol(CompletionItem item) {
        ProjectSymbol symbol = new ProjectSymbol();
        symbol.setName(item.getLabel());
        symbol.setKind(toSymbolKind(item.getKind()));
        symbol.setSignature(item.getSignature());
        symbol.setType(item.getReturnType());
        String owner = item.getOwner();
        if (owner != null) {
            int dot = owner.lastIndexOf('.');
            if (dot >= 0) owner = owner.substring(dot + 1);
        }
        if (item.getKind() == CompletionItemKind.CLASS
                || item.getKind() == CompletionItemKind.INTERFACE
                || item.getKind() == CompletionItemKind.ENUM
                || item.getKind() == CompletionItemKind.RECORD) {
            symbol.setOwner(null);
        } else {
            symbol.setOwner(owner);
        }
        return symbol;
    }

    private static SymbolKind toSymbolKind(CompletionItemKind kind) {
        return switch (kind) {
            case CLASS -> SymbolKind.CLASS;
            case INTERFACE -> SymbolKind.INTERFACE;
            case ENUM -> SymbolKind.ENUM;
            case RECORD -> SymbolKind.RECORD;
            case METHOD -> SymbolKind.METHOD;
            case FIELD -> SymbolKind.FIELD;
            case CONSTRUCTOR -> SymbolKind.CONSTRUCTOR;
            case VARIABLE, PACKAGE, KEYWORD, SNIPPET -> SymbolKind.LOCAL_VARIABLE;
        };
    }
}
