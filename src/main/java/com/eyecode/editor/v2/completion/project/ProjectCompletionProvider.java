package com.eyecode.editor.v2.completion.project;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionProvider;
import com.eyecode.editor.v2.completion.CompletionSnapshot;
import com.eyecode.editor.v2.language.LanguageContext;
import com.eyecode.editor.v2.language.LanguageContextQueries;
import com.eyecode.editor.v2.language.java.symbols.CompletionSymbolAdapter;
import com.eyecode.editor.v2.language.java.symbols.ProjectSymbol;
import com.eyecode.editor.v2.language.java.symbols.SemanticResolver;
import com.eyecode.editor.v2.language.java.symbols.SymbolKind;
import com.eyecode.editor.v2.project.ProjectIndexer;
import com.eyecode.editor.v2.project.ProjectSymbolIndex;

import java.nio.file.Path;
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
        ensureFileIndexed(context);
        SemanticResolver resolver = index.resolver();
        List<ProjectSymbol> symbols;

        String receiver = getDotReceiver(context);
        if (receiver != null) {
            ProjectSymbol receiverSymbol;
            if (receiver.equals("this")) {
                receiverSymbol = resolveCurrentClass(context, resolver);
            } else if (receiver.equals("super")) {
                ProjectSymbol currentClass = resolveCurrentClass(context, resolver);
                receiverSymbol = resolveSuperClass(currentClass, resolver);
            } else {
                receiverSymbol = resolver.resolveVariable(receiver);
            }
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
            SymbolKind k = symbol.getKind();
            if (k != SymbolKind.CLASS && k != SymbolKind.INTERFACE
                    && k != SymbolKind.ENUM && k != SymbolKind.RECORD) continue;
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

    private ProjectSymbol resolveSuperClass(ProjectSymbol classSymbol, SemanticResolver resolver) {
        return resolver.resolveSuperClass(classSymbol);
    }

    private void ensureFileIndexed(LanguageContext context) {
        Path sourceFile = context.getDocument().getSourceFile();
        if (sourceFile == null) return;
        if (!index.getForFile(sourceFile).isEmpty()) return;

        String source = context.getDocument().getText();
        List<ProjectSymbol> symbols = ProjectIndexer.parseSymbols(sourceFile, source);
        if (symbols.isEmpty()) return;
        index.replaceFile(sourceFile, symbols);
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
