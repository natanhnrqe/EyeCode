package com.eyecode.language;

import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.language.java.JavaLexerService;
import com.eyecode.language.java.LexerCache;
import com.eyecode.language.java.LexerCacheEntry;
import com.eyecode.language.java.LexerSnapshot;
import com.eyecode.language.java.incremental.FullRelexStrategy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Coarse performance sanity check (Sprint 5.2e). NOT a benchmark — the formal
 * benchmark lives in Sprint 5.8. Prints full vs incremental timings, token
 * counts and cache stats on a large document; the only strict assertions are
 * correctness (incremental must equal full re-lex) and sane cache usage.
 */
class LexerPerformanceSanityTest {

    private static final int LINES = 2000;

    private final FullRelexStrategy full = new FullRelexStrategy();

    private static String bigDocument() {
        StringBuilder sb = new StringBuilder(LINES * 60);
        sb.append("package com.example.perf;\n\n");
        sb.append("import java.util.List;\n");
        sb.append("import java.util.ArrayList;\n\n");
        sb.append("public final class PerfClass {\n");
        sb.append("    private final List<Integer> values = new ArrayList<>();\n\n");
        for (int i = 0; i < LINES; i++) {
            sb.append("    public int method").append(i)
                    .append("(int a, int b) { int sum = a + b + ").append(i)
                    .append("; return sum; }\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    @Test
    void sanityCheckLargeFileEdits() {
        String base = bigDocument();
        EditorDocument document = new EditorDocument(null, base);
        LexerCache cache = new LexerCache();
        JavaLexerService service = new JavaLexerService(cache);
        String sessionId = document.sessionId();

        long fullNanos = time(() -> full.lex(base, 1));
        long start = System.nanoTime();
        LexerSnapshot v1 = service.lex(document.snapshot());
        long firstNanos = System.nanoTime() - start;
        assertEquals(full.lex(base, 1), v1, "first lex must be a full-lex equivalent");
        System.out.printf("[perf] first-lex: text=%d chars tokens=%d service=%.3fms full=%.3fms%n",
                base.length(), v1.tokens().size(), firstNanos / 1_000_000.0,
                fullNanos / 1_000_000.0);

        String current = base + "// small tail edit\n";
        assertMutation(service, cache, document, current, "end-edit", sessionId);

        current = "// small head edit\n" + current;
        assertMutation(service, cache, document, current, "start-edit", sessionId);

        current = current.replace("public final class PerfClass {",
                "public final class PerfClass { // middle edit");
        assertMutation(service, cache, document, current, "middle-edit", sessionId);

        System.out.printf("[perf] cache: entries=%d misses=%d hits=%d evictions=%d%n",
                cache.size(), cache.stats().missCount(), cache.stats().hitCount(),
                cache.stats().evictionCount());
    }

    private void assertMutation(JavaLexerService service, LexerCache cache,
                                EditorDocument document, String newText,
                                String scenario, String sessionId) {
        document.setText(newText);
        long start = System.nanoTime();
        LexerSnapshot snapshot = service.lex(document.snapshot());
        long nanos = System.nanoTime() - start;

        assertEquals(full.lex(newText, snapshot.version()), snapshot,
                scenario + " must equal full re-lex");
        LexerCacheEntry entry = cache.get(sessionId, snapshot.version());
        assertNotNull(entry, scenario + " must leave a cache entry");
        assertEquals(snapshot.version(), entry.version());
        System.out.printf("[perf] %s: version=%d tokens=%d lex=%.3fms slot=%s%n",
                scenario, snapshot.version(), snapshot.tokens().size(),
                nanos / 1_000_000.0, entry.hasIncrementalSlot() ? "incremental" : "full");
    }

    private interface Timed {
        void run();
    }

    private static long time(Timed timed) {
        long start = System.nanoTime();
        timed.run();
        return System.nanoTime() - start;
    }
}
