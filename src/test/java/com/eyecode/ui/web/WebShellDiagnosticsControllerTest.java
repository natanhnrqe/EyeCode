package com.eyecode.ui.web;

import com.eyecode.language.DocumentLanguageResolver;
import com.eyecode.language.ExtensionDocumentLanguageResolver;
import com.eyecode.language.LanguageId;
import com.eyecode.language.diagnostics.Diagnostic;
import com.eyecode.language.diagnostics.DiagnosticSeverity;
import com.eyecode.language.diagnostics.DiagnosticsProvider;
import com.eyecode.language.diagnostics.DiagnosticsRequest;
import com.eyecode.language.diagnostics.DiagnosticsResult;
import com.eyecode.language.diagnostics.DiagnosticsService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebShellDiagnosticsControllerTest {

    @Test
    void publishesOnlyTheLatestResultForADocument() throws InterruptedException {
        Surface surface = new Surface();
        BlockingProvider provider = new BlockingProvider();
        WebShellDiagnosticsController controller = new WebShellDiagnosticsController(surface,
                new DiagnosticsService(resolver(), List.of(provider)));

        surface.handler("diagnostics", "request").handle(request("one", "first", 1));
        assertTrue(provider.firstStarted.await(2, TimeUnit.SECONDS));
        surface.handler("diagnostics", "request").handle(request("two", "second", 2));
        provider.releaseFirst.countDown();

        assertTrue(surface.secondPublished.await(2, TimeUnit.SECONDS));
        controller.dispose();

        List<WebShellEnvelope> publishes = surface.sent.stream()
                .filter(message -> message.channel().equals("diagnostics") && message.name().equals("publish"))
                .toList();
        assertEquals(1, publishes.size());
        assertEquals("two", publishes.getFirst().payload().get("requestId"));
        assertEquals(2L, publishes.getFirst().payload().get("modelVersion"));
    }

    private static DocumentLanguageResolver resolver() {
        return new ExtensionDocumentLanguageResolver(Map.of(LanguageId.JAVA, Set.of("java")));
    }

    private static WebShellEnvelope request(String requestId, String content, long version) {
        return WebShellEnvelope.request("diagnostics", "request", requestId, Map.of(
                "uri", "lesson://language/stale/main",
                "language", "java",
                "displayName", "main",
                "content", content,
                "modelVersion", version));
    }

    private static final class BlockingProvider implements DiagnosticsProvider {
        private final CountDownLatch firstStarted = new CountDownLatch(1);
        private final CountDownLatch releaseFirst = new CountDownLatch(1);

        @Override public LanguageId languageId() { return LanguageId.JAVA; }

        @Override
        public DiagnosticsResult analyze(DiagnosticsRequest request) {
            if (request.source().equals("first")) {
                firstStarted.countDown();
                try {
                    releaseFirst.await(2, TimeUnit.SECONDS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return new DiagnosticsResult(List.of(), "interrupted");
                }
            }
            return new DiagnosticsResult(List.of(new Diagnostic(DiagnosticSeverity.INFO, request.source(), "ok",
                    1, 1, 1, 2)), "");
        }
    }

    private static final class Surface implements WebShellSurface {
        private final Map<String, WebShellMessageHandler> handlers = new java.util.concurrent.ConcurrentHashMap<>();
        private final List<WebShellEnvelope> sent = java.util.Collections.synchronizedList(new ArrayList<>());
        private final CountDownLatch secondPublished = new CountDownLatch(1);

        @Override
        public void send(WebShellEnvelope message) {
            sent.add(message);
            if (message.channel().equals("diagnostics") && message.name().equals("publish")
                    && "two".equals(message.payload().get("requestId"))) {
                secondPublished.countDown();
            }
        }

        @Override
        public void registerHandler(String channel, String name, WebShellMessageHandler handler) {
            handlers.put(channel + '/' + name, handler);
        }

        private WebShellMessageHandler handler(String channel, String name) {
            return handlers.get(channel + '/' + name);
        }
    }
}
