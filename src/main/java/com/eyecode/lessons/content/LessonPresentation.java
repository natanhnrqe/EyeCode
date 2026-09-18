package com.eyecode.lessons.content;

import com.eyecode.lessons.presentation.PresentationProgram;

import java.util.List;

public record LessonPresentation(String id, List<LessonEditorCommand> commands, LessonAnnotation annotation, String canonicalCode,
                                 LessonPresentationTransition transition, PresentationProgram program,
                                 PresentationProgram reverseProgram) {
    public LessonPresentation(String id, List<LessonEditorCommand> commands, LessonAnnotation annotation, String canonicalCode) {
        this(id, commands, annotation, canonicalCode, LessonPresentationTransition.AUTO, null, null);
    }

    public LessonPresentation(String id, List<LessonEditorCommand> commands, LessonAnnotation annotation) {
        this(id, commands, annotation, null);
    }

    public LessonPresentation {
        if (id == null || id.isBlank() || commands == null) throw new IllegalArgumentException("Apresentação de aula inválida");
        commands = List.copyOf(commands);
        transition = transition == null ? LessonPresentationTransition.AUTO : transition;
    }

    public LessonPresentation withProgram(PresentationProgram compiledProgram) {
        return new LessonPresentation(id, commands, annotation, canonicalCode, transition, compiledProgram, null);
    }

    public LessonPresentation withPrograms(PresentationProgram compiledProgram, PresentationProgram compiledReverseProgram) {
        return new LessonPresentation(id, commands, annotation, canonicalCode, transition, compiledProgram, compiledReverseProgram);
    }
}
