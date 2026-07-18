package com.eyecode.learning.markdown;

import com.eyecode.ui.designsystem.ColorManager;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.swing.text.BadLocationException;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.Color;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MarkdownRendererTest {

    private final MarkdownRenderer renderer = new MarkdownRenderer();

    @Test
    void nullDocument() {
        StyledDocument doc = renderer.render(null);
        assertEquals(0, doc.getLength());
    }

    @Test
    void emptyDocument() {
        MarkdownDocument md = new MarkdownDocument(List.of());
        StyledDocument doc = renderer.render(md);
        assertEquals(0, doc.getLength());
    }

    @Nested
    class Headings {

        @Test
        void h1() throws BadLocationException {
            var md = new MarkdownDocument(List.of(new HeadingNode(1, "Title")));
            StyledDocument doc = renderer.render(md);
            String text = doc.getText(0, doc.getLength());
            assertTrue(text.contains("Title"), "Should contain heading text");

            var attrs = doc.getCharacterElement(0).getAttributes();
            assertEquals(24, StyleConstants.getFontSize(attrs));
            assertTrue(StyleConstants.isBold(attrs));
            assertEquals(ColorManager.TEXT_PRIMARY, StyleConstants.getForeground(attrs));
        }

        @Test
        void h2DefaultColor() throws BadLocationException {
            var md = new MarkdownDocument(List.of(new HeadingNode(2, "Section")));
            StyledDocument doc = renderer.render(md);
            var attrs = doc.getCharacterElement(0).getAttributes();
            assertEquals(16, StyleConstants.getFontSize(attrs));
            assertTrue(StyleConstants.isBold(attrs));
            assertEquals(ColorManager.TEXT_PRIMARY, StyleConstants.getForeground(attrs));
        }

        @Test
        void h2WithEmojiColor() throws BadLocationException {
            var md = new MarkdownDocument(List.of(
                    new HeadingNode(2, "\uD83D\uDCA1 Dica")));
            StyledDocument doc = renderer.render(md);
            var attrs = doc.getCharacterElement(0).getAttributes();
            assertEquals(ColorManager.ACCENT_BLUE_LIGHT, StyleConstants.getForeground(attrs));
        }

        @Test
        void h2BackgroundApplied() throws BadLocationException {
            var md = new MarkdownDocument(List.of(new HeadingNode(2, "Section")));
            StyledDocument doc = renderer.render(md);
            var paraAttrs = doc.getParagraphElement(0).getAttributes();
            assertNotNull(StyleConstants.getBackground(paraAttrs));
        }

        @Test
        void h1HasNoBackground() throws BadLocationException {
            var md = new MarkdownDocument(List.of(new HeadingNode(1, "Title")));
            StyledDocument doc = renderer.render(md);
            var paraAttrs = doc.getParagraphElement(0).getAttributes();
            Color bg = StyleConstants.getBackground(paraAttrs);
            assertNotEquals(new Color(0x1A, 0x73, 0xE8, 0x0C), bg, "H1 should not have H2 background");
        }
    }

    @Nested
    class Paragraphs {

        @Test
        void bodyStyle() throws BadLocationException {
            var md = new MarkdownDocument(List.of(
                    new ParagraphNode(List.of(Segment.text("Hello.")))));
            StyledDocument doc = renderer.render(md);
            var attrs = doc.getCharacterElement(0).getAttributes();
            assertEquals(13, StyleConstants.getFontSize(attrs));
            assertEquals(ColorManager.TEXT_SECONDARY, StyleConstants.getForeground(attrs));
        }

        @Test
        void preservesTextContent() throws BadLocationException {
            var md = new MarkdownDocument(List.of(
                    new ParagraphNode(List.of(Segment.text("First paragraph."))),
                    new ParagraphNode(List.of(Segment.text("Second paragraph.")))));
            StyledDocument doc = renderer.render(md);
            String text = doc.getText(0, doc.getLength());
            assertTrue(text.contains("First paragraph."));
            assertTrue(text.contains("Second paragraph."));
            int firstPos = text.indexOf("First paragraph.");
            int secondPos = text.indexOf("Second paragraph.");
            assertTrue(firstPos < secondPos, "First paragraph should come before second");
        }

        @Test
        void boldStyle() throws BadLocationException {
            var md = new MarkdownDocument(List.of(
                    new ParagraphNode(List.of(Segment.bold("bold")))));
            StyledDocument doc = renderer.render(md);
            var attrs = doc.getCharacterElement(0).getAttributes();
            assertTrue(StyleConstants.isBold(attrs));
            assertEquals(ColorManager.TEXT_SECONDARY, StyleConstants.getForeground(attrs));
            assertEquals(13, StyleConstants.getFontSize(attrs));
        }

        @Test
        void italicStyle() throws BadLocationException {
            var md = new MarkdownDocument(List.of(
                    new ParagraphNode(List.of(Segment.italic("italic")))));
            StyledDocument doc = renderer.render(md);
            var attrs = doc.getCharacterElement(0).getAttributes();
            assertFalse(StyleConstants.isBold(attrs));
            assertEquals(13, StyleConstants.getFontSize(attrs));
        }

        @Test
        void inlineCodeStyle() throws BadLocationException {
            var md = new MarkdownDocument(List.of(
                    new ParagraphNode(List.of(Segment.code("code()")))));
            StyledDocument doc = renderer.render(md);
            var attrs = doc.getCharacterElement(0).getAttributes();
            assertEquals(ColorManager.SYNTAX_KEYWORD, StyleConstants.getForeground(attrs));
            assertEquals(ColorManager.PANEL_BG, StyleConstants.getBackground(attrs));
            assertEquals(13, StyleConstants.getFontSize(attrs));
        }

        @Test
        void linkStyle() throws BadLocationException {
            var md = new MarkdownDocument(List.of(
                    new ParagraphNode(List.of(Segment.link("click", "url")))));
            StyledDocument doc = renderer.render(md);
            var attrs = doc.getCharacterElement(0).getAttributes();
            assertEquals(ColorManager.ACCENT_BLUE_LIGHT, StyleConstants.getForeground(attrs));
            assertTrue(StyleConstants.isUnderline(attrs));
            assertEquals(13, StyleConstants.getFontSize(attrs));
        }

        @Test
        void mixedSegmentsPreserveOrder() throws BadLocationException {
            var md = new MarkdownDocument(List.of(
                    new ParagraphNode(List.of(
                            Segment.text("before "),
                            Segment.bold("bold"),
                            Segment.text(" between "),
                            Segment.italic("italic"),
                            Segment.text(" after")))));
            StyledDocument doc = renderer.render(md);
            String text = doc.getText(0, doc.getLength());
            int boldPos = text.indexOf("bold");
            int italicPos = text.indexOf("italic");
            assertTrue(boldPos < italicPos, "bold should appear before italic");
        }
    }

    @Nested
    class Bullets {

        @Test
        void uncheckedBullet() throws BadLocationException {
            var md = new MarkdownDocument(List.of(
                    new BulletNode("item", false)));
            StyledDocument doc = renderer.render(md);
            String text = doc.getText(0, doc.getLength());
            assertTrue(text.startsWith("\u2022 "), "Should start with bullet marker");
            assertTrue(text.contains("item"), "Should contain bullet text");

            var attrs = doc.getCharacterElement(0).getAttributes();
            assertEquals(ColorManager.TEXT_PRIMARY, StyleConstants.getForeground(attrs));
            assertEquals(13, StyleConstants.getFontSize(attrs));
        }

        @Test
        void checkedBulletUsesCheckmark() throws BadLocationException {
            var md = new MarkdownDocument(List.of(
                    new BulletNode("done", true)));
            StyledDocument doc = renderer.render(md);
            String text = doc.getText(0, doc.getLength());
            assertTrue(text.startsWith("\u2611 "), "Checked bullet should use checkmark");
        }
    }

    @Nested
    class CodeBlocks {

        @Test
        void codeContentRendered() throws BadLocationException {
            var md = new MarkdownDocument(List.of(
                    new CodeBlockNode("java", "class A {}")));
            StyledDocument doc = renderer.render(md);
            String text = doc.getText(0, doc.getLength());
            assertTrue(text.contains("class A {}"), "Should contain code content");
        }

        @Test
        void emptyCodeNotRendered() throws BadLocationException {
            var md = new MarkdownDocument(List.of(
                    new CodeBlockNode("java", "")));
            StyledDocument doc = renderer.render(md);
            assertEquals(0, doc.getLength(), "Empty code should produce no output");
        }

        @Test
        void codeBlockStyle() throws BadLocationException {
            var md = new MarkdownDocument(List.of(
                    new CodeBlockNode("java", "code")));
            StyledDocument doc = renderer.render(md);
            int codePos = doc.getText(0, doc.getLength()).indexOf("code");
            assertTrue(codePos >= 0);
            var attrs = doc.getCharacterElement(codePos).getAttributes();
            assertEquals(ColorManager.EDITOR_FOREGROUND, StyleConstants.getForeground(attrs));
            assertEquals(13, StyleConstants.getFontSize(attrs));
        }

        @Test
        void codeParagraphBackground() throws BadLocationException {
            var md = new MarkdownDocument(List.of(
                    new CodeBlockNode("java", "line1\nline2")));
            StyledDocument doc = renderer.render(md);
            String text = doc.getText(0, doc.getLength());
            int firstLinePos = text.indexOf("line1");
            var firstParaAttrs = doc.getParagraphElement(firstLinePos).getAttributes();
            assertEquals(ColorManager.EDITOR_BG, StyleConstants.getBackground(firstParaAttrs));
            assertEquals(28.0f, StyleConstants.getLeftIndent(firstParaAttrs), 0.01f);
            assertEquals(28.0f, StyleConstants.getRightIndent(firstParaAttrs), 0.01f);
        }

        @Test
        void firstLineHasTopPadding() throws BadLocationException {
            var md = new MarkdownDocument(List.of(
                    new CodeBlockNode("java", "line1")));
            StyledDocument doc = renderer.render(md);
            String text = doc.getText(0, doc.getLength());
            int pos = text.indexOf("line1");
            var paraAttrs = doc.getParagraphElement(pos).getAttributes();
            assertEquals(10.0f, StyleConstants.getSpaceAbove(paraAttrs), 0.01f);
            assertEquals(10.0f, StyleConstants.getSpaceBelow(paraAttrs), 0.01f);
        }

        @Test
        void middleLinesNoPadding() throws BadLocationException {
            var md = new MarkdownDocument(List.of(
                    new CodeBlockNode("java", "line1\nline2\nline3")));
            StyledDocument doc = renderer.render(md);
            String text = doc.getText(0, doc.getLength());
            int midPos = text.indexOf("line2");
            var midAttrs = doc.getParagraphElement(midPos).getAttributes();
            assertEquals(0.0f, StyleConstants.getSpaceAbove(midAttrs), 0.01f);
            assertEquals(0.0f, StyleConstants.getSpaceBelow(midAttrs), 0.01f);
        }

        @Test
        void lastLineHasBottomPadding() throws BadLocationException {
            var md = new MarkdownDocument(List.of(
                    new CodeBlockNode("java", "line1\nline2")));
            StyledDocument doc = renderer.render(md);
            String text = doc.getText(0, doc.getLength());
            int lastPos = text.indexOf("line2");
            var lastAttrs = doc.getParagraphElement(lastPos).getAttributes();
            assertEquals(0.0f, StyleConstants.getSpaceAbove(lastAttrs), 0.01f);
            assertEquals(10.0f, StyleConstants.getSpaceBelow(lastAttrs), 0.01f);
        }

        @Test
        void javaSyntaxHighlighting() throws BadLocationException {
            var md = new MarkdownDocument(List.of(
                    new CodeBlockNode("java", "class Pessoa { }")));
            StyledDocument doc = renderer.render(md);
            String text = doc.getText(0, doc.getLength());
            int classPos = text.indexOf("class");
            assertTrue(classPos >= 0);
            var attrs = doc.getCharacterElement(classPos).getAttributes();
            assertEquals(ColorManager.SYNTAX_KEYWORD, StyleConstants.getForeground(attrs));
            assertEquals(13, StyleConstants.getFontSize(attrs),
                    "Keyword must share same font size as base code text");
        }

        @Test
        void nonJavaLanguageNoHighlight() throws BadLocationException {
            var md = new MarkdownDocument(List.of(
                    new CodeBlockNode("text", "class ignored")));
            StyledDocument doc = renderer.render(md);
            String text = doc.getText(0, doc.getLength());
            int pos = text.indexOf("class");
            var attrs = doc.getCharacterElement(pos).getAttributes();
            assertEquals(ColorManager.EDITOR_FOREGROUND, StyleConstants.getForeground(attrs));
        }
    }

    @Nested
    class Dividers {

        @Test
        void dividerTextAndStyle() throws BadLocationException {
            var md = new MarkdownDocument(List.of(new DividerNode()));
            StyledDocument doc = renderer.render(md);
            String text = doc.getText(0, doc.getLength());
            assertTrue(text.contains("\u2500"), "Divider should contain line characters");

            var attrs = doc.getCharacterElement(0).getAttributes();
            assertEquals(ColorManager.BORDER_CARD, StyleConstants.getForeground(attrs));
        }
    }

    @Nested
    class Callouts {

        @Test
        void infoCallout() throws BadLocationException {
            var md = new MarkdownDocument(List.of(
                    new CalloutNode("info", "some info")));
            StyledDocument doc = renderer.render(md);
            String text = doc.getText(0, doc.getLength());
            assertTrue(text.contains("\uD83D\uDCD8"), "Should contain info emoji");
            assertTrue(text.contains("some info"), "Should contain callout text");
        }

        @Test
        void tipCallout() throws BadLocationException {
            var md = new MarkdownDocument(List.of(
                    new CalloutNode("tip", "a tip")));
            StyledDocument doc = renderer.render(md);
            String text = doc.getText(0, doc.getLength());
            assertTrue(text.contains("\uD83D\uDCA1"), "Should contain tip emoji");
            assertTrue(text.contains("a tip"));
        }

        @Test
        void warningCallout() throws BadLocationException {
            var md = new MarkdownDocument(List.of(
                    new CalloutNode("warning", "caution")));
            StyledDocument doc = renderer.render(md);
            String text = doc.getText(0, doc.getLength());
            assertTrue(text.contains("\u26A0\uFE0F"), "Should contain warning emoji");
            assertTrue(text.contains("caution"));
        }

        @Test
        void calloutTitleColor() throws BadLocationException {
            var md = new MarkdownDocument(List.of(
                    new CalloutNode("tip", "text")));
            StyledDocument doc = renderer.render(md);
            String text = doc.getText(0, doc.getLength());
            int titlePos = text.indexOf("\uD83D\uDCA1");
            var attrs = doc.getCharacterElement(titlePos).getAttributes();
            assertEquals(ColorManager.SUCCESS_GREEN, StyleConstants.getForeground(attrs));
            assertTrue(StyleConstants.isBold(attrs));
        }

        @Test
        void calloutWarningRedTitle() throws BadLocationException {
            var md = new MarkdownDocument(List.of(
                    new CalloutNode("warning", "text")));
            StyledDocument doc = renderer.render(md);
            String text = doc.getText(0, doc.getLength());
            int titlePos = text.indexOf("\u26A0\uFE0F");
            var attrs = doc.getCharacterElement(titlePos).getAttributes();
            assertEquals(ColorManager.ERROR_RED, StyleConstants.getForeground(attrs));
        }

        @Test
        void calloutContainerHasBackgroundAndIndent() throws BadLocationException {
            var md = new MarkdownDocument(List.of(
                    new CalloutNode("info", "text")));
            StyledDocument doc = renderer.render(md);
            String text = doc.getText(0, doc.getLength());
            int bodyPos = text.indexOf("text");
            var paraAttrs = doc.getParagraphElement(bodyPos).getAttributes();
            assertNotNull(StyleConstants.getBackground(paraAttrs), "Callout should have background");
            assertEquals(20.0f, StyleConstants.getLeftIndent(paraAttrs), 0.01f);
        }

        @Test
        void calloutBodyStyle() throws BadLocationException {
            var md = new MarkdownDocument(List.of(
                    new CalloutNode("tip", "body text")));
            StyledDocument doc = renderer.render(md);
            String text = doc.getText(0, doc.getLength());
            int bodyPos = text.indexOf("body text");
            var attrs = doc.getCharacterElement(bodyPos).getAttributes();
            assertEquals(ColorManager.TEXT_SECONDARY, StyleConstants.getForeground(attrs));
            assertEquals(13, StyleConstants.getFontSize(attrs));
            assertFalse(StyleConstants.isBold(attrs));
        }

        @Test
        void defaultCalloutTypeUsesInfo() throws BadLocationException {
            var md = new MarkdownDocument(List.of(
                    new CalloutNode("unknown", "text")));
            StyledDocument doc = renderer.render(md);
            String text = doc.getText(0, doc.getLength());
            assertTrue(text.contains("\uD83D\uDCD8"), "Default type should use info emoji");
        }

        @Test
        void nullCalloutTypeUsesInfo() throws BadLocationException {
            var md = new MarkdownDocument(List.of(
                    new CalloutNode(null, "text")));
            StyledDocument doc = renderer.render(md);
            String text = doc.getText(0, doc.getLength());
            assertTrue(text.contains("\uD83D\uDCD8"));
        }

        @Test
        void nullCalloutTextRendersPrefixOnly() throws BadLocationException {
            var md = new MarkdownDocument(List.of(
                    new CalloutNode("tip", null)));
            StyledDocument doc = renderer.render(md);
            String text = doc.getText(0, doc.getLength());
            assertTrue(text.contains("Dica"));
        }
    }

    @Nested
    class Arrow {

        @Test
        void arrowRendered() throws BadLocationException {
            var md = new MarkdownDocument(List.of(
                    new ParagraphNode(List.of(Segment.text("\u2193")))));
            StyledDocument doc = renderer.render(md);
            String text = doc.getText(0, doc.getLength());
            assertTrue(text.contains("\u2193"), "Should contain arrow character");

            int arrowPos = text.indexOf("\u2193");
            var attrs = doc.getCharacterElement(arrowPos).getAttributes();
            assertEquals(ColorManager.TEXT_TERTIARY, StyleConstants.getForeground(attrs));
            assertTrue(StyleConstants.isBold(attrs));
            assertEquals(14, StyleConstants.getFontSize(attrs));
        }
    }

    @Nested
    class MixedDocument {

        @Test
        void allNodeTypesRenderedInOrder() throws BadLocationException {
            var md = new MarkdownDocument(List.of(
                    new HeadingNode(1, "Title"),
                    new ParagraphNode(List.of(Segment.text("Body."))),
                    new DividerNode(),
                    new BulletNode("item", false),
                    new CodeBlockNode("java", "code"),
                    new CalloutNode("tip", "tip text"),
                    new ParagraphNode(List.of(Segment.text("\u2193")))));
            StyledDocument doc = renderer.render(md);
            String text = doc.getText(0, doc.getLength());
            assertTrue(text.contains("Title"));
            assertTrue(text.contains("Body."));
            assertTrue(text.contains("\u2500"));
            assertTrue(text.contains("\u2022"));
            assertTrue(text.contains("item"));
            assertTrue(text.contains("code"));
            assertTrue(text.contains("tip"));
            assertTrue(text.contains("\u2193"));

            int titlePos = text.indexOf("Title");
            int bodyPos = text.indexOf("Body.");
            int dividerPos = text.indexOf("\u2500");
            int bulletPos = text.indexOf("item");
            int codePos = text.indexOf("code");
            int calloutPos = text.indexOf("tip");
            int arrowPos = text.indexOf("\u2193");

            assertTrue(titlePos < bodyPos, "Title before body");
            assertTrue(bodyPos < dividerPos, "Body before divider");
            assertTrue(dividerPos < bulletPos, "Divider before bullet");
            assertTrue(bulletPos < codePos, "Bullet before code");
            assertTrue(codePos < calloutPos, "Code before callout");
            assertTrue(calloutPos < arrowPos, "Callout before arrow");
        }
    }
}
