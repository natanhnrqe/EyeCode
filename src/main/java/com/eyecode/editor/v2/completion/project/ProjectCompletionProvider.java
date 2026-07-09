package com.eyecode.editor.v2.completion.project;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionProvider;
import com.eyecode.editor.v2.completion.CompletionSnapshot;
import com.eyecode.editor.v2.language.LanguageContext;
import com.eyecode.editor.v2.language.LanguageContextQueries;
import com.eyecode.editor.v2.language.java.symbols.CompletionSymbolAdapter;
import com.eyecode.editor.v2.language.java.symbols.ProjectSymbol;
import com.eyecode.editor.v2.language.java.symbols.SemanticResolver;
import com.eyecode.editor.v2.project.ProjectSymbolIndex;

import java.util.List;

public final class ProjectCompletionProvider implements CompletionProvider {

    private final ProjectSymbolIndex index;
    private final CompletionSymbolAdapter adapter;

    public ProjectCompletionProvider(ProjectSymbolIndex index) {
        this.index = index;
        this.adapter = new CompletionSymbolAdapter();
    }

    @Override
    public CompletionSnapshot complete(LanguageContext context) {
        SemanticResolver resolver = index.resolver();
        List<ProjectSymbol> symbols;

        String receiver = getDotReceiver(context);
        if (receiver != null) {
            ProjectSymbol receiverSymbol = receiver.equals("this")
                    ? resolveCurrentClass(context, resolver)
                    : resolver.resolveVariable(receiver);
            symbols = resolver.resolveMembers(receiverSymbol);
        } else {
            symbols = resolver.resolveVisibleSymbols();
        }

        String prefix = LanguageContextQueries.getCurrentWordPrefix(context);
        if (!prefix.isEmpty()) {
            symbols = symbols.stream()
                    .filter(symbol -> symbol.getName().toLowerCase().startsWith(prefix.toLowerCase()))
                    .toList();
        }

        List<CompletionItem> items = symbols.stream()
                .map(adapter::toCompletionItem)
                .toList();

        return new CompletionSnapshot(items);
    }

    private String getDotReceiver(LanguageContext context) {
        String text = context.getDocument().getText();
        int offset = offsetForPosition(context);
        int safeOffset = Math.max(0, Math.min(offset, text.length()));

        if (safeOffset < 1) return null;

        int pos = safeOffset;
        while (pos > 0 && Character.isJavaIdentifierPart(text.charAt(pos - 1))) {
            pos--;
        }

        if (pos < 1 || text.charAt(pos - 1) != '.') {
            return null;
        }

        int receiverEnd = pos - 1;
        int receiverStart = receiverEnd;
        while (receiverStart > 0 && Character.isJavaIdentifierPart(text.charAt(receiverStart - 1))) {
            receiverStart--;
        }

        if (receiverStart >= receiverEnd) return null;
        return text.substring(receiverStart, receiverEnd);
    }

    private ProjectSymbol resolveCurrentClass(LanguageContext context, SemanticResolver resolver) {
        String text = context.getDocument().getText();
        int offset = offsetForPosition(context);
        int safeOffset = Math.max(0, Math.min(offset, text.length()));
        ProjectSymbol current = null;

        for (ProjectSymbol symbol : resolver.resolveVisibleSymbols()) {
            Object ast = symbol.getAstReference();
            if (ast == null) continue;
            if (symbol.getSourceFile() == null) continue;
            if (!symbol.getSourceFile().equals(context.getDocument().getSourceFile())) continue;
            int idx = text.indexOf(symbol.getName());
            if (idx >= 0 && idx <= safeOffset) {
                current = symbol;
            }
        }
        return current;
    }

    private int offsetForPosition(LanguageContext context) {
        String text = context.getDocument().getText();
        int line = 0;
        int column = 0;
        for (int offset = 0; offset < text.length(); offset++) {
            if (line == context.getCaret().line() && column == context.getCaret().column()) {
                return offset;
            }
            char current = text.charAt(offset);
            if (current == '\n') {
                line++;
                column = 0;
            } else {
                column++;
            }
        }
        return text.length();
    }
}
