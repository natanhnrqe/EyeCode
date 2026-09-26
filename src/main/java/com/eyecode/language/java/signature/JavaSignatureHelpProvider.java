package com.eyecode.language.java.signature;

import com.eyecode.language.LanguageFeatureRequest;
import com.eyecode.language.LanguageId;
import com.eyecode.language.java.lsp.JdtLsProjectService;
import com.eyecode.language.signature.SignatureHelpProvider;
import com.eyecode.language.signature.SignatureHelpResult;

import java.util.Optional;

public final class JavaSignatureHelpProvider implements SignatureHelpProvider {
    private final JdtLsProjectService jdt;
    private final LocalSignatureHelpResolver localResolver = new LocalSignatureHelpResolver();

    public JavaSignatureHelpProvider(JdtLsProjectService jdt) {
        this.jdt = jdt;
    }

    @Override
    public LanguageId languageId() {
        return LanguageId.JAVA;
    }

    @Override
    public Optional<SignatureHelpResult> signatureHelp(LanguageFeatureRequest request) {
        Optional<SignatureHelpResult> fromJdt = jdt == null ? Optional.empty() : jdt.signatureHelp(request);
        if (fromJdt.isPresent() && !fromJdt.get().signatures().isEmpty()) {
            return fromJdt;
        }
        return localResolver.resolve(request.source(), request.caretOffset());
    }
}
