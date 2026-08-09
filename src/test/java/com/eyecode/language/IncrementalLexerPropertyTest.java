package com.eyecode.language;

import com.eyecode.language.java.LexerSnapshot;
import com.eyecode.language.java.incremental.FullRelexStrategy;
import com.eyecode.language.java.incremental.IncrementalJavaLexer;
import com.eyecode.language.java.incremental.IncrementalLexResult;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Randomized equivalence property test: after every random mutation the
 * incremental result must be exactly equal (version and every token) to a full
 * re-lex of the same text.
 */
class IncrementalLexerPropertyTest {

    private static final String[] TEMPLATES = {
            "class Test { int x = 42; }",
            "String s = \"hello world\";",
            "char c = 'a';",
            "// line comment\nint x = 1;",
            "/* block\n * comment */\nint x = 1;",
            "/* unterminated block comment",
            "String s = \"unterminated;",
            "int a = 1;\r\nint b = 2;\r\n",
            "if (a >= 10 && b <= 3) { a++; } else { b--; }",
            "var x = \"caf\u00e9\"; // \u00fcber",
            "@Override\npublic void run() { boolean b = true; Object o = null; }",
            ""
    };

    private static final String EDIT_CHARS = "abcXYZ019_\"'\\/*\n\r \t(){}[]=+-<>&!;:.,@#$%^|\u00e9\u2028";

    private final IncrementalJavaLexer incremental = new IncrementalJavaLexer();
    private final FullRelexStrategy full = new FullRelexStrategy();

    @Test
    void singleEditsMatchFullRelexWithSeed42() {
        runSingleEdits(42);
    }

    @Test
    void singleEditsMatchFullRelexWithSeed7() {
        runSingleEdits(7);
    }

    @Test
    void chainedMutationsMatchFullRelex() {
        long[] seeds = {11, 42};
        for (long seed : seeds) {
            Random random = new Random(seed);
            String text = "class Test {\n"
                    + "    int a = 1;\n"
                    + "    String s = \"hi\";\n"
                    + "    // note\n"
                    + "}\n";
            long version = 0;
            LexerSnapshot previous = full.lex(text, version);
            for (int i = 0; i < 200; i++) {
                String mutated = mutate(random, text);
                version++;
                IncrementalLexResult result = incremental.lex(text, previous, mutated, version);
                assertEquals(full.lex(mutated, version), result.snapshot(),
                        "seed=" + seed + " edit#" + i);
                text = mutated;
                previous = result.snapshot();
            }
        }
    }

    private void runSingleEdits(long seed) {
        Random random = new Random(seed);
        for (String template : TEMPLATES) {
            String text = template;
            long version = 0;
            LexerSnapshot previous = full.lex(text, version);
            for (int i = 0; i < 100; i++) {
                String mutated = mutate(random, text);
                version++;
                IncrementalLexResult result = incremental.lex(text, previous, mutated, version);
                assertEquals(full.lex(mutated, version), result.snapshot(),
                        "seed=" + seed + " template=\"" + template + "\" edit#" + i);
                text = mutated;
                previous = result.snapshot();
            }
        }
    }

    private String mutate(Random random, String text) {
        int len = text.length();
        int position = len == 0 ? 0 : random.nextInt(len + 1);
        int operation = random.nextInt(3);
        return switch (operation) {
            case 0 -> text.substring(0, position)
                    + randomText(random, 1 + random.nextInt(20))
                    + text.substring(position);
            case 1 -> deleteRun(random, text, position, len);
            default -> deleteRun(random, text, position, len)
                    + randomText(random, 1 + random.nextInt(3));
        };
    }

    private String deleteRun(Random random, String text, int position, int len) {
        if (len == 0) {
            return text;
        }
        int end = Math.min(len, position + 1 + random.nextInt(4));
        return text.substring(0, position) + text.substring(end);
    }

    private String randomText(Random random, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(EDIT_CHARS.charAt(random.nextInt(EDIT_CHARS.length())));
        }
        return sb.toString();
    }
}
