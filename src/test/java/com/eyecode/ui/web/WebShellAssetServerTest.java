package com.eyecode.ui.web;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebShellAssetServerTest {
    private static final Pattern ASSET = Pattern.compile("(?:src|href)=\"\\./(assets/[^\"]+)\"");
    private static final Pattern STYLE_ASSET = Pattern.compile("(?:\\./)?([A-Za-z0-9_-]+\\.css)");

    @Test
    void servesPackagedIndexAndItsGeneratedAssetsFromLoopback() throws Exception {
        try (WebShellAssetServer server = WebShellAssetServer.start()) {
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> index = client.send(HttpRequest.newBuilder(URI.create(server.entryUrl())).build(),
                    HttpResponse.BodyHandlers.ofString());

            assertEquals(200, index.statusCode());
            assertTrue(index.headers().firstValue("Content-Type").orElseThrow().startsWith("text/html"));
            Matcher assets = ASSET.matcher(index.body());
            boolean servedJavaScript = false;
            boolean servedStyleSheet = false;
            while (assets.find()) {
                String asset = assets.group(1);
                HttpResponse<String> response = client.send(HttpRequest.newBuilder(
                        URI.create(server.baseUrl() + "/webshell/" + asset)).build(),
                        HttpResponse.BodyHandlers.ofString());
                assertEquals(200, response.statusCode());
                assertTrue(response.body().length() > 0);
                if (asset.endsWith(".js")) {
                    servedJavaScript = response.headers().firstValue("Content-Type").orElseThrow()
                            .startsWith("text/javascript");
                    Matcher styles = STYLE_ASSET.matcher(response.body());
                    while (styles.find()) {
                        HttpResponse<String> style = client.send(HttpRequest.newBuilder(
                                URI.create(server.baseUrl() + "/webshell/assets/" + styles.group(1))).build(),
                                HttpResponse.BodyHandlers.ofString());
                        servedStyleSheet |= style.statusCode() == 200 && style.headers()
                                .firstValue("Content-Type").orElseThrow().startsWith("text/css");
                    }
                }
                if (asset.endsWith(".css")) {
                    servedStyleSheet = response.headers().firstValue("Content-Type").orElseThrow()
                            .startsWith("text/css");
                }
            }
            assertTrue(servedJavaScript);
            assertTrue(servedStyleSheet);
        }
    }

    @Test
    void rejectsPathsOutsideThePackagedWebShellRoot() throws Exception {
        try (WebShellAssetServer server = WebShellAssetServer.start()) {
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> traversal = client.send(HttpRequest.newBuilder(
                    URI.create(server.baseUrl() + "/webshell/%2e%2e/pom.xml")).build(),
                    HttpResponse.BodyHandlers.ofString());
            HttpResponse<String> outside = client.send(HttpRequest.newBuilder(
                    URI.create(server.baseUrl() + "/pom.xml")).build(), HttpResponse.BodyHandlers.ofString());

            assertEquals(404, traversal.statusCode());
            assertEquals(404, outside.statusCode());
        }
    }

    @Test
    void servesTheExistingMonacoLoaderFromItsFixedClasspathRoot() throws Exception {
        try (WebShellAssetServer server = WebShellAssetServer.start()) {
            HttpResponse<String> response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(
                    URI.create(server.baseUrl() + "/monaco/editor/vs/loader.js")).build(),
                    HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertTrue(response.headers().firstValue("Content-Type").orElseThrow().startsWith("text/javascript"));
            assertTrue(response.body().length() > 0);
        }
    }

    @Test
    void exposesDevelopmentBootstrapOnlyToTheConfiguredViteOrigin() throws Exception {
        try (WebShellAssetServer server = WebShellAssetServer.start(() -> "", () -> "{\"token\":\"secret\"}",
                Set.of("http://127.0.0.1:5173"))) {
            HttpClient client = HttpClient.newHttpClient();
            URI url = URI.create(server.baseUrl() + "/webshell/bootstrap");
            HttpResponse<String> accepted = client.send(HttpRequest.newBuilder(url)
                    .header("Origin", "http://127.0.0.1:5173").build(), HttpResponse.BodyHandlers.ofString());
            HttpResponse<String> rejected = client.send(HttpRequest.newBuilder(url)
                    .header("Origin", "http://example.invalid").build(), HttpResponse.BodyHandlers.ofString());

            assertEquals(200, accepted.statusCode());
            assertEquals("http://127.0.0.1:5173", accepted.headers()
                    .firstValue("Access-Control-Allow-Origin").orElseThrow());
            assertEquals("{\"token\":\"secret\"}", accepted.body());
            assertEquals(403, rejected.statusCode());
        }
    }
}
