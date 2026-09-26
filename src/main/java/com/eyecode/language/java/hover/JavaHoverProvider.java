package com.eyecode.language.java.hover;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.database.CompletionDatabase;
import com.eyecode.editor.v2.completion.knowledge.JavaKnowledgeBase;
import com.eyecode.language.LanguageFeatureRequest;
import com.eyecode.language.LanguageId;
import com.eyecode.language.hover.HoverContent;
import com.eyecode.language.hover.HoverHtmlRenderer;
import com.eyecode.language.hover.HoverProvider;
import com.eyecode.language.hover.HoverResult;
import com.eyecode.language.java.doc.JavadocFormatter;
import com.eyecode.language.java.doc.LocalJavadocResolver;
import com.eyecode.language.java.lsp.JdtLsProjectService;

import java.util.List;
import java.util.Optional;

public final class JavaHoverProvider implements HoverProvider {
    private final JdtLsProjectService jdt;
    private final LocalJavadocResolver javadocResolver = new LocalJavadocResolver();

    public JavaHoverProvider(JdtLsProjectService jdt) {
        this.jdt = jdt;
    }

    @Override
    public LanguageId languageId() {
        return LanguageId.JAVA;
    }

    @Override
    public Optional<HoverResult> hover(LanguageFeatureRequest request) {
        Optional<HoverResult> result = jdt == null ? Optional.empty() : jdt.hover(request);
        if (result.isEmpty() || result.get().contents().isEmpty()) {
            result = local(request);
        }
        return result.flatMap(HoverHtmlRenderer::toHtml);
    }

    private Optional<HoverResult> local(LanguageFeatureRequest request) {
        Optional<LocalJavadocResolver.JavadocAtCaret> atCaret =
                javadocResolver.atCaret(request.source(), request.caretOffset());
        if (atCaret.isEmpty()) {
            return Optional.empty();
        }
        LocalJavadocResolver.JavadocAtCaret symbol = atCaret.get();
        String documentation = JavadocFormatter.format(symbol.javadoc());
        if (documentation.isBlank() && !symbol.declared() && !symbol.variable()) {
            documentation = knowledgeDocumentation(symbol.symbol());
        }
        if (documentation.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new HoverResult(List.of(new HoverContent("markdown", documentation)),
                symbol.rangeStart(), symbol.rangeEnd()));
    }

    private static String knowledgeDocumentation(String name) {
        CompletionItem item = CompletionDatabase.get(name);
        if (item == null) {
            item = JavaKnowledgeBase.get(name);
        }
        return item == null || item.getDocumentation() == null ? "" : item.getDocumentation();
    }
}
