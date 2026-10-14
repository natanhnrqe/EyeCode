package com.eyecode.diagnostics;

import com.eyecode.language.diagnostics.Diagnostic;
import com.eyecode.language.diagnostics.DiagnosticEducation;
import com.eyecode.language.diagnostics.DiagnosticSeverity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaDiagnosticExplainerTest {
    private static final List<String> CATALOG = List.of(
            "java/jdk/fundamentos/variables", "java/jdk/fundamentos/classes",
            "java/jdk/java.lang/string", "java/jdk/java.lang/system");
    private final JavaDiagnosticExplainer explainer = new JavaDiagnosticExplainer();

    @Test
    void explainsStructuredMissingClosingParenthesis() {
        Diagnostic diagnostic = new Diagnostic(DiagnosticSeverity.ERROR, "compiler.err.expected", "')' expected",
                4, 17, 4, 18, "MISSING_CLOSING_PARENTHESIS");
        DiagnosticEducation education = explainer.explain(diagnostic).orElseThrow();
        assertEquals("Parece que sua condição está incompleta", education.title());
        assertTrue(education.explanation().contains("`)`"));
        assertEquals(2, education.relatedContent().size());
        assertTrue(education.relatedContent().stream().allMatch(related -> CATALOG.contains(related.id())));
    }

    @Test
    void explainsMissingSemicolonFromRealJavacMessage() {
        Diagnostic diagnostic = new Diagnostic(DiagnosticSeverity.ERROR, "compiler.err.expected", "';' expected",
                4, 17, 4, 18);
        DiagnosticEducation education = explainer.explain(diagnostic).orElseThrow();
        assertEquals("Falta o ponto e vírgula", education.title());
        assertTrue(education.correctedPattern().endsWith(";"));
    }

    @Test
    void explainsUndeclaredVariableFromRealJdtMessage() {
        Diagnostic diagnostic = new Diagnostic(DiagnosticSeverity.ERROR, "", "saldo cannot be resolved to a variable",
                3, 20, 3, 25);
        DiagnosticEducation education = explainer.explain(diagnostic).orElseThrow();
        assertEquals("Variável não declarada", education.title());
        assertTrue(education.relatedContent().stream()
                .anyMatch(related -> "java/jdk/fundamentos/variables".equals(related.id())));
    }

    @Test
    void explainsUndefinedMethodFromRealJdtMessage() {
        Diagnostic diagnostic = new Diagnostic(DiagnosticSeverity.ERROR, "67108964",
                "The method printlne(String) is undefined for the type Main", 4, 9, 4, 25);
        assertEquals("Método não encontrado", explainer.explain(diagnostic).orElseThrow().title());
    }

    @Test
    void explainsTypeMismatchFromRealJdtMessage() {
        Diagnostic diagnostic = new Diagnostic(DiagnosticSeverity.ERROR, "0",
                "Type mismatch: cannot convert from double to int", 3, 5, 3, 16);
        assertEquals("Tipos incompatíveis", explainer.explain(diagnostic).orElseThrow().title());
    }

    @Test
    void explainsUnclosedStringFromRealJavacMessage() {
        Diagnostic diagnostic = new Diagnostic(DiagnosticSeverity.ERROR, "compiler.err.unclosed.str.lit",
                "unclosed string literal", 3, 22, 3, 30);
        assertEquals("Texto sem fechar", explainer.explain(diagnostic).orElseThrow().title());
    }

    @Test
    void explainsUnclosedClassBodyFromRealJavacMessage() {
        Diagnostic diagnostic = new Diagnostic(DiagnosticSeverity.ERROR, "compiler.err.premature.eof",
                "reached end of file while parsing", 9, 1, 9, 1);
        assertEquals("Classe sem fechar", explainer.explain(diagnostic).orElseThrow().title());
    }

    @Test
    void explainsDuplicateVariableFromRealMessages() {
        Diagnostic javac = new Diagnostic(DiagnosticSeverity.ERROR, "compiler.err.already.defined",
                "variable total is already defined in method main(String[])", 4, 9, 4, 14);
        assertEquals("Nome já usado", explainer.explain(javac).orElseThrow().title());
        Diagnostic jdt = new Diagnostic(DiagnosticSeverity.ERROR, "0", "Duplicate local variable total",
                4, 9, 4, 14);
        assertEquals("Nome já usado", explainer.explain(jdt).orElseThrow().title());
    }

    @Test
    void explainsNotAStatementFromRealJavacMessage() {
        Diagnostic diagnostic = new Diagnostic(DiagnosticSeverity.ERROR, "compiler.err.not.stmt",
                "not a statement", 3, 5, 3, 12);
        assertEquals("Instrução incompleta", explainer.explain(diagnostic).orElseThrow().title());
    }

    @Test
    void explainsDivisionByZeroFromRealJvmMessage() {
        Diagnostic diagnostic = new Diagnostic(DiagnosticSeverity.ERROR, "runtime",
                "java.lang.ArithmeticException: / by zero", 4, 21, 4, 27);
        DiagnosticEducation education = explainer.explain(diagnostic).orElseThrow();
        assertEquals("Divisão por zero", education.title());
        assertTrue(education.explanation().contains("ArithmeticException"));
    }

    @Test
    void leavesUnsupportedDiagnosticWithoutFabricatedExplanation() {
        Diagnostic diagnostic = new Diagnostic(DiagnosticSeverity.ERROR, "compiler.err.some.future",
                "algum erro ainda não catalogado", 4, 17, 4, 18);
        assertTrue(explainer.explain(diagnostic).isEmpty());
    }

    @Test
    void everyRelatedContentIdExistsInTheDocumentationCatalog() {
        for (String message : List.of("';' expected", "saldo cannot be resolved to a variable",
                "The method printlne(String) is undefined for the type Main",
                "Type mismatch: cannot convert from double to int", "unclosed string literal",
                "reached end of file while parsing", "not a statement", "Duplicate local variable total",
                "illegal start of expression", "Strig cannot be resolved to a type")) {
            DiagnosticEducation education = explainer.explain(new Diagnostic(DiagnosticSeverity.ERROR, "", message, 1, 1, 1, 2)).orElseThrow();
            String ids = education.relatedContent().stream().map(related -> related.id() + " title=" + related.title())
                    .reduce((first, second) -> first + " | " + second).orElse("");
            assertTrue(education.relatedContent().stream().allMatch(related -> CATALOG.contains(related.id())),
                    "ids=[" + ids + "] message=" + message);
        }
    }
}
