package com.eyecode.learning.markdown;

import com.eyecode.ui.designsystem.ColorManager;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
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

        }
    }

    private void renderHeading(HeadingNode heading) throws BadLocationException {
        SimpleAttributeSet style = switch (heading.level()) {
            case 1 -> MarkdownTheme.h1();
            default -> MarkdownTheme.h2();
        };
        int start = doc.getLength();
        append(heading.text(), style);
        append("\n", MarkdownTheme.body());
        doc.setParagraphAttributes(start, 1, style, false);
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

        int blockStart = doc.getLength();
        append(code, MarkdownTheme.codeBlock());
        MarkdownCodeHighlighter.highlight(doc, blockStart, code, codeBlock.language());

        String[] lines = code.split("\n", -1);
        int lineCount = code.endsWith("\n") ? lines.length - 1 : lines.length;
        int offset = blockStart;
        for (int i = 0; i < lineCount; i++) {
            boolean firstLine = (i == 0);
            boolean lastLine = (i == lineCount - 1);
            doc.setParagraphAttributes(offset, 1,
                    MarkdownTheme.codeBlockParagraph(firstLine, lastLine), false);
            offset += lines[i].length() + 1;
        }

        if (!code.endsWith("\n")) {
            append("\n", MarkdownTheme.codeBlock());
        }
    }

    private void renderDivider() throws BadLocationException {
        int start = doc.getLength();
        append(MarkdownTheme.dividerText(), MarkdownTheme.divider());
        append("\n", MarkdownTheme.body());
        doc.setParagraphAttributes(start, 1, MarkdownTheme.divider(), false);
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

    private void append(String text, SimpleAttributeSet style) throws BadLocationException {
        doc.insertString(doc.getLength(), text, style);
    }
}
