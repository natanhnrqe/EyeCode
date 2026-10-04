package com.eyecode.ui.web;

import com.eyecode.challenge.ChallengeWorkspaceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebShellChallengesControllerTest {
    @TempDir
    Path temporary;

    @Test
    void ensureStateAndResetFollowTheChallengesChannelContract() {
        Surface surface = new Surface();
        WebShellChallengesController controller = new WebShellChallengesController(surface,
                new ChallengeWorkspaceService(temporary));

        WebShellEnvelope ensured = surface.handler("challenges", "ensure").handle(request("one",
                Map.of("id", "cnpj-validator")));
        assertEquals("RESPONSE", ensured.kind().name());
        assertEquals("challenges", ensured.channel());
        assertEquals("ensure", ensured.name());
        assertEquals(temporary.resolve("cnpj-validator").toString(), ensured.payload().get("path"));
        assertEquals(Boolean.TRUE, ensured.payload().get("fresh"));
        assertTrue(String.valueOf(ensured.payload().get("mainFilePath")).endsWith("CnpjValidator.java"),
                () -> "mainFilePath=" + ensured.payload().get("mainFilePath"));

        WebShellEnvelope state = surface.handler("challenges", "state").handle(request("two",
                Map.of("id", "cnpj-validator")));
        assertEquals(Boolean.TRUE, state.payload().get("exists"));

        WebShellEnvelope reset = surface.handler("challenges", "reset").handle(request("three",
                Map.of("id", "cnpj-validator")));
        assertEquals(Boolean.TRUE, reset.payload().get("reset"));

        controller.dispose();
    }

    @Test
    void ensureRejectsInvalidOrMissingIds() {
        Surface surface = new Surface();
        WebShellChallengesController controller = new WebShellChallengesController(surface,
                new ChallengeWorkspaceService(temporary));

        WebShellEnvelope missing = surface.handler("challenges", "ensure").handle(request("one", Map.of()));
        assertEquals("INVALID_CHALLENGE_REQUEST", missing.error().code());
        assertTrue(missing.error().recoverable());

        WebShellEnvelope invalid = surface.handler("challenges", "ensure").handle(request("two",
                Map.of("id", "../escape")));
        assertEquals("INVALID_CHALLENGE_REQUEST", invalid.error().code());

        controller.dispose();
    }

    @Test
    void disposedControllerRejectsRequests() {
        Surface surface = new Surface();
        WebShellChallengesController controller = new WebShellChallengesController(surface,
                new ChallengeWorkspaceService(temporary));
        controller.dispose();

        WebShellEnvelope response = surface.handler("challenges", "state").handle(request("one",
                Map.of("id", "cnpj-validator")));
        assertNotNull(response.error());
        assertEquals("CHALLENGES_UNAVAILABLE", response.error().code());
    }

    private static WebShellEnvelope request(String requestId, Map<String, Object> payload) {
        return WebShellEnvelope.request("challenges", "ensure", requestId, payload);
    }

    private static final class Surface implements WebShellSurface {
        private final Map<String, WebShellMessageHandler> handlers = new ConcurrentHashMap<>();

        @Override
        public void registerHandler(String channel, String name, WebShellMessageHandler handler) {
            handlers.put(channel + "/" + name, handler);
        }

        @Override
        public void send(WebShellEnvelope message) {
        }

        private WebShellMessageHandler handler(String channel, String name) {
            return handlers.get(channel + "/" + name);
        }
    }
}
