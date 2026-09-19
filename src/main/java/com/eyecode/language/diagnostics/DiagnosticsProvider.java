package com.eyecode.language.diagnostics;

import com.eyecode.language.LanguageId;

public interface DiagnosticsProvider {
    LanguageId languageId();

    DiagnosticsResult analyze(DiagnosticsRequest request);
}
