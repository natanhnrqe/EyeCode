package com.eyecode.diagnostics;

import com.sun.source.util.JavacTask;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public final class JavaSyntaxDiagnosticAnalyzer {

    private final Supplier<JavaCompiler> compilerSupplier;

    public JavaSyntaxDiagnosticAnalyzer() {
        this(ToolProvider::getSystemJavaCompiler);
    }

    JavaSyntaxDiagnosticAnalyzer(Supplier<JavaCompiler> compilerSupplier) {
        this.compilerSupplier = compilerSupplier == null ? () -> null : compilerSupplier;
    }

    public JavaDiagnosticsResult analyze(JavaDiagnosticRequest request) {
        if (request == null) {
            return new JavaDiagnosticsResult(new JavaDiagnosticRequest("", "", 0, ""), List.of(),
                    "A Java diagnostics request is required.");
        }
        JavaCompiler compiler = compilerSupplier.get();
        if (compiler == null) {
            return new JavaDiagnosticsResult(request, List.of(),
                    "Java compiler is unavailable. Run EyeCode with a full JDK.");
        }
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        try {
            JavaFileObject source = new InMemoryJavaSource(request.uri(), request.source());
            JavaCompiler.CompilationTask task = compiler.getTask(null, null, collector,
                    List.of("-proc:none"), null, List.of(source));
            if (!(task instanceof JavacTask javacTask)) {
                return new JavaDiagnosticsResult(request, List.of(), "Java compiler does not support parse diagnostics.");
            }
            javacTask.parse();
            List<JavaDiagnostic> diagnostics = new ArrayList<>();
            for (Diagnostic<? extends JavaFileObject> diagnostic : collector.getDiagnostics()) {
                if (diagnostic.getSource() != source) continue;
                diagnostics.add(toDiagnostic(request, diagnostic));
            }
            return new JavaDiagnosticsResult(request, diagnostics, "");
        } catch (IOException | RuntimeException exception) {
            String message = exception.getMessage();
            return new JavaDiagnosticsResult(request, List.of(),
                    message == null || message.isBlank() ? "Java syntax analysis failed." : message);
        }
    }

    private JavaDiagnostic toDiagnostic(JavaDiagnosticRequest request, Diagnostic<? extends JavaFileObject> diagnostic) {
        Range range = range(request.source(), diagnostic.getStartPosition(), diagnostic.getEndPosition());
        return new JavaDiagnostic(request.uri(), request.requestId(), request.modelVersion(), severity(diagnostic.getKind()),
                diagnostic.getCode(), category(request.source(), diagnostic, range), diagnostic.getMessage(Locale.ROOT), range.startLine(), range.startColumn(),
                range.endLine(), range.endColumn());
    }

    private static String category(String source, Diagnostic<? extends JavaFileObject> diagnostic, Range range) {
        if (!"compiler.err.expected".equals(diagnostic.getCode())) return "";
        int start = position(diagnostic.getStartPosition(), source == null ? 0 : source.length(), 0);
        return hasUnclosedParenthesis(source, start) ? "MISSING_CLOSING_PARENTHESIS" : "";
    }

    private static boolean hasUnclosedParenthesis(String source, int end) {
        String text = source == null ? "" : source;
        int depth = 0;
        boolean lineComment = false;
        boolean blockComment = false;
        boolean string = false;
        boolean character = false;
        boolean escaped = false;
        for (int index = 0; index < Math.min(end, text.length()); index++) {
            char current = text.charAt(index);
            char next = index + 1 < text.length() ? text.charAt(index + 1) : '\0';
            if (lineComment) {
                if (current == '\n') lineComment = false;
                continue;
            }
            if (blockComment) {
                if (current == '*' && next == '/') { blockComment = false; index++; }
                continue;
            }
            if (string || character) {
                if (escaped) { escaped = false; continue; }
                if (current == '\\') { escaped = true; continue; }
                if ((string && current == '"') || (character && current == '\'')) { string = false; character = false; }
                continue;
            }
            if (current == '/' && next == '/') { lineComment = true; index++; continue; }
            if (current == '/' && next == '*') { blockComment = true; index++; continue; }
            if (current == '"') { string = true; continue; }
            if (current == '\'') { character = true; continue; }
            if (current == '(') depth++;
            if (current == ')' && depth > 0) depth--;
        }
        return depth > 0;
    }

    static Range range(String source, long startPosition, long endPosition) {
        String text = source == null ? "" : source;
        int length = text.length();
        int start = position(startPosition, length, 0);
        int end = position(endPosition, length, Math.min(length, start + 1));
        if (end <= start) {
            end = start < length ? start + 1 : start;
        }
        Position startPoint = positionOf(text, start);
        Position endPoint = positionOf(text, end);
        if (end == start) {
            endPoint = new Position(startPoint.line(), startPoint.column() + 1);
        }
        return new Range(startPoint.line(), startPoint.column(), endPoint.line(), endPoint.column());
    }

    private static int position(long value, int length, int fallback) {
        if (value == Diagnostic.NOPOS || value < 0) {
            return fallback;
        }
        return (int) Math.max(0, Math.min(length, value));
    }

    private static Position positionOf(String source, int offset) {
        int line = 1;
        int column = 1;
        int bounded = Math.max(0, Math.min(source.length(), offset));
        for (int index = 0; index < bounded; index++) {
            if (source.charAt(index) == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }
        return new Position(line, column);
    }

    private static JavaDiagnosticSeverity severity(Diagnostic.Kind kind) {
        return switch (kind) {
            case ERROR -> JavaDiagnosticSeverity.ERROR;
            case WARNING, MANDATORY_WARNING -> JavaDiagnosticSeverity.WARNING;
            case NOTE -> JavaDiagnosticSeverity.INFO;
            default -> JavaDiagnosticSeverity.HINT;
        };
    }

    static record Range(int startLine, int startColumn, int endLine, int endColumn) { }
    private record Position(int line, int column) { }

    private static final class InMemoryJavaSource extends SimpleJavaFileObject {
        private final String source;

        private InMemoryJavaSource(String uri, String source) {
            super(sourceUri(uri), Kind.SOURCE);
            this.source = source == null ? "" : source;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }

        private static URI sourceUri(String uri) {
            try {
                URI parsed = URI.create(uri);
                if (parsed.getScheme() != null) {
                    return parsed;
                }
            } catch (IllegalArgumentException ignored) {
            }
            return URI.create("mem:///EyeCode.java");
        }
    }
}
