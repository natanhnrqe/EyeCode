package com.eyecode.learning.markdown.swing;

import com.eyecode.learning.markdown.*;
import com.eyecode.learning.markdown.component.*;
import com.eyecode.learning.markdown.layout.ComponentLayout;
import com.eyecode.learning.markdown.layout.MarkdownLayoutEngine;
import com.eyecode.learning.markdown.theme.DefaultMarkdownThemeProvider;
import com.eyecode.learning.markdown.theme.MarkdownDesignTokens;
import com.eyecode.learning.markdown.theme.MarkdownThemeProvider;
import com.eyecode.ui.designsystem.ColorManager;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.util.List;

public final class SwingMarkdownRenderer {

    private final MarkdownThemeProvider themeProvider;
    private final MarkdownLayoutEngine layoutEngine;
    private StyledDocument doc;
    private int viewportWidth;

    public SwingMarkdownRenderer() {
        this(new DefaultMarkdownThemeProvider());
    }

    public SwingMarkdownRenderer(MarkdownThemeProvider themeProvider) {
        this.themeProvider = themeProvider;
        this.layoutEngine = new MarkdownLayoutEngine();
    }

    public StyledDocument render(List<MarkdownComponent> components) {
        return render(components, 0);
    }

    public StyledDocument render(List<MarkdownComponent> components, int viewportWidth) {
        this.viewportWidth = viewportWidth;
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
        SimpleAttributeSet style = switch (heading.level()) {
            case 1 -> themeProvider.h1();
            default -> themeProvider.h2();
        };
        ComponentLayout layout = layoutEngine.layout(heading, viewportWidth);
        StyleConstants.setSpaceAbove(style, layout.spaceAbove());
        StyleConstants.setSpaceBelow(style, layout.spaceBelow());
        StyleConstants.setLeftIndent(style, layout.leftIndent());
        StyleConstants.setRightIndent(style, layout.rightIndent());
        int start = doc.getLength();
        append(heading.text(), style);
        append("\n", themeProvider.body());
        doc.setParagraphAttributes(start, 1, style, false);
    }

    private void renderParagraph(ParagraphComponent paragraph) throws BadLocationException {
        List<Segment> segments = paragraph.segments();
        String firstText = segments.isEmpty() ? "" : segments.getFirst().text();

        if (firstText.equals("\u2193")) {
            int start = doc.getLength();
            for (Segment segment : segments) {
                append(segment.text(), themeProvider.arrow());
            }
            append("\n", themeProvider.arrow());
            ComponentLayout arrowLayout = layoutEngine.arrowLayout(viewportWidth);
            SimpleAttributeSet arrowStyle = themeProvider.arrow();
            StyleConstants.setSpaceAbove(arrowStyle, arrowLayout.spaceAbove());
            StyleConstants.setSpaceBelow(arrowStyle, arrowLayout.spaceBelow());
            StyleConstants.setLeftIndent(arrowStyle, arrowLayout.leftIndent());
            StyleConstants.setRightIndent(arrowStyle, arrowLayout.rightIndent());
            doc.setParagraphAttributes(start, 1, arrowStyle, false);
            return;
        }

        int start = doc.getLength();
        for (Segment segment : segments) {
            append(segment.text(), segmentStyle(segment));
        }
        append("\n", themeProvider.body());
        ComponentLayout layout = layoutEngine.layout(paragraph, viewportWidth);
        SimpleAttributeSet style = themeProvider.body();
        StyleConstants.setSpaceAbove(style, layout.spaceAbove());
        StyleConstants.setSpaceBelow(style, layout.spaceBelow());
        StyleConstants.setLeftIndent(style, layout.leftIndent());
        StyleConstants.setRightIndent(style, layout.rightIndent());
        StyleConstants.setLineSpacing(style, layout.lineSpacing());
        doc.setParagraphAttributes(start, 1, style, false);
    }

    private void renderList(ListComponent list) throws BadLocationException {
        ComponentLayout layout = layoutEngine.layout(list, viewportWidth);
        SimpleAttributeSet baseStyle = themeProvider.bullet();
        StyleConstants.setSpaceAbove(baseStyle, layout.spaceAbove());
        StyleConstants.setSpaceBelow(baseStyle, layout.spaceBelow());
        StyleConstants.setLeftIndent(baseStyle, layout.leftIndent());
        StyleConstants.setRightIndent(baseStyle, layout.rightIndent());
        StyleConstants.setLineSpacing(baseStyle, layout.lineSpacing());
        boolean ordered = list.ordered();
        for (int i = 0; i < list.items().size(); i++) {
            String prefix = ordered
                    ? (i + 1) + ". "
                    : "\u2022 ";
            int start = doc.getLength();
            append(prefix + list.items().get(i), baseStyle);
            append("\n", themeProvider.body());
            int itemSpaceBelow = (i == list.items().size() - 1)
                    ? MarkdownDesignTokens.LIST_SPACE_BELOW
                    : MarkdownDesignTokens.BULLET_SPACE_BELOW;
            StyleConstants.setSpaceBelow(baseStyle, itemSpaceBelow);
            doc.setParagraphAttributes(start, 1, baseStyle, false);
        }
    }

    private void renderCodeBlock(CodeBlockComponent codeBlock) throws BadLocationException {
        String code = codeBlock.code();
        if (code.isEmpty()) {
            return;
        }

        int blockStart = doc.getLength();
        append(code, themeProvider.codeBlock());
        MarkdownCodeHighlighter.highlight(doc, blockStart, code, codeBlock.language());

        ComponentLayout layout = layoutEngine.layout(codeBlock, viewportWidth);
        String[] lines = code.split("\n", -1);
        int lineCount = code.endsWith("\n") ? lines.length - 1 : lines.length;
        int offset = blockStart;
        for (int i = 0; i < lineCount; i++) {
            boolean firstLine = (i == 0);
            boolean lastLine = (i == lineCount - 1);
            SimpleAttributeSet lineStyle = themeProvider.codeBlockParagraph(firstLine, lastLine);
            StyleConstants.setSpaceAbove(lineStyle, firstLine ? layout.paddingTop() : 0);
            StyleConstants.setSpaceBelow(lineStyle, lastLine ? layout.paddingBottom() : 0);
            StyleConstants.setLeftIndent(lineStyle, layout.leftIndent() + MarkdownDesignTokens.CODE_PADDING_LEFT);
            StyleConstants.setRightIndent(lineStyle, layout.rightIndent() + MarkdownDesignTokens.CODE_PADDING_RIGHT);
            StyleConstants.setLineSpacing(lineStyle, layout.lineSpacing());
            doc.setParagraphAttributes(offset, 1, lineStyle, false);
            offset += lines[i].length() + 1;
        }

        if (!code.endsWith("\n")) {
            append("\n", themeProvider.codeBlock());
        }
    }

    private void renderDivider() throws BadLocationException {
        int start = doc.getLength();
        ComponentLayout layout = layoutEngine.layout(new DividerComponent(), viewportWidth);
        SimpleAttributeSet style = themeProvider.divider();
        StyleConstants.setSpaceAbove(style, layout.spaceAbove());
        StyleConstants.setSpaceBelow(style, layout.spaceBelow());
        StyleConstants.setLeftIndent(style, layout.leftIndent());
        StyleConstants.setRightIndent(style, layout.rightIndent());
        append(themeProvider.dividerText(), style);
        append("\n", themeProvider.body());
        doc.setParagraphAttributes(start, 1, style, false);
    }

    private SimpleAttributeSet segmentStyle(Segment segment) {
        return switch (segment.type()) {
            case TEXT -> themeProvider.body();
            case BOLD -> themeProvider.bold();
            case ITALIC -> themeProvider.italic();
            case CODE -> themeProvider.codeInline();
            case LINK -> themeProvider.link();
        };
    }

    private void append(String text, SimpleAttributeSet style) throws BadLocationException {
        doc.insertString(doc.getLength(), text, style);
    }
}
