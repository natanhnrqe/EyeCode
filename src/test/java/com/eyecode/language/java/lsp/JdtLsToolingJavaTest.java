package com.eyecode.language.java.lsp;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdtLsToolingJavaTest {

    @Test
    void parsesModernJavaVersionOutput() {
        assertEquals(25, JdtLsToolingJava.parseMajorVersion("java version \"25.0.1\""));
        assertEquals(21, JdtLsToolingJava.parseMajorVersion("openjdk 21.0.8 2025-07-15"));
    }

    @Test
    void rejectsUnrecognizableJavaVersionOutput() {
        assertThrows(IllegalArgumentException.class, () -> JdtLsToolingJava.parseMajorVersion("unexpected"));
    }

    @Test
    void verifiesCurrentRuntimeAsCompatibleToolingJava() {
        JdtLsToolingJava toolingJava = JdtLsToolingJava.currentRuntime(Duration.ofSeconds(5));

        assertTrue(toolingJava.majorVersion() >= 21);
    }
}
