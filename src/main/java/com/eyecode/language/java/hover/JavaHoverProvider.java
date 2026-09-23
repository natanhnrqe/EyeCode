package com.eyecode.language.java.hover;

import com.eyecode.language.LanguageFeatureRequest;
import com.eyecode.language.LanguageId;
import com.eyecode.language.hover.HoverProvider;
import com.eyecode.language.hover.HoverResult;
import com.eyecode.language.java.lsp.JdtLsProjectService;

import java.util.Optional;

public final class JavaHoverProvider implements HoverProvider {
    private final JdtLsProjectService jdt;

    public JavaHoverProvider(JdtLsProjectService jdt) {
        this.jdt = jdt;
    }

    @Override
    public LanguageId languageId() {
        return LanguageId.JAVA;
    }

    @Override
    public Optional<HoverResult> hover(LanguageFeatureRequest request) {
        return jdt == null ? Optional.empty() : jdt.hover(request);
    }
}
