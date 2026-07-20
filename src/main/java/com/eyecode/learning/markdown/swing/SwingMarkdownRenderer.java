package com.eyecode.learning.markdown.swing;

import com.eyecode.learning.markdown.*;
import com.eyecode.learning.markdown.component.*;
import com.eyecode.ui.designsystem.ColorManager;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyledDocument;
import java.awt.Color;
import java.util.List;

public final class SwingMarkdownRenderer {

    private StyledDocument doc;

    public StyledDocument render(List<MarkdownComponent> components) {
        doc = new DefaultStyledDocument();
        if (components == null) {
            return doc;
        }
        try {
            for (MarkdownComponent component : components) {
                renderComponent(component);
            }
        } catch (BadLocationException e) {
        }
        return doc;
    }

    private void renderComponent(MarkdownComponent component) throws BadLocationException {
        switch (component) {
            case HeadingComponent h -> renderHeading(h);
            case ParagraphComponent p -> renderParagraph(p);
            case CodeBlockComponent c -> renderCodeBlock(c);
            case DividerComponent d -> renderDivider();
            case ListComponent l -> renderList(l);
        }
    }

    private void renderHeading(HeadingComponent heading) throws BadLocationException {
        if (heading.level() == 2) {
            String text = heading.text();
            if (isInformationHeading(text)) {
                renderInformationHeading(heading);
                return;
            }
            if (isWarningHeading(text)) {
                renderWarningHeading(heading);
                return;
            }
            if (isTipHeading(text)) {
                renderTipHeading(heading);
                return;
            }
            if (isNextStepHeading(text)) {
                renderNextStepHeading(heading);
                return;
            }
        }
        renderDefaultHeading(heading);
    }

    private void renderDefaultHeading(HeadingComponent heading) throws BadLocationException {
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

    private void renderInformationHeading(HeadingComponent heading) throws BadLocationException {
        renderDefaultHeading(heading);
    }

    private void renderWarningHeading(HeadingComponent heading) throws BadLocationException {
        renderDefaultHeading(heading);
    }

    private void renderTipHeading(HeadingComponent heading) throws BadLocationException {
        renderDefaultHeading(heading);
    }

    private void renderNextStepHeading(HeadingComponent heading) throws BadLocationException {
        renderDefaultHeading(heading);
    }

    private void renderParagraph(ParagraphComponent paragraph) throws BadLocationException {
        List<Segment> segments = paragraph.segments();
        String firstText = segments.isEmpty() ? "" : segments.getFirst().text();

        if (firstText.equals("\u2193")) {
            int start = doc.getLength();
            for (Segment segment : segments) {
                append(segment.text(), MarkdownTheme.arrow());
            }
            append("\n", MarkdownTheme.arrow());
            doc.setParagraphAttributes(start, 1, MarkdownTheme.arrow(), false);
            return;
        }

        int start = doc.getLength();
        for (Segment segment : segments) {
            append(segment.text(), segmentStyle(segment));
        }
        append("\n", MarkdownTheme.body());
        doc.setParagraphAttributes(start, 1, MarkdownTheme.body(), false);
    }

    private void renderList(ListComponent list) throws BadLocationException {
        boolean ordered = list.ordered();
        for (int i = 0; i < list.items().size(); i++) {
            String prefix = ordered
                    ? (i + 1) + ". "
                    : "\u2022 ";
            int start = doc.getLength();
            append(prefix + list.items().get(i), MarkdownTheme.bullet());
            append("\n", MarkdownTheme.body());
            doc.setParagraphAttributes(start, 1, MarkdownTheme.bullet(), false);
        }
    }

    private void renderCodeBlock(CodeBlockComponent codeBlock) throws BadLocationException {
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

    private static boolean isInformationHeading(String text) {
        return text.startsWith("\uD83D\uDCD8 Informa\u00E7\u00E3o");
    }

    private static boolean isWarningHeading(String text) {
        return text.startsWith("\u26A0\uFE0F Aten\u00E7\u00E3o");
    }

    private static boolean isTipHeading(String text) {
        return text.startsWith("\uD83D\uDCA1 Dica");
    }

    private static boolean isNextStepHeading(String text) {
        return text.startsWith("\u27A1\uFE0F Pr\u00F3ximo passo");
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
