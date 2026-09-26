package com.eyecode.language.java.doc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavadocFormatterTest {

    @Test
    void nullOrBlank_returnsEmpty() {
        assertEquals("", JavadocFormatter.format(null));
        assertEquals("", JavadocFormatter.format("   "));
    }

    @Test
    void plainDescription_preservedAsSingleParagraph() {
        assertEquals("Adds two values together.", JavadocFormatter.format("Adds two values together."));
    }

    @Test
    void paramTag_rendersPortugueseSectionWithBullet() {
        String out = JavadocFormatter.format("Adds values.\n@param left first operand");

        assertTrue(out.startsWith("Adds values."));
        assertTrue(out.contains("## Parâmetros"));
        assertTrue(out.contains("- **left** — first operand"));
    }

    @Test
    void twoParamTags_shareASingleSectionHeading() {
        String out = JavadocFormatter.format("Adds values.\n@param left first\n@param right second");

        assertEquals(1, countOccurrences(out, "## Parâmetros"));
        assertTrue(out.contains("- **left** — first"));
        assertTrue(out.contains("- **right** — second"));
    }

    @Test
    void returnTag_rendersRetornoSection() {
        String out = JavadocFormatter.format("Adds values.\n@return the sum");

        assertTrue(out.contains("## Retorno"));
        assertTrue(out.contains("the sum"));
    }

    @Test
    void throwsAndExceptionTags_rendersExcecoesSection() {
        String out = JavadocFormatter.format("Validates.\n@throws IllegalArgumentException when invalid\n"
                + "@exception IllegalStateException when broken");

        assertEquals(1, countOccurrences(out, "## Exceções"));
        assertTrue(out.contains("when invalid"));
        assertTrue(out.contains("when broken"));
    }

    @Test
    void seeTag_plainName_rendersBacktickBullet() {
        String out = JavadocFormatter.format("Related.\n@see java.util.List");

        assertTrue(out.contains("## Ver também"));
        assertTrue(out.contains("- `java.util.List`"));
    }

    @Test
    void unknownTag_rendersAsLooseParagraphWithoutHeading() {
        String out = JavadocFormatter.format("Description.\n@custom something noted");

        assertTrue(out.contains("something noted"));
        assertFalse(out.contains("## "));
    }

    @Test
    void codeTag_rendersInlineCodeWithBackticks() {
        String out = JavadocFormatter.format("Use {@code a < b} here.");

        assertTrue(out.contains("`a < b`"));
        assertFalse(out.contains("{@code"));
    }

    @Test
    void linkTag_rendersLabelOnlyInBackticks() {
        String withLabel = JavadocFormatter.format("See {@link java.util.List#size() the size}.");
        String bare = JavadocFormatter.format("See {@link Foo}.");

        assertTrue(withLabel.contains("`the size`"));
        assertFalse(withLabel.contains("{@link"));
        assertTrue(bare.contains("`Foo`"));
    }

    @Test
    void htmlParagraphAndBold_convertedToMarkdown() {
        String out = JavadocFormatter.format("<p>One</p><p><b>Two</b></p>");

        assertTrue(out.contains("One\n\n**Two**"));
        assertFalse(out.contains("<p>"));
        assertFalse(out.contains("<b>"));
    }

    @Test
    void preBlock_rendersJavaFence() {
        String out = JavadocFormatter.format("Example:\n<pre>int x = 1;</pre>");

        assertTrue(out.contains("```java\nint x = 1;\n```"));
        assertFalse(out.contains("<pre>"));
    }

    @Test
    void rawStarJavadoc_isCleanedBeforeFormatting() {
        String out = JavadocFormatter.format("/**\n * Adds values.\n * @param x value\n */");

        assertTrue(out.contains("Adds values."));
        assertTrue(out.contains("## Parâmetros"));
        assertTrue(out.contains("- **x** — value"));
        assertFalse(out.contains("*/"));
    }

    @Test
    void angleBracketsOutsideTags_arePreserved() {
        String out = JavadocFormatter.format("True when a < b && c > d.");

        assertTrue(out.contains("a < b && c > d"));
    }

    @Test
    void paramContinuationLines_joinTheDescription() {
        String out = JavadocFormatter.format("Adds.\n@param x first line\n  second line");

        assertTrue(out.contains("first line"));
        assertTrue(out.contains("second line"));
        assertTrue(out.contains("- **x** — first line"));
    }

    @Test
    void descriptionComesBeforeSections() {
        String out = JavadocFormatter.format("Adds values.\n@return the sum");

        assertTrue(out.indexOf("Adds values.") < out.indexOf("## Retorno"));
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int index = text.indexOf(needle);
        while (index >= 0) {
            count++;
            index = text.indexOf(needle, index + needle.length());
        }
        return count;
    }
}
