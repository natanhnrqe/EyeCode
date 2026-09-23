package com.eyecode.language.java.signature;

import com.eyecode.language.LanguageFeatureRequest;
import com.eyecode.language.LanguageId;
import com.eyecode.language.java.lsp.JdtLsProjectService;
import com.eyecode.language.signature.SignatureHelpProvider;
import com.eyecode.language.signature.SignatureHelpResult;

import java.util.Optional;

public final class JavaSignatureHelpProvider implements SignatureHelpProvider {
    private final JdtLsProjectService jdt;

    public JavaSignatureHelpProvider(JdtLsProjectService jdt) {
        this.jdt = jdt;
    }

    @Override
    public LanguageId languageId() {
        return LanguageId.JAVA;
    }

    @Override
    public Optional<SignatureHelpResult> signatureHelp(LanguageFeatureRequest request) {
        return jdt == null ? Optional.empty() : jdt.signatureHelp(request);
    }
}
