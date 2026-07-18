package com.eyecode.learning.markdown;

import com.eyecode.learning.content.LearningContentSection;
import com.eyecode.learning.content.LearningContentType;
import com.eyecode.learning.content.LearningPage;
import com.eyecode.learning.model.DifficultyLevel;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MarkdownParserTest {

    private final MarkdownParser parser = new MarkdownParser();

    @Nested
    class EmptyInput {

        @Test
        void nullInput() {
            MarkdownDocument doc = parser.parse(null);
            assertTrue(doc.nodes().isEmpty());
        }

        @Test
        void blankInput() {
            MarkdownDocument doc = parser.parse("   \n\n  ");
            assertTrue(doc.nodes().isEmpty());
        }

        @Test
        void emptyString() {
            MarkdownDocument doc = parser.parse("");
            assertTrue(doc.nodes().isEmpty());
        }
    }

    @Nested
    class Headings {

        @Test
        void h1() {
            MarkdownDocument doc = parser.parse("# Hello");
            assertEquals(1, doc.nodes().size());
            assertNodeType(doc.nodes().get(0), HeadingNode.class);
            HeadingNode h = (HeadingNode) doc.nodes().get(0);
            assertEquals(1, h.level());
            assertEquals("Hello", h.text());
        }

        @Test
        void h2() {
            MarkdownDocument doc = parser.parse("## Section");
            assertEquals(1, doc.nodes().size());
            HeadingNode h = (HeadingNode) doc.nodes().get(0);
            assertEquals(2, h.level());
            assertEquals("Section", h.text());
        }

        @Test
        void h1WithTrailingSpaces() {
            MarkdownDocument doc = parser.parse("#   Title with spaces   ");
            HeadingNode h = (HeadingNode) doc.nodes().get(0);
            assertEquals("Title with spaces", h.text());
        }

        @Test
        void h2WithUnicode() {
            MarkdownDocument doc = parser.parse("## \uD83D\uDCA1 Dica");
            HeadingNode h = (HeadingNode) doc.nodes().get(0);
            assertEquals("\uD83D\uDCA1 Dica", h.text());
        }

        @Test
        void h1FollowedByParagraph() {
            MarkdownDocument doc = parser.parse("# Title\n\nBody text.");
            assertEquals(2, doc.nodes().size());
            assertInstanceOf(HeadingNode.class, doc.nodes().get(0));
            assertInstanceOf(ParagraphNode.class, doc.nodes().get(1));
        }
    }

    @Nested
    class Paragraphs {

        @Test
        void simpleText() {
            MarkdownDocument doc = parser.parse("Hello world.");
            assertEquals(1, doc.nodes().size());
            ParagraphNode p = (ParagraphNode) doc.nodes().get(0);
            assertEquals(1, p.segments().size());
            assertEquals("Hello world.", p.segments().getFirst().text());
            assertEquals(Segment.Type.TEXT, p.segments().getFirst().type());
        }

        @Test
        void multiLineJoinsWithSpace() {
            MarkdownDocument doc = parser.parse("First line\nSecond line\nThird line.");
            ParagraphNode p = (ParagraphNode) doc.nodes().get(0);
            assertEquals("First line Second line Third line.", p.segments().getFirst().text());
        }

        @Test
        void multipleParagraphs() {
            MarkdownDocument doc = parser.parse("First paragraph.\n\nSecond paragraph.");
            assertEquals(2, doc.nodes().size());
            ParagraphNode p1 = (ParagraphNode) doc.nodes().get(0);
            ParagraphNode p2 = (ParagraphNode) doc.nodes().get(1);
            assertEquals("First paragraph.", p1.segments().getFirst().text());
            assertEquals("Second paragraph.", p2.segments().getFirst().text());
        }

        @Test
        void paragraphWithTrailingSpaces() {
            MarkdownDocument doc = parser.parse("  text with spaces   ");
            ParagraphNode p = (ParagraphNode) doc.nodes().get(0);
            assertEquals("text with spaces", p.segments().getFirst().text());
        }
    }

    @Nested
    class Dividers {

        @Test
        void standAlone() {
            MarkdownDocument doc = parser.parse("---");
            assertEquals(1, doc.nodes().size());
            assertInstanceOf(DividerNode.class, doc.nodes().get(0));
        }

        @Test
        void betweenParagraphs() {
            MarkdownDocument doc = parser.parse("Before\n\n---\n\nAfter");
            assertEquals(3, doc.nodes().size());
            assertInstanceOf(ParagraphNode.class, doc.nodes().get(0));
            assertInstanceOf(DividerNode.class, doc.nodes().get(1));
            assertInstanceOf(ParagraphNode.class, doc.nodes().get(2));
        }

        @Test
        void withSurroundingSpaces() {
            MarkdownDocument doc = parser.parse("  ---  ");
            assertInstanceOf(DividerNode.class, doc.nodes().get(0));
        }
    }

    @Nested
    class Bullets {

        @Test
        void single() {
            MarkdownDocument doc = parser.parse("- item");
            assertEquals(1, doc.nodes().size());
            BulletNode b = (BulletNode) doc.nodes().get(0);
            assertEquals("item", b.text());
            assertFalse(b.checked());
        }

        @Test
        void multiple() {
            MarkdownDocument doc = parser.parse("- a\n- b\n- c");
            assertEquals(3, doc.nodes().size());
            assertEquals("a", ((BulletNode) doc.nodes().get(0)).text());
            assertEquals("b", ((BulletNode) doc.nodes().get(1)).text());
            assertEquals("c", ((BulletNode) doc.nodes().get(2)).text());
        }

        @Test
        void withUnicodeMarker() {
            MarkdownDocument doc = parser.parse("\u2022 item");
            assertEquals(1, doc.nodes().size());
            assertEquals("item", ((BulletNode) doc.nodes().get(0)).text());
        }

        @Test
        void followedByParagraph() {
            MarkdownDocument doc = parser.parse("- bullet\n\nText");
            assertEquals(2, doc.nodes().size());
            assertInstanceOf(BulletNode.class, doc.nodes().get(0));
            assertInstanceOf(ParagraphNode.class, doc.nodes().get(1));
        }

        @Test
        void blankLineBreaksBulletRun() {
            MarkdownDocument doc = parser.parse("- a\n\n- b");
            assertEquals(2, doc.nodes().size());
            assertEquals("a", ((BulletNode) doc.nodes().get(0)).text());
            assertEquals("b", ((BulletNode) doc.nodes().get(1)).text());
        }

        @Test
        void indentedLineAfterBulletBecomesParagraph() {
            MarkdownDocument doc = parser.parse("- main\n  continuation");
            assertEquals(2, doc.nodes().size());
            assertInstanceOf(BulletNode.class, doc.nodes().get(0));
            assertInstanceOf(ParagraphNode.class, doc.nodes().get(1));
        }
    }

    @Nested
    class CodeBlocks {

        @Test
        void withLanguage() {
            MarkdownDocument doc = parser.parse("```java\nclass A {}\n```");
            assertEquals(1, doc.nodes().size());
            CodeBlockNode c = (CodeBlockNode) doc.nodes().get(0);
            assertEquals("java", c.language());
            assertEquals("class A {}", c.code());
        }

        @Test
        void withoutLanguage() {
            MarkdownDocument doc = parser.parse("```\nplain code\n```");
            CodeBlockNode c = (CodeBlockNode) doc.nodes().get(0);
            assertEquals("", c.language());
            assertEquals("plain code", c.code());
        }

        @Test
        void multiLineCode() {
            MarkdownDocument doc = parser.parse("```java\nline1\nline2\nline3\n```");
            CodeBlockNode c = (CodeBlockNode) doc.nodes().get(0);
            assertEquals("line1\nline2\nline3", c.code());
        }

        @Test
        void emptyCode() {
            MarkdownDocument doc = parser.parse("```java\n\n```");
            CodeBlockNode c = (CodeBlockNode) doc.nodes().get(0);
            assertEquals("", c.code());
        }

        @Test
        void withoutClosingFence() {
            MarkdownDocument doc = parser.parse("```java\nclass A {}");
            CodeBlockNode c = (CodeBlockNode) doc.nodes().get(0);
            assertEquals("java", c.language());
            assertEquals("class A {}", c.code());
        }

        @Test
        void followedByParagraph() {
            MarkdownDocument doc = parser.parse("```java\ncode\n```\n\ntext");
            assertEquals(2, doc.nodes().size());
            assertInstanceOf(CodeBlockNode.class, doc.nodes().get(0));
            assertInstanceOf(ParagraphNode.class, doc.nodes().get(1));
        }

        @Test
        void languageWithExtraSpaces() {
            MarkdownDocument doc = parser.parse("```   java   \ncode\n```");
            CodeBlockNode c = (CodeBlockNode) doc.nodes().get(0);
            assertEquals("java", c.language());
        }
    }

    @Nested
    class Callouts {

        @Test
        void infoCallout() {
            MarkdownDocument doc = parser.parse(":::info\nsome text\n:::");
            assertEquals(1, doc.nodes().size());
            CalloutNode c = (CalloutNode) doc.nodes().get(0);
            assertEquals("info", c.type());
            assertEquals("some text", c.text());
        }

        @Test
        void tipCallout() {
            MarkdownDocument doc = parser.parse(":::tip\ndica\n:::");
            CalloutNode c = (CalloutNode) doc.nodes().get(0);
            assertEquals("tip", c.type());
            assertEquals("dica", c.text());
        }

        @Test
        void warningCallout() {
            MarkdownDocument doc = parser.parse(":::warning\naten\u00E7\u00E3o\n:::");
            CalloutNode c = (CalloutNode) doc.nodes().get(0);
            assertEquals("warning", c.type());
            assertEquals("aten\u00E7\u00E3o", c.text());
        }

        @Test
        void multiLineContent() {
            MarkdownDocument doc = parser.parse(":::tip\nline one\nline two\n:::");
            CalloutNode c = (CalloutNode) doc.nodes().get(0);
            assertEquals("line one\nline two", c.text());
        }

        @Test
        void withoutClosingFence() {
            MarkdownDocument doc = parser.parse(":::info\ntext");
            CalloutNode c = (CalloutNode) doc.nodes().get(0);
            assertEquals("info", c.type());
            assertEquals("text", c.text());
        }

        @Test
        void multipleCallouts() {
            MarkdownDocument doc = parser.parse(":::tip\nfirst\n:::\n\n:::warning\nsecond\n:::");
            assertEquals(2, doc.nodes().size());
            assertEquals("tip", ((CalloutNode) doc.nodes().get(0)).type());
            assertEquals("warning", ((CalloutNode) doc.nodes().get(1)).type());
        }

        @Test
        void calloutWithSpecialCharsInText() {
            MarkdownDocument doc = parser.parse(":::info\n**bold** *italic* `code`\n:::");
            CalloutNode c = (CalloutNode) doc.nodes().get(0);
            assertTrue(c.text().contains("**bold**"));
        }
    }

    @Nested
    class InlineFormatting {

        @Test
        void bold() {
            MarkdownDocument doc = parser.parse("**bold text**");
            ParagraphNode p = (ParagraphNode) doc.nodes().get(0);
            assertEquals(1, p.segments().size());
            Segment s = p.segments().get(0);
            assertEquals(Segment.Type.BOLD, s.type());
            assertEquals("bold text", s.text());
        }

        @Test
        void italic() {
            MarkdownDocument doc = parser.parse("*italic text*");
            ParagraphNode p = (ParagraphNode) doc.nodes().get(0);
            assertEquals(1, p.segments().size());
            Segment s = p.segments().get(0);
            assertEquals(Segment.Type.ITALIC, s.type());
            assertEquals("italic text", s.text());
        }

        @Test
        void inlineCode() {
            MarkdownDocument doc = parser.parse("`code snippet`");
            ParagraphNode p = (ParagraphNode) doc.nodes().get(0);
            assertEquals(1, p.segments().size());
            Segment s = p.segments().get(0);
            assertEquals(Segment.Type.CODE, s.type());
            assertEquals("code snippet", s.text());
        }

        @Test
        void link() {
            MarkdownDocument doc = parser.parse("[click here](https://example.com)");
            ParagraphNode p = (ParagraphNode) doc.nodes().get(0);
            assertEquals(1, p.segments().size());
            Segment s = p.segments().get(0);
            assertEquals(Segment.Type.LINK, s.type());
            assertEquals("click here", s.text());
            assertEquals("https://example.com", s.url());
        }

        @Test
        void mixedInline() {
            MarkdownDocument doc = parser.parse("**bold** and *italic*");
            ParagraphNode p = (ParagraphNode) doc.nodes().get(0);
            assertEquals(3, p.segments().size());
            assertEquals(Segment.Type.BOLD, p.segments().get(0).type());
            assertEquals("bold", p.segments().get(0).text());
            assertEquals(Segment.Type.TEXT, p.segments().get(1).type());
            assertEquals(" and ", p.segments().get(1).text());
            assertEquals(Segment.Type.ITALIC, p.segments().get(2).type());
            assertEquals("italic", p.segments().get(2).text());
        }

        @Test
        void boldThenCodeThenLink() {
            MarkdownDocument doc = parser.parse("**bold** `code` [link](url)");
            ParagraphNode p = (ParagraphNode) doc.nodes().get(0);
            assertEquals(5, p.segments().size());
            assertEquals(Segment.Type.BOLD, p.segments().get(0).type());
            assertEquals(Segment.Type.TEXT, p.segments().get(1).type());
            assertEquals(Segment.Type.CODE, p.segments().get(2).type());
            assertEquals(Segment.Type.TEXT, p.segments().get(3).type());
            assertEquals(Segment.Type.LINK, p.segments().get(4).type());
        }

        @Test
        void boldContainsLink() {
            MarkdownDocument doc = parser.parse("**[link](url)**");
            ParagraphNode p = (ParagraphNode) doc.nodes().get(0);
            assertEquals(1, p.segments().size());
            assertEquals(Segment.Type.BOLD, p.segments().get(0).type());
        }

        @Test
        void textBeforeAndAfter() {
            MarkdownDocument doc = parser.parse("before **bold** after");
            ParagraphNode p = (ParagraphNode) doc.nodes().get(0);
            assertEquals(3, p.segments().size());
            assertEquals(Segment.Type.TEXT, p.segments().get(0).type());
            assertEquals("before ", p.segments().get(0).text());
            assertEquals(Segment.Type.BOLD, p.segments().get(1).type());
            assertEquals(Segment.Type.TEXT, p.segments().get(2).type());
            assertEquals(" after", p.segments().get(2).text());
        }
    }

    @Nested
    class Arrow {

        @Test
        void arrowOnItsOwn() {
            MarkdownDocument doc = parser.parse("\u2193");
            assertEquals(1, doc.nodes().size());
            ParagraphNode p = (ParagraphNode) doc.nodes().get(0);
            assertEquals("\u2193", p.segments().getFirst().text());
        }

        @Test
        void arrowBetweenParagraphs() {
            MarkdownDocument doc = parser.parse("Step 1\n\n\u2193\n\nStep 2");
            assertEquals(3, doc.nodes().size());
            assertInstanceOf(ParagraphNode.class, doc.nodes().get(0));
            assertInstanceOf(ParagraphNode.class, doc.nodes().get(1));
            assertInstanceOf(ParagraphNode.class, doc.nodes().get(2));
            ParagraphNode arrow = (ParagraphNode) doc.nodes().get(1);
            assertEquals("\u2193", arrow.segments().getFirst().text());
        }
    }

    @Nested
    class MixedDocument {

        @Test
        void headingParagraphDividerSequence() {
            String md = "# Title\n" +
                    "\n" +
                    "Paragraph.\n" +
                    "\n" +
                    "---\n" +
                    "\n" +
                    "- item one\n" +
                    "- item two\n" +
                    "\n" +
                    "```java\n" +
                    "class A {}\n" +
                    "```\n" +
                    "\n" +
                    ":::tip\n" +
                    "helpful tip\n" +
                    ":::";
            MarkdownDocument doc = parser.parse(md);
            assertEquals(7, doc.nodes().size());
            assertInstanceOf(HeadingNode.class, doc.nodes().get(0));
            assertInstanceOf(ParagraphNode.class, doc.nodes().get(1));
            assertInstanceOf(DividerNode.class, doc.nodes().get(2));
            assertInstanceOf(BulletNode.class, doc.nodes().get(3));
            assertInstanceOf(BulletNode.class, doc.nodes().get(4));
            assertInstanceOf(CodeBlockNode.class, doc.nodes().get(5));
            assertInstanceOf(CalloutNode.class, doc.nodes().get(6));

            HeadingNode h = (HeadingNode) doc.nodes().get(0);
            assertEquals(1, h.level());
            assertEquals("Title", h.text());
        }

        @Test
        void multipleSectionsWithDividers() {
            String md = "# Title\n" +
                    "\n" +
                    "First section.\n" +
                    "\n" +
                    "---\n" +
                    "\n" +
                    "## Second\n" +
                    "\n" +
                    "---\n" +
                    "\n" +
                    "## Third\n" +
                    "\n" +
                    "Last.";
            MarkdownDocument doc = parser.parse(md);
            assertEquals(7, doc.nodes().size());
            assertEquals(HeadingNode.class, doc.nodes().get(0).getClass());
            assertEquals(ParagraphNode.class, doc.nodes().get(1).getClass());
            assertEquals(DividerNode.class, doc.nodes().get(2).getClass());
            assertEquals(HeadingNode.class, doc.nodes().get(3).getClass());
            assertEquals(DividerNode.class, doc.nodes().get(4).getClass());
            assertEquals(HeadingNode.class, doc.nodes().get(5).getClass());
            assertEquals(ParagraphNode.class, doc.nodes().get(6).getClass());
        }
    }

    @Nested
    class Roundtrip {

        private final MarkdownBuilder builder = new MarkdownBuilder();

        private LearningPage createMinimalPage() {
            List<LearningContentSection> sections = new ArrayList<>();
            sections.add(section("intro", "Intro", LearningContentType.INTRODUCTION, 1,
                    "Um texto introdut\u00F3rio.\n\nCom duas senten\u00E7as."));
            sections.add(section("code", "Code", LearningContentType.CODE_EXAMPLE, 2,
                    "class A {\n    void run() {}\n}\n\nExplicando o c\u00F3digo."));
            sections.add(section("mistakes", "Erros", LearningContentType.COMMON_MISTAKES, 3,
                    "1. Erro um\n\nDetalhe do erro.\n\n2. Erro dois\n\nMais detalhe."));
            sections.add(section("tip", "Dica", LearningContentType.CURIOSITY, 4,
                    "Uma curiosidade interessante."));
            LearningPage page = new LearningPage();
            page.setId("test");
            page.setTitle("Test Page");
            page.setDifficulty(DifficultyLevel.BEGINNER);
            page.setSections(sections);
            return page;
        }

        @Test
        void fullPageRoundtrip() {
            LearningPage page = createMinimalPage();
            String markdown = builder.build(page);
            MarkdownDocument doc = parser.parse(markdown);

            assertNotNull(doc);
            assertFalse(doc.nodes().isEmpty());

            long headingCount = doc.nodes().stream()
                    .filter(n -> n instanceof HeadingNode).count();
            assertTrue(headingCount > 0, "Should have at least one heading");

            long paragraphCount = doc.nodes().stream()
                    .filter(n -> n instanceof ParagraphNode).count();
            assertTrue(paragraphCount > 0, "Should have at least one paragraph");

            long dividerCount = doc.nodes().stream()
                    .filter(n -> n instanceof DividerNode).count();
            assertTrue(dividerCount > 0, "Should have dividers between sections");

            long codeBlockCount = doc.nodes().stream()
                    .filter(n -> n instanceof CodeBlockNode).count();
            assertTrue(codeBlockCount > 0, "Should have a code block");

            long calloutCount = doc.nodes().stream()
                    .filter(n -> n instanceof CalloutNode).count();
            assertTrue(calloutCount > 0, "Should have callouts for mistakes and tips");
        }

        @Test
        void reparseIsDeterministic() {
            LearningPage page = createMinimalPage();
            String markdown = builder.build(page);
            MarkdownDocument first = parser.parse(markdown);
            MarkdownDocument second = parser.parse(markdown);

            assertEquals(first.nodes().size(), second.nodes().size());
            for (int i = 0; i < first.nodes().size(); i++) {
                assertEquals(
                        first.nodes().get(i).getClass(),
                        second.nodes().get(i).getClass(),
                        "Node type mismatch at index " + i);
            }
        }

        @Test
        void builderOutputParsesWithoutLoss() {
            LearningPage page = createMinimalPage();
            String markdown = builder.build(page);
            MarkdownDocument doc = parser.parse(markdown);

            int headings = 0, paragraphs = 0, dividers = 0,
                    bullets = 0, codeBlocks = 0, callouts = 0;
            for (MarkdownNode node : doc.nodes()) {
                switch (node) {
                    case HeadingNode h -> {
                        headings++;
                        assertFalse(h.text().isEmpty(), "Heading text should not be empty");
                    }
                    case ParagraphNode p -> {
                        paragraphs++;
                        assertFalse(p.segments().isEmpty(), "Paragraph should have segments");
                    }
                    case DividerNode d -> dividers++;
                    case BulletNode b -> bullets++;
                    case CodeBlockNode c -> {
                        codeBlocks++;
                        assertFalse(c.code().isEmpty(), "Code should not be empty");
                    }
                    case CalloutNode c -> {
                        callouts++;
                        assertFalse(c.type().isEmpty(), "Callout type should not be empty");
                    }
                }
            }
            assertTrue(headings > 0);
            assertTrue(paragraphs > 0);
            assertTrue(dividers > 0);
            assertTrue(codeBlocks > 0);
            assertTrue(callouts > 0);
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void codeBlockFenceInsideParagraphDetection() {
            String md = "Not a code block ```java";
            MarkdownDocument doc = parser.parse(md);
            assertEquals(1, doc.nodes().size());
            assertInstanceOf(ParagraphNode.class, doc.nodes().get(0));
        }

        @Test
        void dividerInsideParagraphNotDivider() {
            String md = "Text with --- inside.";
            MarkdownDocument doc = parser.parse(md);
            assertEquals(1, doc.nodes().size());
            assertInstanceOf(ParagraphNode.class, doc.nodes().get(0));
        }

        @Test
        void headingSymbolInsideParagraph() {
            String md = "Text with # symbol.";
            MarkdownDocument doc = parser.parse(md);
            assertEquals(1, doc.nodes().size());
            ParagraphNode p = (ParagraphNode) doc.nodes().get(0);
            assertEquals("Text with # symbol.", p.segments().getFirst().text());
        }

        @Test
        void codeBlockSymbolInsideParagraph() {
            String md = "Text with ``` symbols.";
            MarkdownDocument doc = parser.parse(md);
            ParagraphNode p = (ParagraphNode) doc.nodes().get(0);
            StringBuilder full = new StringBuilder();
            for (Segment s : p.segments()) {
                full.append(s.text());
            }
            assertTrue(full.toString().contains("```"));
        }

        @Test
        void calloutSymbolInsideParagraph() {
            String md = "Text with ::: symbols.";
            MarkdownDocument doc = parser.parse(md);
            ParagraphNode p = (ParagraphNode) doc.nodes().get(0);
            assertEquals("Text with ::: symbols.", p.segments().getFirst().text());
        }

        @Test
        void trailingSpacesInLines() {
            String md = "# Title  \n  \nParagraph.  ";
            MarkdownDocument doc = parser.parse(md);
            assertEquals(2, doc.nodes().size());
            assertEquals("Title", ((HeadingNode) doc.nodes().get(0)).text());
            assertEquals("Paragraph.", ((ParagraphNode) doc.nodes().get(1)).segments().getFirst().text());
        }
    }

    private static <T> void assertNodeType(MarkdownNode node, Class<T> expectedType) {
        assertInstanceOf(expectedType, node);
    }

    private static LearningContentSection section(String id, String title,
                                                  LearningContentType type, int order, String content) {
        LearningContentSection s = new LearningContentSection();
        s.setId(id);
        s.setTitle(title);
        s.setContent(content);
        s.setType(type);
        s.setOrder(order);
        return s;
    }
}
