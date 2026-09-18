package com.eyecode.lessons.presentation;

import java.util.List;

public record PresentationProgram(String sourceCode, String targetCode, List<PresentationOperation> operations) {
    public PresentationProgram {
        if (sourceCode == null || targetCode == null || operations == null) {
            throw new IllegalArgumentException("Programa de apresentação inválido");
        }
        operations = List.copyOf(operations);
    }

    public boolean isAnimated() {
        return !operations.isEmpty() && operations.stream().noneMatch(operation -> operation.type() == PresentationOperationType.MATERIALIZE);
    }
}
