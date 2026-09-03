package com.eyecode.diagnostics;

import org.junit.jupiter.api.Test;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaSyntaxDiagnosticAnalyzerTest {

    private final JavaSyntaxDiagnosticAnalyzer analyzer = new JavaSyntaxDiagnosticAnalyzer();

    @Test
    void validJavaProducesNoDiagnostics() {
        JavaDiagnosticsResult result = analyzer.analyze(request("class Main { void run() { int value = 1; } }"));
        assertFalse(result.hasInfrastructureError());
        assertTrue(result.diagnostics().isEmpty());
    }

    @Test
    void missingSemicolonProducesSyntaxDiagnostic() {
        JavaDiagnosticsResult result = analyzer.analyze(request("class Main { void run() { int value = 1 } }"));
        assertFalse(result.diagnostics().isEmpty());
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.severity() == JavaDiagnosticSeverity.ERROR));
    }

    @Test
    void malformedExpressionProducesSyntaxDiagnostic() {
        JavaDiagnosticsResult result = analyzer.analyze(request("class Main { void run() { int value = ; } }"));
        assertFalse(result.diagnostics().isEmpty());
    }

    @Test
    void preservesRequestIdentityAndValidRanges() {
        JavaDiagnosticsResult result = analyzer.analyze(new JavaDiagnosticRequest("file:///C:/project/Main.java", "42", 19,
                "class Main { void run() { int value = ; } }"));
        JavaDiagnostic diagnostic = result.diagnostics().getFirst();
        assertEquals("file:///C:/project/Main.java", diagnostic.uri());
        assertEquals("42", diagnostic.requestId());
        assertEquals(19, diagnostic.modelVersion());
        assertTrue(diagnostic.startLine() >= 1);
        assertTrue(diagnostic.startColumn() >= 1);
        assertTrue(diagnostic.endLine() > diagnostic.startLine()
                || diagnostic.endColumn() > diagnostic.startColumn());
    }

    @Test
    void missingPositionsProduceSafeNonEmptyRange() {
        JavaSyntaxDiagnosticAnalyzer.Range range = JavaSyntaxDiagnosticAnalyzer.range("", -1, -1);
        assertEquals(1, range.startLine());
        assertEquals(1, range.startColumn());
        assertEquals(1, range.endLine());
        assertEquals(2, range.endColumn());
    }

    @Test
    void unavailableCompilerProducesInfrastructureFailure() {
        JavaSyntaxDiagnosticAnalyzer unavailable = new JavaSyntaxDiagnosticAnalyzer(() -> null);
        JavaDiagnosticsResult result = unavailable.analyze(request("class Main {}"));
        assertTrue(result.hasInfrastructureError());
        assertTrue(result.diagnostics().isEmpty());
    }

    @Test
    void parseOnlyAnalysisDoesNotRequireProjectClasspath() {
        JavaDiagnosticsResult result = analyzer.analyze(request("import missing.Dependency; class Main {}"));
        assertFalse(result.hasInfrastructureError());
        assertTrue(result.diagnostics().isEmpty());
    }

    private JavaDiagnosticRequest request(String source) {
        return new JavaDiagnosticRequest("file:///C:/project/Main.java", "request-7", 7, source);
    }
}