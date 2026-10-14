package com.eyecode.runtime;

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

/**
 * Structured diagnostics for a failed full compilation of a lesson program.
 *
 * <p>Used after a lesson run fails during compilation: instead of parsing the
 * raw {@code javac} output, the same real sources are compiled once more
 * through the system compiler with a {@link DiagnosticCollector}, which yields
 * the original compiler messages with exact positions. Messages are read with
 * {@link Locale#ROOT}, the same convention already used by the syntax
 * diagnostics pipeline. A successful compilation produces no entries.</p>
 */
public final class LessonCompileAnalyzer {

    public record Entry(String uri, String severity, String code, String message,
                        int startLine, int startColumn, int endLine, int endColumn) {
    }

    private final Supplier<JavaCompiler> compilerSupplier;

    public LessonCompileAnalyzer() {
        this(ToolProvider::getSystemJavaCompiler);
    }

    LessonCompileAnalyzer(Supplier<JavaCompiler> compilerSupplier) {
        this.compilerSupplier = compilerSupplier == null ? () -> null : compilerSupplier;
    }

    public List<Entry> analyze(LessonRunRequest request) {
        JavaCompiler compiler = compilerSupplier.get();
        if (compiler == null || request == null) {
            return List.of();
        }
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        List<JavaFileObject> sources = new ArrayList<>();
        for (LessonRunRequest.SourceFile file : request.files()) {
            if (file.name().endsWith(".java")) {
                sources.add(new InMemorySource(file.name(), file.source(), file.uri()));
            }
        }
        if (sources.isEmpty()) {
            return List.of();
        }
        JavaCompiler.CompilationTask task = compiler.getTask(null, null, collector,
                List.of("-proc:none"), null, sources);
        Boolean succeeded;
        try {
            succeeded = task.call();
        } catch (RuntimeException failure) {
            return List.of();
        }
        if (succeeded == Boolean.TRUE) {
            return List.of();
        }
        List<Entry> entries = new ArrayList<>();
        for (Diagnostic<? extends JavaFileObject> diagnostic : collector.getDiagnostics()) {
            if (diagnostic.getKind() != Diagnostic.Kind.ERROR && diagnostic.getKind() != Diagnostic.Kind.WARNING) {
                continue;
            }
            JavaFileObject source = diagnostic.getSource();
            if (!(source instanceof InMemorySource inMemory)) {
                continue;
            }
            Range range = range(inMemory.contents(), (int) diagnostic.getStartPosition(),
                    (int) diagnostic.getEndPosition());
            entries.add(new Entry(inMemory.uri(), diagnostic.getKind() == Diagnostic.Kind.ERROR ? "ERROR" : "WARNING",
                    diagnostic.getCode(), diagnostic.getMessage(Locale.ROOT),
                    range.startLine, range.startColumn, range.endLine, range.endColumn));
        }
        return entries;
    }

    private record Range(int startLine, int startColumn, int endLine, int endColumn) {
    }

    private static Range range(String source, int start, int end) {
        int[] startLineColumn = lineColumn(source, start);
        int[] endLineColumn = lineColumn(source, Math.max(start, end));
        return new Range(startLineColumn[0], startLineColumn[1], endLineColumn[0], endLineColumn[1]);
    }

    private static int[] lineColumn(String source, int position) {
        int line = 1;
        int column = 1;
        int limit = Math.min(Math.max(0, position), source == null ? 0 : source.length());
        for (int index = 0; index < limit; index++) {
            char current = source.charAt(index);
            if (current == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }
        return new int[]{Math.max(1, line), Math.max(1, column)};
    }

    private static final class InMemorySource extends SimpleJavaFileObject {
        private final String uri;
        private final String contents;

        private InMemorySource(String name, String contents, String uri) {
            super(URI.create("mem://lesson/" + name.replace('\\', '/')), Kind.SOURCE);
            this.uri = uri == null ? "" : uri;
            this.contents = contents == null ? "" : contents;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return contents;
        }

        private String uri() {
            return uri;
        }

        private String contents() {
            return contents;
        }
    }
}
