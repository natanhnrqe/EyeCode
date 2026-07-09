package com.eyecode.editor.v2.language.java.symbols;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionItemKind;

public final class CompletionSymbolAdapter {

    public CompletionItem toCompletionItem(ProjectSymbol symbol) {
        return CompletionItem.builder(symbol.getName(), insertText(symbol), toCompletionKind(symbol.getKind()))
                .detail(detail(symbol))
                .signature(symbol.getSignature())
                .returnType(symbol.getType())
                .owner(symbol.getOwner())
                .category(category(symbol.getKind()))
                .priority(priority(symbol.getKind()))
                .build();
    }

    private String insertText(ProjectSymbol symbol) {
        return switch (symbol.getKind()) {
            case CONSTRUCTOR, METHOD -> symbol.getName() + "()";
            default -> symbol.getName();
        };
    }

    private String detail(ProjectSymbol symbol) {
        return switch (symbol.getKind()) {
            case CLASS, INTERFACE, ENUM, RECORD -> symbol.getName();
            default -> "";
        };
    }

    private CompletionItemKind toCompletionKind(SymbolKind kind) {
        return switch (kind) {
            case CLASS -> CompletionItemKind.CLASS;
            case INTERFACE -> CompletionItemKind.INTERFACE;
            case ENUM -> CompletionItemKind.ENUM;
            case RECORD -> CompletionItemKind.RECORD;
            case FIELD -> CompletionItemKind.FIELD;
            case CONSTRUCTOR -> CompletionItemKind.CONSTRUCTOR;
            case METHOD -> CompletionItemKind.METHOD;
            case PARAMETER, LOCAL_VARIABLE -> CompletionItemKind.VARIABLE;
        };
    }

    private String category(SymbolKind kind) {
        return switch (kind) {
            case CLASS -> "Class";
            case INTERFACE -> "Interface";
            case ENUM -> "Enum";
            case RECORD -> "Record";
            case FIELD -> "Project Field";
            case CONSTRUCTOR -> "Constructor";
            case METHOD -> "Project Method";
            case PARAMETER -> "Parameter";
            case LOCAL_VARIABLE -> "Local Variable";
        };
    }

    private int priority(SymbolKind kind) {
        return switch (kind) {
            case FIELD -> 35;
            case CONSTRUCTOR, METHOD -> 40;
            case PARAMETER, LOCAL_VARIABLE -> 45;
            default -> 0;
        };
    }
}
