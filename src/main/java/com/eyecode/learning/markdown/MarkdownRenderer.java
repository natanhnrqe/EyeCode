package com.eyecode.learning.markdown;

import com.eyecode.ui.designsystem.ColorManager;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.Color;

public final class MarkdownRenderer {

    private StyledDocument doc;

    public StyledDocument render(MarkdownDocument document) {
        doc = new DefaultStyledDocument();
        if (document == null) {
            return doc;
        }
        try {
            for (MarkdownNode node : document.nodes()) {
                renderNode(node);
            }
        } catch (BadLocationException e) {
        }
        return doc;
    }

    private void renderNode(MarkdownNode node) throws BadLocationException {
        switch (node) {
            case HeadingNode h -> renderHeading(h);
            case ParagraphNode p -> renderParagraph(p);
            case BulletNode b -> renderBullet(b);
            case CodeBlockNode c -> renderCodeBlock(c);
            case DividerNode d -> renderDivider();
            case CalloutNode c -> renderCallout(c);
        }
    }

    private void renderHeading(HeadingNode heading) throws BadLocationException {
        SimpleAttributeSet style = switch (heading.level()) {
            case 1 -> MarkdownTheme.h1();
            case 2 -> MarkdownTheme.h2Colored(sectionColor(heading.text()));
            default -> MarkdownTheme.h2();
        };
        int start = doc.getLength();
        append(heading.text(), style);
        append("\n", MarkdownTheme.body());
        doc.setParagraphAttributes(start, 1, style, false);
        if (heading.level() == 2) {
            doc.setParagraphAttributes(start, 1, MarkdownTheme.h2Background(), false);
        }
    }

    private void renderParagraph(ParagraphNode paragraph) throws BadLocationException {
        String firstText = paragraph.segments().isEmpty()
                ? "" : paragraph.segments().getFirst().text();

        if (firstText.equals("\u2193")) {
            int start = doc.getLength();
            for (Segment segment : paragraph.segments()) {
                append(segment.text(), MarkdownTheme.arrow());
            }
            append("\n", MarkdownTheme.arrow());
            doc.setParagraphAttributes(start, 1, MarkdownTheme.arrow(), false);
            return;
        }

        int start = doc.getLength();
        for (Segment segment : paragraph.segments()) {
            append(segment.text(), segmentStyle(segment));
        }
        append("\n", MarkdownTheme.body());
        doc.setParagraphAttributes(start, 1, MarkdownTheme.body(), false);
    }

    private void renderBullet(BulletNode bullet) throws BadLocationException {
        String prefix = bullet.checked() ? "\u2611 " : "\u2022 ";

        int start = doc.getLength();
        append(prefix + bullet.text(), MarkdownTheme.bullet());
        append("\n", MarkdownTheme.body());
        doc.setParagraphAttributes(start, 1, MarkdownTheme.bullet(), false);
    }

    private void renderCodeBlock(CodeBlockNode codeBlock) throws BadLocationException {
        String code = codeBlock.code();
        if (code.isEmpty()) {
            return;
        }

        // Insert separator newline to end previous paragraph
        int sepStart = doc.getLength();
        append("\n", MarkdownTheme.paragraph());
        doc.setParagraphAttributes(sepStart, 1, MarkdownTheme.paragraph(), true);
        doc.setCharacterAttributes(sepStart, 1, MarkdownTheme.paragraph(), true);

        int blockStart = doc.getLength();
        append(code, MarkdownTheme.codeBlock());
        MarkdownCodeHighlighter.highlight(doc, blockStart, code, codeBlock.language());

        String[] lines = code.split("\n", -1);
        int offset = blockStart;
        for (int i = 0; i < lines.length; i++) {
            boolean firstLine = (i == 0);
            boolean lastLine = (i == lines.length - 1);
            doc.setParagraphAttributes(offset, 1,
                    MarkdownTheme.codeBlockParagraph(firstLine, lastLine), false);
            offset += lines[i].length() + 1;
        }

        // Trailing newline — ensures next paragraph starts with clean body attributes
        append("\n", MarkdownTheme.body());
    }

    private void renderDivider() throws BadLocationException {
        int start = doc.getLength();
        append(MarkdownTheme.dividerText(), MarkdownTheme.divider());
        append("\n", MarkdownTheme.body());
        doc.setParagraphAttributes(start, 1, MarkdownTheme.divider(), false);
    }

    private void renderCallout(CalloutNode callout) throws BadLocationException {
        String type = callout.type() != null ? callout.type().toLowerCase() : "info";
        String text = callout.text() != null ? callout.text() : "";

        String prefix = switch (type) {
            case "tip" -> "\uD83D\uDCA1 Dica: ";
            case "warning" -> "\u26A0\uFE0F Aten\u00E7\u00E3o: ";
            default -> "\uD83D\uDCD8 Informa\u00E7\u00E3o: ";
        };

        // Leading separator — end previous paragraph and reset to body defaults
        int sepStart = doc.getLength();
        append("\n", MarkdownTheme.body());
        doc.setParagraphAttributes(sepStart, 1, MarkdownTheme.body(), true);
        doc.setCharacterAttributes(sepStart, 1, MarkdownTheme.body(), true);

        // Title paragraph
        SimpleAttributeSet titlePara = new SimpleAttributeSet();
        titlePara.addAttributes(MarkdownTheme.calloutContainer(type));
        titlePara.addAttributes(MarkdownTheme.calloutTitleParagraph());
        int titleStart = doc.getLength();
        append(prefix, MarkdownTheme.calloutTitle(type));
        append("\n", MarkdownTheme.body());
        doc.setParagraphAttributes(titleStart, 1, titlePara, false);

        // Body paragraph
        if (!text.isEmpty()) {
            SimpleAttributeSet bodyPara = new SimpleAttributeSet();
            bodyPara.addAttributes(MarkdownTheme.calloutContainer(type));
            bodyPara.addAttributes(MarkdownTheme.calloutBodyParagraph());
            int bodyStart = doc.getLength();
            append(text, MarkdownTheme.calloutBody());
            append("\n", MarkdownTheme.body());
            doc.setParagraphAttributes(bodyStart, 1, bodyPara, false);
        }

        // Trailing separator — restore next paragraph to normal body
        int afterStart = doc.getLength();
        append("\n", MarkdownTheme.body());
        doc.setParagraphAttributes(afterStart, 1, MarkdownTheme.body(), true);
    }

    private SimpleAttributeSet segmentStyle(Segment segment) {
        return switch (segment.type()) {
            case TEXT -> MarkdownTheme.body();
            case BOLD -> MarkdownTheme.bold();
            case ITALIC -> MarkdownTheme.italic();
            case CODE -> MarkdownTheme.codeInline();
            case LINK -> MarkdownTheme.link();
        };
    }

    private Color sectionColor(String headingText) {
        String emoji = headingText.isEmpty()
                ? "" : headingText.substring(0, Math.min(2, headingText.length()));
        return switch (emoji) {
            case "\uD83D\uDCA1" -> ColorManager.ACCENT_BLUE_LIGHT;
            case "\uD83C\uDFE0" -> ColorManager.SYNTAX_CLASS;
            case "\uD83C\uDF0E" -> ColorManager.SUCCESS_GREEN;
            case "\uD83D\uDCBB" -> ColorManager.SYNTAX_KEYWORD;
            case "\uD83E\uDDE0" -> ColorManager.SYNTAX_CLASS;
            case "\u26A0\uFE0F" -> ColorManager.ERROR_RED;
            case "\uD83D\uDE80" -> ColorManager.TEXT_PRIMARY;
            case "\u27A1\uFE0F" -> ColorManager.ACCENT_BLUE_LIGHT;
            case "\uD83D\uDCDA" -> ColorManager.TEXT_TERTIARY;
            default -> ColorManager.TEXT_PRIMARY;
        };
    }

    private void append(String text, SimpleAttributeSet style) throws BadLocationException {
        doc.insertString(doc.getLength(), text, style);
    }
}
