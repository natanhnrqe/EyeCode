package com.eyecode.diagnostics;

import com.eyecode.language.LanguageId;
import com.eyecode.language.diagnostics.Diagnostic;
import com.eyecode.language.diagnostics.DiagnosticSeverity;
import com.eyecode.language.diagnostics.DiagnosticsProvider;
import com.eyecode.language.diagnostics.DiagnosticsRequest;
import com.eyecode.language.diagnostics.DiagnosticsResult;

public final class JavaDiagnosticsProvider implements DiagnosticsProvider {
    private final JavaSyntaxDiagnosticAnalyzer analyzer;

    public JavaDiagnosticsProvider() {
        this(new JavaSyntaxDiagnosticAnalyzer());
    }

    public JavaDiagnosticsProvider(JavaSyntaxDiagnosticAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    @Override
    public LanguageId languageId() {
        return LanguageId.JAVA;
    }

    @Override
    public DiagnosticsResult analyze(DiagnosticsRequest request) {
        JavaDiagnosticsResult result = analyzer.analyze(new JavaDiagnosticRequest(request.document().uri(), "",
                request.version(), request.source()));
        return new DiagnosticsResult(result.diagnostics().stream().map(this::diagnostic).toList(),
                result.infrastructureError());
    }

    private Diagnostic diagnostic(JavaDiagnostic value) {
        return new Diagnostic(switch (value.severity()) {
            case ERROR -> DiagnosticSeverity.ERROR;
            case WARNING -> DiagnosticSeverity.WARNING;
            case INFO -> DiagnosticSeverity.INFO;
            case HINT -> DiagnosticSeverity.HINT;
        }, value.code(), value.message(), value.startLine(), value.startColumn(), value.endLine(), value.endColumn());
    }
}
