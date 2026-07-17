package com.eyecode.learning.markdown;

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
            // should not happen with append-only mutations
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
            case 2 -> {
                Color color = MarkdownTheme.sectionColor(heading.text().isEmpty()
                        ? "" : heading.text().substring(0, Math.min(2, heading.text().length())));
                yield MarkdownTheme.h2Colored(color);
            }
            default -> MarkdownTheme.h2();
        };
        append(heading.text(), style);
        append("\n", MarkdownTheme.body());
    }

    private void renderParagraph(ParagraphNode paragraph) throws BadLocationException {
        for (Segment segment : paragraph.segments()) {
            SimpleAttributeSet segStyle = segmentStyle(segment);
            append(segment.text(), segStyle);
        }
        append("\n", MarkdownTheme.body());
    }

    private void renderBullet(BulletNode bullet) throws BadLocationException {
        String prefix = bullet.checked() ? "\u2611 " : "\u2022 ";
        append(prefix + bullet.text(), MarkdownTheme.bullet());
        append("\n", MarkdownTheme.body());
    }

    private void renderCodeBlock(CodeBlockNode codeBlock) throws BadLocationException {
        int blockStart = doc.getLength();
        append("\n", MarkdownTheme.body());

        for (String line : codeBlock.code().split("\n", -1)) {
            int lineStart = doc.getLength();
            if (!line.isEmpty()) {
                append(line, MarkdownTheme.codeBlock());
                MarkdownCodeHighlighter.highlightJava(doc, line, lineStart);
            }
            append("\n", MarkdownTheme.codeBlock());
            doc.setParagraphAttributes(lineStart, 1, MarkdownTheme.codeParagraph(), false);
        }

        append("\n", MarkdownTheme.body());
    }

    private void renderDivider() throws BadLocationException {
        append(MarkdownTheme.dividerText(), MarkdownTheme.divider());
        append("\n", MarkdownTheme.body());
    }

    private void renderCallout(CalloutNode callout) throws BadLocationException {
        Color bg = MarkdownTheme.calloutBackground(callout.type());
        int blockStart = doc.getLength();

        append("\n", MarkdownTheme.body());
        int start = doc.getLength();
        append(callout.text(), MarkdownTheme.callout());
        append("\n", MarkdownTheme.body());

        if (bg != null) {
            SimpleAttributeSet calloutBg = new SimpleAttributeSet();
            StyleConstants.setBackground(calloutBg, bg);
            // apply paragraph background to the callout paragraph
            doc.setParagraphAttributes(start, 1, calloutBg, false);
        }
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
