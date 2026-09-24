package com.eyecode.language.java.completion;

import com.eyecode.language.LanguageDocument;
import com.eyecode.language.completion.CompletionRequest;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaCompletionProviderTest {

    @Test
    void exposesJavaCompletionThroughTheNeutralCapability() {
        var result = new JavaCompletionProvider().complete(new CompletionRequest(
                new LanguageDocument("file:///Main.java", Path.of("Main.java"), "Main.java", null),
                1, "cla", 3, true, -1, -1));

        assertTrue(result.candidates().stream().anyMatch(candidate -> candidate.label().equals("class")));
    }

    @Test
    void automaticPrefixCompletionReturnsMatchingLocalVariable() {
        String source = "class Main { void run() { String nome = \"EyeCode\"; nom } }";
        var result = new JavaCompletionProvider().complete(request(source, "nom", false,
                CompletionRequest.TriggerKind.INVOKED, null));

        assertTrue(result.candidates().stream().anyMatch(candidate -> candidate.label().equals("nome")));
    }

    @Test
    void dotTriggerAllowsMemberCompletionWithEmptyPrefixAndPreservesMethodInsertion() {
        String source = "class Main { void run() { String text = \"x\"; text. } }";
        int caret = source.indexOf("text.") + "text.".length();
        var result = new JavaCompletionProvider().complete(new CompletionRequest(
                new LanguageDocument("file:///Main.java", Path.of("Main.java"), "Main.java", null),
                1, source, caret, false, -1, -1,
                CompletionRequest.TriggerKind.TRIGGER_CHARACTER, "."));

        assertTrue(result.candidates().stream().anyMatch(candidate -> candidate.label().equals("substring")
                && candidate.insertText().equals("substring()")));
    }

    @Test
    void arrGlobalRankingPlacesArraysAheadOfSamePrefixClassCandidates() {
        String source = "class Main { void run() { Arr } }";
        var result = new JavaCompletionProvider().complete(request(source, "Arr", false,
                CompletionRequest.TriggerKind.INVOKED, null));

        assertEquals("Arrays", result.candidates().getFirst().label());
        assertTrue(result.candidates().stream().limit(3)
                .anyMatch(candidate -> candidate.label().equals("ArrayList")));
    }

    private static CompletionRequest request(String source, String prefix, boolean explicit,
                                             CompletionRequest.TriggerKind triggerKind, String triggerCharacter) {
        int caret = source.lastIndexOf(prefix) + prefix.length();
        return new CompletionRequest(new LanguageDocument("file:///Main.java", Path.of("Main.java"), "Main.java", null),
                1, source, caret, explicit, -1, -1, triggerKind, triggerCharacter);
    }
}
