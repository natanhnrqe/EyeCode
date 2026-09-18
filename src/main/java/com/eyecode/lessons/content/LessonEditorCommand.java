package com.eyecode.lessons.content;

public record LessonEditorCommand(LessonEditorCommandType type, LessonEditorRange range) {
    public LessonEditorCommand {
        if (type == null) throw new IllegalArgumentException("Tipo de comando ausente");
        if ((type == LessonEditorCommandType.HIGHLIGHT_RANGE || type == LessonEditorCommandType.REVEAL_RANGE)
                && range == null) throw new IllegalArgumentException("Comando de intervalo exige intervalo");
    }
}
