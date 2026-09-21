package com.eyecode.diagnostics;

import com.eyecode.language.LanguageId;
import com.eyecode.language.diagnostics.Diagnostic;
import com.eyecode.language.diagnostics.DiagnosticEducation;
import com.eyecode.language.diagnostics.DiagnosticExplainer;
import com.eyecode.language.diagnostics.DiagnosticRelatedContent;

import java.util.List;
import java.util.Optional;

public final class JavaDiagnosticExplainer implements DiagnosticExplainer {
    private static final String MISSING_CLOSING_PARENTHESIS = "MISSING_CLOSING_PARENTHESIS";

    @Override
    public LanguageId languageId() {
        return LanguageId.JAVA;
    }

    @Override
    public Optional<DiagnosticEducation> explain(Diagnostic diagnostic) {
        if (!MISSING_CLOSING_PARENTHESIS.equals(diagnostic.category())) return Optional.empty();
        return Optional.of(new DiagnosticEducation(
                "Parece que sua condição está incompleta",
                "Está faltando fechar a condição com `)`.",
                "O compilador encontrou o início do bloco antes de encontrar o `)` que fecha a condição.",
                "if (condição) {\n    // código\n}",
                "if (idade >= 18) {\n    System.out.println(\"Você é maior de idade\");\n}",
                "Depois de escrever a condição, confira se os parênteses e chaves estão fechados.",
                List.of(
                        new DiagnosticRelatedContent("java.if-structure", "Estrutura do if", "Condição e bloco de código"),
                        new DiagnosticRelatedContent("java.parentheses-braces", "Parênteses e chaves", "Delimitadores que organizam o código"),
                        new DiagnosticRelatedContent("java.comparison-operators", "Operadores de comparação", "Compare valores dentro da condição")
                )));
    }
}
