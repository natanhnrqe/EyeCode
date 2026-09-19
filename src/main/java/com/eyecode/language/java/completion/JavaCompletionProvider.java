package com.eyecode.language.java.completion;

import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.editor.v2.EditorPosition;
import com.eyecode.editor.v2.EditorSelection;
import com.eyecode.editor.v2.completion.CompletionEngine;
import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.JavaKeywordCompletionProvider;
import com.eyecode.editor.v2.completion.JavaSnippetProvider;
import com.eyecode.editor.v2.completion.JavaStandardLibraryProvider;
import com.eyecode.editor.v2.completion.insert.CompletionPrefixResolver;
import com.eyecode.editor.v2.completion.knowledge.JavaKnowledgeBaseProvider;
import com.eyecode.editor.v2.completion.semantic.JavaSemanticMemberCompletionProvider;
import com.eyecode.editor.v2.completion.semantic.SemanticCompletionProvider;
import com.eyecode.editor.v2.completion.semantic.SemanticSymbolRegistry;
import com.eyecode.editor.v2.language.LanguageContext;
import com.eyecode.editor.v2.syntax.JavaSyntaxAnalyzer;
import com.eyecode.language.LanguageId;
import com.eyecode.language.completion.CompletionCandidate;
import com.eyecode.language.completion.CompletionRequest;
import com.eyecode.language.completion.CompletionResult;
import com.eyecode.language.completion.CompletionProvider;

import java.util.List;

public final class JavaCompletionProvider implements CompletionProvider {
    private final JavaSyntaxAnalyzer syntaxAnalyzer;
    private final CompletionEngine engine;

    public JavaCompletionProvider() {
        this(new JavaSyntaxAnalyzer(), new CompletionEngine(List.of(
                new JavaKeywordCompletionProvider(),
                new JavaSemanticMemberCompletionProvider(),
                new JavaKnowledgeBaseProvider(),
                new JavaStandardLibraryProvider(),
                new JavaSnippetProvider(),
                new SemanticCompletionProvider(new SemanticSymbolRegistry())
        )));
    }

    JavaCompletionProvider(JavaSyntaxAnalyzer syntaxAnalyzer, CompletionEngine engine) {
        this.syntaxAnalyzer = syntaxAnalyzer;
        this.engine = engine;
    }

    @Override
    public LanguageId languageId() {
        return LanguageId.JAVA;
    }

    @Override
    public CompletionResult complete(CompletionRequest request) {
        EditorDocument document = new EditorDocument(request.document().sourceFile(), request.source());
        EditorPosition caret = document.positionOf(request.caretOffset());
        LanguageContext context = new LanguageContext(document, caret, new EditorSelection(caret, caret),
                syntaxAnalyzer.analyze(document), com.eyecode.editor.v2.diagnostics.DiagnosticSnapshot.empty());
        var snapshot = engine.complete(context, request.explicit());
        String prefix = CompletionPrefixResolver.resolvePrefix(context);
        int start = request.replaceStart() >= 0 && request.replaceStart() <= request.caretOffset()
                ? request.replaceStart() : Math.max(0, request.caretOffset() - prefix.length());
        int end = request.replaceEnd() >= request.caretOffset() ? request.replaceEnd() : request.caretOffset();
        int boundedStart = Math.max(0, Math.min(start, request.source().length()));
        int boundedEnd = Math.max(boundedStart, Math.min(end, request.source().length()));
        return new CompletionResult(snapshot.getItems().stream()
                .limit(100)
                .map(item -> candidate(item, boundedStart, boundedEnd, prefix)).toList());
    }

    private CompletionCandidate candidate(CompletionItem item, int replaceStart, int replaceEnd, String prefix) {
        return new CompletionCandidate(item.getLabel(), item.getKind().name(), item.getDetail(), item.getDocumentation(),
                item.getInsertText(), item.getLabel(), item.getKind() == com.eyecode.editor.v2.completion.CompletionItemKind.SNIPPET,
                replaceStart, replaceEnd, item.getPriority(), item.getSignature(), item.getReturnType(), item.getOwner(),
                item.getExample(), item.getCategory(), engine.matchIndices(item, prefix));
    }
}
