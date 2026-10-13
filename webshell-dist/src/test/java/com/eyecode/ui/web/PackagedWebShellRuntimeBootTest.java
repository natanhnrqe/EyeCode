package com.eyecode.ui.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackagedWebShellRuntimeBootTest {

    @Test
    @Timeout(60)
    void packagedRuntimeStartsAndServesBundledWebShell() throws Exception {
        LocalWebShellRuntime runtime = new LocalWebShellRuntime(null);
        try {
            LocalWebShellSurface surface = runtime.surface();
            String entryUrl = surface.entryUrl();
            assertNotNull(entryUrl);
            assertTrue(entryUrl.startsWith("http://127.0.0.1:"), "entry URL must be loopback, got: " + entryUrl);
            assertTrue(entryUrl.endsWith("/webshell/index.html"), "entry URL must point at bundled index, got: " + entryUrl);
            assertFalse(surface.development(), "packaged runtime must not be in dev mode");

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            HttpResponse<String> index = client.send(
                    HttpRequest.newBuilder(URI.create(entryUrl)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, index.statusCode(), "bundled index.html should be served");
            assertTrue(index.body().contains("__EYECODE_LOCAL_TRANSPORT__"),
                    "index should carry the local transport bootstrap script");

            String base = entryUrl.substring(0, entryUrl.indexOf("/webshell/"));
            HttpResponse<String> monaco = client.send(
                    HttpRequest.newBuilder(URI.create(base + "/monaco/editor/vs/loader.js")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, monaco.statusCode(), "monaco loader should be served from the packaged jar");
            assertTrue(monaco.body().length() > 1000, "monaco loader should not be empty");

            String socketUrl = surface.entryUrl();
            assertFalse(socketUrl.isBlank());
        } finally {
            runtime.close();
        }
    }
}
