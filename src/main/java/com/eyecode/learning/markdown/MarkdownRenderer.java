package com.eyecode.learning.markdown;

import com.eyecode.ui.designsystem.ColorManager;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.Color;

public final class MarkdownRenderer {

    private static final Color TRANSPARENT = new Color(0, 0, 0, 0);

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
            case 2 -> MarkdownTheme.h2Colored(sectionColor(heading.text()));
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
        append("\n", MarkdownTheme.body());

        int blockStart = doc.getLength();
        String code = codeBlock.code();

        if (!code.isEmpty()) {
            append(code, MarkdownTheme.codeBlock());
            MarkdownCodeHighlighter.highlight(doc, blockStart, code, codeBlock.language());
        }
        append("\n", MarkdownTheme.codeBlock());

        int paraOffset = blockStart;
        for (String line : code.split("\n", -1)) {
            doc.setParagraphAttributes(paraOffset, 1, MarkdownTheme.codeParagraph(), false);
            paraOffset += line.length() + 1;
        }

        append("\n", MarkdownTheme.body());
    }

    private void renderDivider() throws BadLocationException {
        append(MarkdownTheme.dividerText(), MarkdownTheme.divider());
        append("\n", MarkdownTheme.body());
    }

    private void renderCallout(CalloutNode callout) throws BadLocationException {
        Color bg = calloutBackground(callout.type());

        append("\n", MarkdownTheme.body());
        int start = doc.getLength();
        append(callout.text(), MarkdownTheme.callout());
        append("\n", MarkdownTheme.body());

        if (bg != null && !bg.equals(TRANSPARENT)) {
            SimpleAttributeSet calloutBg = new SimpleAttributeSet();
            StyleConstants.setBackground(calloutBg, bg);
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

    private Color calloutBackground(String type) {
        if (type == null) {
            return TRANSPARENT;
        }
        return switch (type.toLowerCase()) {
            case "info" -> MarkdownTheme.calloutInfoBg();
            case "warning" -> MarkdownTheme.calloutWarningBg();
            case "tip" -> MarkdownTheme.calloutTipBg();
            default -> TRANSPARENT;
        };
    }

    private void append(String text, SimpleAttributeSet style) throws BadLocationException {
        doc.insertString(doc.getLength(), text, style);
    }
}
