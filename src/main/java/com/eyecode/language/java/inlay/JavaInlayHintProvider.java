package com.eyecode.language.java.inlay;

import com.eyecode.language.inlay.InlayHintProvider;
import com.eyecode.language.inlay.InlayHintRequest;
import com.eyecode.language.inlay.InlayHintResult;
import com.eyecode.language.java.signature.LocalSignatureHelpResolver;
import com.eyecode.language.LanguageId;

public final class JavaInlayHintProvider implements InlayHintProvider {
    private final LocalSignatureHelpResolver localResolver = new LocalSignatureHelpResolver();

    @Override
    public LanguageId languageId() {
        return LanguageId.JAVA;
    }

    @Override
    public InlayHintResult inlayHints(InlayHintRequest request) {
        return new InlayHintResult(localResolver.resolveInlayHints(
                request.source(), request.fromOffset(), request.toOffset(), request.mode()));
    }
}
