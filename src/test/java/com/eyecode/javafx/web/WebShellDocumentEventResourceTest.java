package com.eyecode.javafx.web;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WebShellDocumentEventResourceTest {

    @Test
    void metadataEventsConfirmSnapshotsWithoutReplacingExistingModelContent() throws IOException {
        String bundle = bundle();

        assertTrue(bundle.contains("delete de.content"));
        assertTrue(bundle.contains("N.apply(Ee,de.name===\"opened\"||de.name===\"externalChanged\")"));
        assertTrue(bundle.contains("apply(T,B=!1){const O=this.models.get(T.uri);return O?this.confirmSnapshot(T)?("));
        assertTrue(bundle.contains("B&&this.updateModel(O,T.content)&&this.scheduleDiagnostics(T.uri,O)"));
        assertTrue(bundle.contains("Oe.request(\"document\",\"change\",{uri:B,content:O,version:this.confirmedVersions.get(B)??0})"));
        assertTrue(bundle.contains("K.document&&this.apply(K.document)&&this.onDocumentChange?.(K.document)"));
    }

    @Test
    void reidentifiedDisposesOldModelAndOpensNewIdentityFromAuthoritativeSnapshot() throws IOException {
        String bundle = bundle();

        assertTrue(bundle.contains("reidentify(T,B){"));
        assertTrue(bundle.contains("O?.dispose(),this.models.delete(T)"));
        assertTrue(bundle.contains("P&&this.viewStates.set(B.uri,P)"));
        assertTrue(bundle.contains("K&&this.pendingReveals.set(B.uri,K),this.open(B)"));
        assertTrue(bundle.contains("this.api.editor.createModel(T.content,T.language||\"java\",this.api.Uri.parse(T.uri))"));
    }

    private String bundle() throws IOException {
        try (InputStream stream = getClass().getResourceAsStream("/webshell/assets/index-DeC9Qe_r.js")) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
