package com.eyecode.editor.v2.diagnostics;

import com.eyecode.editor.v2.EditorPosition;

import java.util.Objects;

public final class Diagnostic {

    private final DiagnosticSeverity severity;
    private final String code;
    private final String message;
    private final EditorPosition position;
    private final int length;

    public Diagnostic(DiagnosticSeverity severity, String code, String message,
                      EditorPosition position, int length) {
        this.severity = severity;
        this.code = code;
        this.message = message;
        this.position = position;
        this.length = length;
    }

    public DiagnosticSeverity getSeverity() { return severity; }

    public String getCode() { return code; }

    public String getMessage() { return message; }

    public EditorPosition getPosition() { return position; }

    public int getLength() { return length; }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Diagnostic that)) return false;
        return length == that.length
                && severity == that.severity
                && Objects.equals(code, that.code)
                && Objects.equals(message, that.message)
                && Objects.equals(position, that.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(severity, code, message, position, length);
    }

    @Override
    public String toString() {
        return "Diagnostic{"
                + "severity=" + severity
                + ", code='" + code + '\''
                + ", message='" + message + '\''
                + ", position=" + position
                + ", length=" + length
                + '}';
    }
}
