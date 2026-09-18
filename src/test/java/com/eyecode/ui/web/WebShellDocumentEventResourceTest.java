package com.eyecode.ui.web;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WebShellDocumentEventResourceTest {

    @Test
    void metadataEventsConfirmSnapshotsWithoutReplacingExistingModelContent() throws IOException {
        String bundle = bundle();

        assertTrue(bundle.contains("this.confirmedVersions.get("));
        assertTrue(bundle.contains("this.changeQueues.get("));
        assertTrue(bundle.contains("request(\"document\",\"change\","));
        assertTrue(bundle.contains("this.onDocumentChange?.("));
    }

    @Test
    void reidentifiedDisposesOldModelAndOpensNewIdentityFromAuthoritativeSnapshot() throws IOException {
        String bundle = bundle();

        assertTrue(bundle.contains("reidentify("));
        assertTrue(bundle.contains("this.models.delete("));
        assertTrue(bundle.contains("this.viewStates.set("));
        assertTrue(bundle.contains("this.open("));
        assertTrue(bundle.contains("this.api.editor.createModel("));
    }

    @Test
    void packagedBundleContainsPracticeVerification() throws IOException {
        String bundle = bundle();

        assertTrue(bundle.contains("session/verify"));
        assertTrue(bundle.contains("Verificar"));
        assertTrue(bundle.contains("Verificando..."));
    }

    private String bundle() throws IOException {
        try (var files = Files.list(Path.of("src/main/resources/webshell/assets"))) {
            return files.filter(path -> path.getFileName().toString().endsWith(".js"))
                    .map(path -> {
                        try {
                            return Files.readString(path);
                        } catch (IOException exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .collect(java.util.stream.Collectors.joining("\n"));
        }
    }
}
