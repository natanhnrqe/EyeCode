package com.eyecode.lessons.presentation;

public final class PresentationProgramExecutor {
    public String apply(PresentationProgram program, String source) {
        if (program == null || source == null || !source.equals(program.sourceCode())) {
            throw new IllegalArgumentException("Estado inicial incompatível com o programa");
        }
        StringBuilder state = new StringBuilder(source);
        for (PresentationOperation operation : program.operations()) {
            if (operation.startOffset() > state.length() || operation.endOffset() > state.length()) {
                throw new IllegalStateException("Operação fora dos limites do documento");
            }
            String replacement = operation.prefix() + operation.text() + operation.suffix();
            state.replace(operation.startOffset(), operation.endOffset(), replacement);
        }
        return state.toString();
    }
}
