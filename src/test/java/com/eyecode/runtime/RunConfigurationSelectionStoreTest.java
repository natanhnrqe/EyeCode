package com.eyecode.runtime;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RunConfigurationSelectionStoreTest {
    @Test
    void persistsSelectionsPerProjectInItsOwnStore() throws Exception {
        var store = new RunConfigurationSelectionStore(Files.createTempFile("eyecode-selection", ".properties"));
        var project = Files.createTempDirectory("eyecode-project");
        assertNull(store.selectedId(project));
        store.select(project, "java:demo.Main");
        assertEquals("java:demo.Main", store.selectedId(project));
    }
}
