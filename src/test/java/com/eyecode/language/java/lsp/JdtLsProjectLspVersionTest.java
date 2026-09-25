package com.eyecode.language.java.lsp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdtLsProjectLspVersionTest {
    @Test
    void documentVersionsMapToStrictlyIncreasingLspVersions() {
        assertTrue(JdtLsProjectCompletion.lspVersion(0) < JdtLsProjectCompletion.lspVersion(1));
        assertTrue(JdtLsProjectCompletion.lspVersion(1) < JdtLsProjectCompletion.lspVersion(2));
        assertTrue(JdtLsProjectCompletion.lspVersion(2) < JdtLsProjectCompletion.lspVersion(3));
    }

    @Test
    void firstDocumentVersionMapsToOne() {
        assertEquals(1, JdtLsProjectCompletion.lspVersion(0));
    }
}
