package com.eyecode.language.java.signature;

import com.eyecode.language.signature.SignatureHelpResult;
import com.eyecode.language.signature.SignatureInformation;
import com.eyecode.language.signature.SignatureParameter;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalSignatureHelpResolverTest {

    private final LocalSignatureHelpResolver resolver = new LocalSignatureHelpResolver();

    @Test
    void unqualifiedProjectMethod_resolvesLabelParametersAndJavadoc() {
        String marked = source("int result = add(|1, 2);");

        Optional<SignatureHelpResult> result = resolver.resolve(text(marked), caret(marked));

        assertTrue(result.isPresent());
        SignatureInformation signature = result.get().signatures().getFirst();
        assertEquals("add(int, int)", signature.label());
        assertEquals(2, signature.parameters().size());
        assertEquals("int", signature.parameters().getFirst().label());
        assertTrue(signature.documentation().contains("Adds two values together."));
        assertNotNull(result.get().activeParameter());
        assertEquals(0, result.get().activeParameter());
    }

    @Test
    void activeParameter_countsTopLevelCommasOnly() {
        String marked = source("int result = add(1, |);");

        Optional<SignatureHelpResult> result = resolver.resolve(text(marked), caret(marked));

        assertTrue(result.isPresent());
        assertEquals(1, result.get().activeParameter());
        assertEquals(1, result.get().signatures().getFirst().activeParameter());
    }

    @Test
    void activeParameter_nestedCallCommasDoNotCount() {
        String marked = source("int result = add(add(1, 2), |);");

        Optional<SignatureHelpResult> result = resolver.resolve(text(marked), caret(marked));

        assertTrue(result.isPresent());
        assertEquals(1, result.get().activeParameter());
    }

    @Test
    void qualifiedJdkMethod_resolvesReceiverMembers() {
        String marked = source("text.substring(|0, 3);");

        Optional<SignatureHelpResult> result = resolver.resolve(text(marked), caret(marked));

        assertTrue(result.isPresent());
        SignatureInformation signature = result.get().signatures().getFirst();
        assertTrue(signature.label().contains("substring("));
        assertFalse(signature.parameters().isEmpty());
    }

    @Test
    void caretInsideStringArgument_stillResolvesEnclosingCall() {
        String marked = source("text.substring(\"(-)|\", 3);");

        Optional<SignatureHelpResult> result = resolver.resolve(text(marked), caret(marked));

        assertTrue(result.isPresent());
        assertTrue(result.get().signatures().getFirst().label().contains("substring("));
        assertEquals(0, result.get().activeParameter());
    }

    @Test
    void commentParenthesis_ignoredForCallDetection() {
        String marked = source("text.substring(0 /* ( */, |);");

        Optional<SignatureHelpResult> result = resolver.resolve(text(marked), caret(marked));

        assertTrue(result.isPresent());
        assertEquals(1, result.get().activeParameter());
    }

    @Test
    void ifCondition_isNotTreatedAsCall() {
        String marked = source("if (ready|) {");

        assertTrue(resolver.resolve(text(marked), caret(marked)).isEmpty());
    }

    @Test
    void caretOutsideCall_returnsEmpty() {
        String marked = source("int value = |1;");

        assertTrue(resolver.resolve(text(marked), caret(marked)).isEmpty());
    }

    @Test
    void unknownReceiverWithoutDatabaseEntry_returnsEmpty() {
        String marked = source("unknown.foobar(|);");

        assertTrue(resolver.resolve(text(marked), caret(marked)).isEmpty());
    }

    @Test
    void unqualifiedJdkName_fallsBackToCompletionDatabase() {
        String marked = source("Object result = valueOf(|);");

        Optional<SignatureHelpResult> result = resolver.resolve(text(marked), caret(marked));

        assertTrue(result.isPresent());
        SignatureInformation signature = result.get().signatures().getFirst();
        assertTrue(signature.label().contains("valueOf"));
        assertEquals(1, signature.parameters().size());
        assertEquals("Object obj", signature.parameters().getFirst().label());
        assertTrue(signature.documentation().contains("string representation"));
    }

    @Test
    void spaceBetweenNameAndParen_isSupported() {
        String marked = source("text.substring (0, |);");

        Optional<SignatureHelpResult> result = resolver.resolve(text(marked), caret(marked));

        assertTrue(result.isPresent());
        assertEquals(1, result.get().activeParameter());
    }

    @Test
    void emptyOrNullSource_returnsEmpty() {
        assertTrue(resolver.resolve(null, 0).isEmpty());
        assertTrue(resolver.resolve("", 4).isEmpty());
    }

    @Test
    void parameterLabelOffsets_pointIntoSignatureLabel() {
        String marked = source("Object result = valueOf(|);");
        SignatureInformation signature = resolver.resolve(text(marked), caret(marked))
                .map(result -> result.signatures().getFirst())
                .orElseThrow();
        SignatureParameter parameter = signature.parameters().getFirst();
        assertNotNull(parameter.labelStart());
        assertNotNull(parameter.labelEnd());
        assertEquals("Object obj", signature.label().substring(parameter.labelStart(), parameter.labelEnd()));
    }

    private static String source(String statement) {
        return "class Calculator {\n"
                + "    /** Adds two values together. */\n"
                + "    int add(int left, int right) {\n"
                + "        return left + right;\n"
                + "    }\n"
                + "\n"
                + "    void run() {\n"
                + "        String text = \"EyeCode\";\n"
                + "        " + statement + "\n"
                + "    }\n"
                + "}";
    }

    private static String text(String marked) {
        return marked.replace("|", "");
    }

    private static int caret(String marked) {
        return marked.indexOf('|');
    }
}
