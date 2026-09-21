package com.eyecode.diagnostics;

import com.eyecode.language.diagnostics.Diagnostic;
import com.eyecode.language.diagnostics.DiagnosticEducation;
import com.eyecode.language.diagnostics.DiagnosticSeverity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaDiagnosticExplainerTest {
    private final JavaDiagnosticExplainer explainer = new JavaDiagnosticExplainer();

    @Test
    void explainsStructuredMissingClosingParenthesis() {
        Diagnostic diagnostic = new Diagnostic(DiagnosticSeverity.ERROR, "compiler.err.expected", "')' expected",
                4, 17, 4, 18, "MISSING_CLOSING_PARENTHESIS");
        DiagnosticEducation education = explainer.explain(diagnostic).orElseThrow();
        assertEquals("Parece que sua condição está incompleta", education.title());
        assertTrue(education.explanation().contains("`)`"));
        assertEquals(3, education.relatedContent().size());
    }

    @Test
    void leavesUnsupportedDiagnosticWithoutFabricatedExplanation() {
        Diagnostic diagnostic = new Diagnostic(DiagnosticSeverity.ERROR, "compiler.err.expected", "';' expected",
                4, 17, 4, 18);
        assertTrue(explainer.explain(diagnostic).isEmpty());
    }
}
