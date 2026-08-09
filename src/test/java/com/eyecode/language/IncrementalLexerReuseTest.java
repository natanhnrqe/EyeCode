package com.eyecode.language;

import com.eyecode.language.java.LexerSnapshot;
import com.eyecode.language.java.incremental.FullRelexStrategy;
import com.eyecode.language.java.incremental.IncrementalJavaLexer;
import com.eyecode.language.java.incremental.IncrementalLexResult;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Demonstrates that the incremental lexer reuses large parts of the previous
 * stream on big documents. Correctness is always asserted against a full
 * re-lex; timings are printed for information only (no strict time assertion —
 * they would be flaky on CI).
 */
class IncrementalLexerReuseTest {

    private static final int LINES = 5000;

    private final IncrementalJavaLexer incremental = new IncrementalJavaLexer();
    private final FullRelexStrategy full = new FullRelexStrategy();

    private static String bigDocument() {
        StringBuilder sb = new StringBuilder(LINES * 24);
        for (int i = 0; i < LINES; i++) {
            sb.append("int value").append(i).append(" = ").append(i).append(";\n");
        }
        return sb.toString();
    }

    private void assertReuse(String oldText, String newText, long version,
                             int maxRelexed, long minTotalReused, String scenario) {
        LexerSnapshot previous = full.lex(oldText, version);
        IncrementalLexResult result = incremental.lex(oldText, previous, newText, version + 1);

        assertEquals(full.lex(newText, version + 1), result.snapshot(), scenario + " mismatch");

        long incrementalNanos;
        long fullNanos;
        long start = System.nanoTime();
        incremental.lex(oldText, previous, newText, version + 1);
        incrementalNanos = System.nanoTime() - start;
        start = System.nanoTime();
        full.lex(newText, version + 1);
        fullNanos = System.nanoTime() - start;

        System.out.printf("[%s] relexed=%d prefixReused=%d tailReused=%d "
                        + "incremental=%.3fms full=%.3fms%n",
                scenario, result.relexedTokenCount(), result.prefixReusedTokenCount(),
                result.reuseWindow().reusedTokenCount(), incrementalNanos / 1_000_000.0,
                fullNanos / 1_000_000.0);

        assertTrue(result.relexedTokenCount() <= maxRelexed,
                scenario + " relexed too many tokens: " + result.relexedTokenCount());        assertTrue(result.totalReusedTokenCount() >= minTotalReused,
                scenario + " reused too few tokens: " + result.totalReusedTokenCount());
    }

    @Test
    void middleEditReusesPrefixAndTail() {
        String oldText = bigDocument();
        String newText = oldText.replace("int value2500 = 2500;", "int value2500 = 25000;");

        assertReuse(oldText, newText, 1, 200, 40_000, "middle-edit");
    }

    @Test
    void startEditReusesTail() {
        String oldText = bigDocument();
        String newText = "// head\n" + oldText;

        assertReuse(oldText, newText, 1, 200, 40_000, "start-edit");
    }

    @Test
    void endEditReusesPrefix() {
        String oldText = bigDocument();
        String newText = oldText + "// tail\n";

        assertReuse(oldText, newText, 1, 200, 40_000, "end-edit");
    }

    @Test
    void emptyChangeReusesEverything() {
        String oldText = bigDocument();
        LexerSnapshot previous = full.lex(oldText, 1);

        IncrementalLexResult result = incremental.lex(oldText, previous, oldText, 2);

        assertEquals(full.lex(oldText, 2), result.snapshot());
        assertTrue(result.prefixReusedTokenCount() > 40_000);
    }
}
