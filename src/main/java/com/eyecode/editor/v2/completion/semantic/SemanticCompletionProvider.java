package com.eyecode.editor.v2.completion.semantic;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionItemKind;
import com.eyecode.editor.v2.completion.CompletionContextKind;
import com.eyecode.editor.v2.completion.CompletionProvider;
import com.eyecode.editor.v2.completion.CompletionSnapshot;
import com.eyecode.editor.v2.completion.insert.CompletionPrefixResolver;
import com.eyecode.editor.v2.language.LanguageContext;
import com.eyecode.editor.v2.syntax.SyntaxToken;
import com.eyecode.editor.v2.syntax.TokenType;
import com.eyecode.language.symbol.DocumentSemanticModelBuilder;
import com.eyecode.language.symbol.SemanticModelSnapshot;
import com.eyecode.language.symbol.Symbol;
import com.eyecode.language.symbol.SymbolKind;
import com.eyecode.language.symbol.SymbolModifier;
import com.eyecode.language.symbol.SymbolScope;
import com.eyecode.language.symbol.SymbolTable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class SemanticCompletionProvider implements CompletionProvider {

    private final SemanticSymbolRegistry registry;
    private final DocumentSemanticModelBuilder semanticModelBuilder;

    public SemanticCompletionProvider(SemanticSymbolRegistry registry) {
        this(registry, new DocumentSemanticModelBuilder());
    }

    public SemanticCompletionProvider(SemanticSymbolRegistry registry,
                                      DocumentSemanticModelBuilder semanticModelBuilder) {
        this.registry = registry;
        this.semanticModelBuilder = semanticModelBuilder;
    }

    @Override
    public boolean supports(CompletionContextKind contextKind) {
        return contextKind != CompletionContextKind.MEMBER_ACCESS;
    }

    @Override
    public CompletionSnapshot complete(LanguageContext context) {
        return complete(context, false);
    }

    @Override
    public CompletionSnapshot complete(LanguageContext context, boolean manual) {
        if (context == null || isTriviaContext(context)) {
            return CompletionSnapshot.empty();
        }
        Optional<SemanticModelSnapshot> semantic = semanticModelBuilder.build(context.getDocument());
        if (semantic.isEmpty()) {
            return fallbackRegistryItems(context, manual);
        }
        CompletionQuery query = completionQuery(context);
        if (!query.qualified() && query.prefix().isEmpty() && !manual) {
            return CompletionSnapshot.empty();
        }
        SymbolTable table = semantic.get().symbolTable();
        Map<String, CompletionItem> items = new LinkedHashMap<>();
        if (query.qualified()) {
            completeQualified(query, table, items);
        } else {
            completeVisible(query, table, items);
        }
        return new CompletionSnapshot(List.copyOf(items.values()));
    }

    private CompletionSnapshot fallbackRegistryItems(LanguageContext context, boolean manual) {
        String prefix = CompletionPrefixResolver.resolvePrefix(context);
        if (prefix.isEmpty() && !manual) {
            return CompletionSnapshot.empty();
        }
        List<CompletionItem> items = registry.getSymbols().stream()
                .filter(symbol -> symbol.getName().startsWith(prefix))
                .map(symbol -> new CompletionItem(
                        symbol.getName(),
                        symbol.getName(),
                        symbol.getDetail(),
                        symbol.getKind()
                ))
                .toList();
        return new CompletionSnapshot(items);
    }

    private void completeVisible(CompletionQuery query, SymbolTable table, Map<String, CompletionItem> items) {
        SymbolScope scope = innermostScopeContaining(table, query.offset());
        ArrayDeque<SymbolScope> chain = new ArrayDeque<>();
        SymbolScope current = scope;
        while (current != null) {
            chain.addLast(current);
            current = current.parent().orElse(null);
        }
        for (SymbolScope next : chain) {
            List<Symbol> declared = new ArrayList<>(next.declaredSymbols());
            declared.sort(Comparator.comparingInt(symbol -> symbol.declarationRange().startOffset()));
            for (Symbol symbol : declared) {
                if (!matchesPrefix(symbol.name(), query.prefix())) {
                    continue;
                }
                CompletionItem item = toItem(symbol);
                if (item != null) {
                    items.putIfAbsent(item.getLabel(), item);
                }
            }
        }
    }

    private void completeQualified(CompletionQuery query, SymbolTable table, Map<String, CompletionItem> items) {
        Optional<Symbol> qualifier = resolveQualifier(query.qualifier(), table, query.offset());
        if (qualifier.isEmpty()) {
            return;
        }
        Symbol symbol = qualifier.get();
        if (!isStaticTypeQualifier(symbol)) {
            return;
        }
        SymbolScope memberScope = table.scope(symbol.scopeId()).orElse(null);
        if (memberScope == null) {
            return;
        }
        List<Symbol> members = new ArrayList<>(memberScope.declaredSymbols());
        members.sort(Comparator.comparingInt(member -> member.declarationRange().startOffset()));
        for (Symbol member : members) {
            if (!isQualifiedMemberVisible(member)) {
                continue;
            }
            if (!matchesPrefix(member.name(), query.prefix())) {
                continue;
            }
            CompletionItem item = toItem(member);
            if (item != null) {
                items.putIfAbsent(item.getLabel(), item);
            }
        }
    }

    private Optional<Symbol> resolveQualifier(String qualifier, SymbolTable table, int offset) {
        if (qualifier == null || qualifier.isBlank()) {
            return Optional.empty();
        }
        SymbolScope scope = innermostScopeContaining(table, offset);
        Optional<Symbol> scoped = scope.lookup(qualifier);
        if (scoped.isPresent()) {
            return scoped;
        }
        return table.lookup(table.rootScope().id(), qualifier);
    }

    private SymbolScope innermostScopeContaining(SymbolTable table, int offset) {
        SymbolScope best = table.rootScope();
        int bestDepth = 0;
        ArrayDeque<ScopeDepth> stack = new ArrayDeque<>();
        stack.push(new ScopeDepth(table.rootScope(), 0));
        while (!stack.isEmpty()) {
            ScopeDepth entry = stack.pop();
            SymbolScope scope = entry.scope();
            if (contains(scope, offset) && entry.depth() >= bestDepth) {
                best = scope;
                bestDepth = entry.depth();
            }
            List<SymbolScope> children = scope.children();
            for (int i = children.size() - 1; i >= 0; i--) {
                stack.push(new ScopeDepth(children.get(i), entry.depth() + 1));
            }
        }
        return best;
    }

    private boolean contains(SymbolScope scope, int offset) {
        if (scope == null) {
            return false;
        }
        int start = scope.range().startOffset();
        int end = scope.range().endOffset();
        return start <= offset && offset <= end;
    }

    private boolean matchesPrefix(String candidate, String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return true;
        }
        return candidate.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT));
    }

    private boolean isStaticTypeQualifier(Symbol symbol) {
        return switch (symbol.kind()) {
            case TYPE, INTERFACE, ENUM, ANNOTATION -> true;
            default -> false;
        };
    }

    private boolean isQualifiedMemberVisible(Symbol symbol) {
        return switch (symbol.kind()) {
            case FIELD, METHOD -> symbol.modifiers().contains(SymbolModifier.STATIC);
            case TYPE, INTERFACE, ENUM, ANNOTATION -> true;
            default -> false;
        };
    }

    private CompletionItem toItem(Symbol symbol) {
        CompletionItemKind kind = toKind(symbol.kind());
        if (kind == null) {
            return null;
        }
        CompletionItem.Builder builder = CompletionItem.builder(symbol.name(), symbol.name(), kind);
        String detail = detailFor(symbol);
        if (!detail.isBlank()) {
            builder.detail(detail);
        }
        if (symbol.qualifiedName() != null && !symbol.qualifiedName().isBlank()) {
            builder.owner(ownerFor(symbol));
        }
        builder.priority(priorityFor(symbol));
        if (kind == CompletionItemKind.METHOD || kind == CompletionItemKind.CONSTRUCTOR) {
            builder.signature(symbol.name() + "()");
        }
        return builder.build();
    }

    private CompletionItemKind toKind(SymbolKind kind) {
        return switch (kind) {
            case PACKAGE -> CompletionItemKind.PACKAGE;
            case TYPE -> CompletionItemKind.CLASS;
            case INTERFACE -> CompletionItemKind.INTERFACE;
            case ENUM -> CompletionItemKind.ENUM;
            case FIELD -> CompletionItemKind.FIELD;
            case METHOD -> CompletionItemKind.METHOD;
            case CONSTRUCTOR -> CompletionItemKind.CONSTRUCTOR;
            case PARAMETER, LOCAL_VARIABLE, TYPE_PARAMETER -> CompletionItemKind.VARIABLE;
            default -> null;
        };
    }

    private String detailFor(Symbol symbol) {
        return switch (symbol.kind()) {
            case LOCAL_VARIABLE -> "Local variable";
            case PARAMETER -> "Parameter";
            case FIELD -> "Field";
            case METHOD -> "Method";
            case CONSTRUCTOR -> "Constructor";
            case TYPE -> "Class";
            case INTERFACE -> "Interface";
            case ENUM -> "Enum";
            case PACKAGE -> "Package";
            default -> "";
        };
    }

    private String ownerFor(Symbol symbol) {
        String qualifiedName = symbol.qualifiedName();
        if (qualifiedName == null || qualifiedName.isBlank()) {
            return "";
        }
        int lastDot = qualifiedName.lastIndexOf('.');
        if (lastDot <= 0) {
            return qualifiedName;
        }
        return qualifiedName.substring(0, lastDot);
    }

    private int priorityFor(Symbol symbol) {
        return switch (symbol.kind()) {
            case LOCAL_VARIABLE -> 80;
            case PARAMETER -> 75;
            case FIELD -> 70;
            case METHOD -> 60;
            case CONSTRUCTOR -> 55;
            case TYPE, INTERFACE, ENUM -> 50;
            case PACKAGE -> 10;
            default -> 0;
        };
    }

    private boolean isTriviaContext(LanguageContext context) {
        int offset = context.getDocument().offsetOf(context.getCaret());
        for (SyntaxToken token : context.getSyntax().getTokens()) {
            if (token.startOffset() <= offset && offset <= token.endOffset()) {
                return token.type() == TokenType.COMMENT || token.type() == TokenType.STRING;
            }
        }
        return false;
    }

    private CompletionQuery completionQuery(LanguageContext context) {
        String text = context.getDocument().getText();
        int offset = context.getDocument().offsetOf(context.getCaret());
        int safeOffset = Math.max(0, Math.min(offset, text.length()));
        int prefixStart = safeOffset;
        while (prefixStart > 0 && Character.isJavaIdentifierPart(text.charAt(prefixStart - 1))) {
            prefixStart--;
        }
        String prefix = text.substring(prefixStart, safeOffset);
        if (prefixStart > 0 && text.charAt(prefixStart - 1) == '.') {
            int qualifierEnd = prefixStart - 1;
            int qualifierStart = qualifierEnd;
            while (qualifierStart > 0 && Character.isJavaIdentifierPart(text.charAt(qualifierStart - 1))) {
                qualifierStart--;
            }
            if (qualifierStart < qualifierEnd) {
                return new CompletionQuery(prefix, text.substring(qualifierStart, qualifierEnd), true, safeOffset);
            }
        }
        return new CompletionQuery(prefix, null, false, safeOffset);
    }

    private record CompletionQuery(String prefix, String qualifier, boolean qualified, int offset) {}

    private record ScopeDepth(SymbolScope scope, int depth) {}
}
