package com.eyecode.runtime;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LessonCompileAnalyzerTest {

    private static LessonRunRequest request(String uri, String source) {
        return new LessonRunRequest(
                List.of(new LessonRunRequest.SourceFile("Main.java", source, uri)), "Main");
    }

    @Test
    void incompatibleTypesProducesStructuredDiagnosticWithRealLineAndUri() {
        String source = """
                public class Main {
                    public static void main(String[] args) {
                        int idade = "vinte";
                        System.out.println(idade);
                    }
                }
                """;
        List<LessonCompileAnalyzer.Entry> entries = new LessonCompileAnalyzer().analyze(request("lesson://variables/s1/main", source));

        assertEquals(1, entries.size());
        LessonCompileAnalyzer.Entry entry = entries.get(0);
        assertEquals("lesson://variables/s1/main", entry.uri());
        assertEquals("ERROR", entry.severity());
        assertEquals(3, entry.startLine());
        assertTrue(entry.message().contains("incompatible types"));
    }

    @Test
    void missingSemicolonKeepsRealCompilerMessage() {
        String source = """
                public class Main {
                    public static void main(String[] args) {
                        int total = 10
                        System.out.println(total);
                    }
                }
                """;
        List<LessonCompileAnalyzer.Entry> entries = new LessonCompileAnalyzer().analyze(request("lesson://variables/s1/main", source));

        assertTrue(entries.stream().anyMatch(entry -> entry.startLine() == 3
                && entry.message().contains("';' expected")));
    }

    @Test
    void validProgramProducesNoDiagnostics() {
        String source = """
                public class Main {
                    public static void main(String[] args) {
                        int total = 10;
                        System.out.println(total);
                    }
                }
                """;
        assertEquals(List.of(), new LessonCompileAnalyzer().analyze(request("lesson://variables/s1/main", source)));
    }

    @Test
    void blankUriEntryIsKeptOutByTheCallerContract() {
        String source = "public class Main { broken";
        List<LessonCompileAnalyzer.Entry> entries = new LessonCompileAnalyzer().analyze(request("", source));
        assertTrue(entries.stream().allMatch(entry -> entry.uri().isEmpty()));
    }
}
