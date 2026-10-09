package com.eyecode.ui.web;

import com.eyecode.challenge.ChallengeWorkspaceService;

import java.util.LinkedHashMap;
import java.util.Map;

public final class WebShellChallengesController {
    private final WebShellSurface surface;
    private final ChallengeWorkspaceService challenges;
    private volatile boolean disposed;

    WebShellChallengesController(WebShellSurface surface, ChallengeWorkspaceService challenges) {
        this.surface = surface;
        this.challenges = challenges;
        register("ensure", this::ensure);
        register("state", this::state);
        register("reset", this::reset);
        register("run", this::run);
    }

    public void dispose() {
        disposed = true;
    }

    private void register(String operation, WebShellMessageHandler handler) {
        surface.registerHandler("challenges", operation, message -> {
            if (disposed) {
                return message.error(new WebShellError("CHALLENGES_UNAVAILABLE",
                        "Challenges are no longer available", true));
            }
            return handler.handle(message);
        });
    }

    private WebShellEnvelope ensure(WebShellEnvelope message) {
        String id = text(message.payload(), "id");
        if (id.isBlank()) {
            return message.error(new WebShellError("INVALID_CHALLENGE_REQUEST",
                    "Challenges require an id", true));
        }
        try {
            ChallengeWorkspaceService.EnsureResult result = challenges.ensure(id);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("path", result.path().toString());
            response.put("mainFilePath", challenges.mainFilePath(id));
            response.put("fresh", result.fresh());
            return message.response(response);
        } catch (RuntimeException exception) {
            return message.error(new WebShellError("INVALID_CHALLENGE_REQUEST",
                    exception.getMessage() == null ? "Challenge workspace failed" : exception.getMessage(), true));
        }
    }

    private WebShellEnvelope state(WebShellEnvelope message) {
        String id = text(message.payload(), "id");
        if (id.isBlank()) {
            return message.error(new WebShellError("INVALID_CHALLENGE_REQUEST",
                    "Challenges require an id", true));
        }
        try {
            return message.response(Map.of("exists", challenges.exists(id)));
        } catch (RuntimeException exception) {
            return message.error(new WebShellError("INVALID_CHALLENGE_REQUEST",
                    exception.getMessage() == null ? "Challenge workspace failed" : exception.getMessage(), true));
        }
    }

    private WebShellEnvelope reset(WebShellEnvelope message) {
        String id = text(message.payload(), "id");
        if (id.isBlank()) {
            return message.error(new WebShellError("INVALID_CHALLENGE_REQUEST",
                    "Challenges require an id", true));
        }
        try {
            challenges.reset(id);
            return message.response(Map.of("reset", true));
        } catch (RuntimeException exception) {
            return message.error(new WebShellError("INVALID_CHALLENGE_REQUEST",
                    exception.getMessage() == null ? "Challenge workspace failed" : exception.getMessage(), true));
        }
    }

    private WebShellEnvelope run(WebShellEnvelope message) {
        String id = text(message.payload(), "id");
        if (id.isBlank()) {
            return message.error(new WebShellError("INVALID_CHALLENGE_REQUEST",
                    "Challenges require an id", true));
        }
        try {
            var result = challenges.runTests(id);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("message", result.message());
            response.put("tests", result.tests().stream().map(test -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", test.id());
                item.put("name", test.name());
                item.put("status", test.status());
                item.put("errorMessage", test.errorMessage());
                item.put("stackTrace", test.stackTrace());
                return item;
            }).toList());
            return message.response(response);
        } catch (RuntimeException exception) {
            return message.error(new WebShellError("CHALLENGE_TESTS_FAILED",
                    exception.getMessage() == null ? "Challenge tests failed" : exception.getMessage(), true));
        }
    }

    private static String text(Map<String, Object> payload, String key) {
        Object value = payload == null ? null : payload.get(key);
        return value == null ? "" : String.valueOf(value);
    }
}
