package com.eyecode.challenge;

import java.util.List;

public record ChallengeTestRun(List<TestCase> tests, String message) {
    public ChallengeTestRun {
        tests = tests == null ? List.of() : List.copyOf(tests);
        message = message == null ? "" : message;
    }

    public record TestCase(String id, String name, String status, String errorMessage, String stackTrace) {
    }
}
