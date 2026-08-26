package com.eyecode.javafx.learning;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface MonacoLearningTargetResolver {
    CompletableFuture<Optional<MonacoLearningContent>> resolve(MonacoLearningTarget target);
}
