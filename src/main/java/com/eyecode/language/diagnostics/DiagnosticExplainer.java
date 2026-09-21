package com.eyecode.language.diagnostics;

import com.eyecode.language.LanguageId;

import java.util.Optional;

public interface DiagnosticExplainer {
    LanguageId languageId();

    Optional<DiagnosticEducation> explain(Diagnostic diagnostic);
}
