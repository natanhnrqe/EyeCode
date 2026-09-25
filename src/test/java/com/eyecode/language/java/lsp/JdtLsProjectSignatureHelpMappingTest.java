package com.eyecode.language.java.lsp;

import org.eclipse.lsp4j.ParameterInformation;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureInformation;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.Tuple;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdtLsProjectSignatureHelpMappingTest {
    @Test
    void mapsTupleParameterLabelsAndActiveIndices() {
        ParameterInformation begin = new ParameterInformation();
        begin.setLabel(Either.forRight(Tuple.two(30, 35)));
        begin.setDocumentation(Either.forLeft("primeiro"));
        ParameterInformation end = new ParameterInformation();
        end.setLabel(Either.forRight(Tuple.two(37, 39)));
        SignatureInformation info = new SignatureInformation();
        info.setLabel("substring(int begin, int end)");
        info.setDocumentation(Either.forLeft("extrai trecho"));
        info.setParameters(List.of(begin, end));
        info.setActiveParameter(1);
        SignatureHelp help = new SignatureHelp();
        help.setSignatures(List.of(info));
        help.setActiveSignature(0);
        help.setActiveParameter(1);

        var result = JdtLsProjectCompletion.toSignatureHelpResult(help);

        assertEquals(1, result.signatures().size());
        assertEquals("substring(int begin, int end)", result.signatures().getFirst().label());
        assertEquals("extrai trecho", result.signatures().getFirst().documentation());
        assertEquals(2, result.signatures().getFirst().parameters().size());
        assertEquals(30, result.signatures().getFirst().parameters().getFirst().labelStart());
        assertEquals(35, result.signatures().getFirst().parameters().getFirst().labelEnd());
        assertEquals("primeiro", result.signatures().getFirst().parameters().getFirst().documentation());
        assertEquals(1, result.signatures().getFirst().activeParameter());
        assertEquals(0, result.activeSignature());
        assertEquals(1, result.activeParameter());
    }

    @Test
    void mapsStringParameterLabelAndNullDocumentation() {
        ParameterInformation parameter = new ParameterInformation();
        parameter.setLabel(Either.forLeft("begin"));
        SignatureInformation info = new SignatureInformation();
        info.setLabel("substring(String s)");
        info.setParameters(List.of(parameter));
        SignatureHelp help = new SignatureHelp();
        help.setSignatures(List.of(info));

        var result = JdtLsProjectCompletion.toSignatureHelpResult(help);

        assertEquals("begin", result.signatures().getFirst().parameters().getFirst().label());
        assertEquals("", result.signatures().getFirst().parameters().getFirst().documentation());
        assertEquals("", result.signatures().getFirst().documentation());
        assertNull(result.activeSignature());
        assertNull(result.activeParameter());
    }

    @Test
    void nullSignaturesBecomeEmptyList() {
        var result = JdtLsProjectCompletion.toSignatureHelpResult(new SignatureHelp());
        assertTrue(result.signatures().isEmpty());
    }

    @Test
    void nullParameterListBecomesEmptyList() {
        SignatureInformation info = new SignatureInformation();
        info.setLabel("run()");
        SignatureHelp help = new SignatureHelp();
        help.setSignatures(List.of(info));

        var result = JdtLsProjectCompletion.toSignatureHelpResult(help);

        assertTrue(result.signatures().getFirst().parameters().isEmpty());
    }
}
