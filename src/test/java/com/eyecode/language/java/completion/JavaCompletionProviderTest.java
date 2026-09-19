package com.eyecode.language.java.completion;

import com.eyecode.language.LanguageDocument;
import com.eyecode.language.completion.CompletionRequest;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaCompletionProviderTest {

    @Test
    void exposesJavaCompletionThroughTheNeutralCapability() {
        var result = new JavaCompletionProvider().complete(new CompletionRequest(
                new LanguageDocument("file:///Main.java", Path.of("Main.java"), "Main.java", null),
                1, "cla", 3, true, -1, -1));

        assertTrue(result.candidates().stream().anyMatch(candidate -> candidate.label().equals("class")));
    }
}
