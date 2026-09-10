package com.eyecode.lessons.content;

import com.vladsch.flexmark.ast.BlockQuote;
import com.vladsch.flexmark.ast.BulletList;
import com.vladsch.flexmark.ast.Code;
import com.vladsch.flexmark.ast.Emphasis;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.ast.ListItem;
import com.vladsch.flexmark.ast.OrderedList;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ast.StrongEmphasis;
import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.ast.SoftLineBreak;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public final class LessonMarkdownNormalizer {
    private final Parser parser = Parser.builder().build();

    public List<LessonContentBlock> normalize(String markdown) {
        if (markdown == null || markdown.isBlank()) throw new IllegalArgumentException("Markdown da aula vazio");
        List<LessonContentBlock> blocks = new ArrayList<>();
        for (Node node = parser.parse(markdown).getFirstChild(); node != null; node = node.getNext()) {
            if (node instanceof Heading heading) blocks.add(textBlock(LessonContentBlockType.HEADING, heading));
            else if (node instanceof Paragraph paragraph) blocks.add(textBlock(LessonContentBlockType.PARAGRAPH, paragraph));
            else if (node instanceof FencedCodeBlock code) blocks.add(new LessonContentBlock(LessonContentBlockType.CODE, null, null,
                    code.getInfo().toString().trim(), code.getContentChars().toString(), List.of()));
            else if (node instanceof BulletList list) blocks.add(listBlock(list, false));
            else if (node instanceof OrderedList list) blocks.add(listBlock(list, true));
            else if (node instanceof BlockQuote quote) blocks.add(callout(quote));
        }
        return List.copyOf(blocks);
    }

    private static LessonContentBlock textBlock(LessonContentBlockType type, Node node) {
        List<LessonInlineContent> inline = inline(node);
        return new LessonContentBlock(type, plain(inline), null, null, null, List.of(), inline, false);
    }

    private static LessonContentBlock listBlock(Node list, boolean ordered) {
        List<String> items = new ArrayList<>();
        for (Node item = list.getFirstChild(); item != null; item = item.getNext()) {
            if (item instanceof ListItem) items.add(plain(inline(firstParagraph(item))));
        }
        return new LessonContentBlock(LessonContentBlockType.LIST, null, null, null, null, items, List.of(), ordered);
    }

    private static LessonContentBlock callout(BlockQuote quote) {
        List<LessonInlineContent> inline = inline(firstParagraph(quote));
        return new LessonContentBlock(LessonContentBlockType.CALLOUT, plain(inline), "Nota", null, null, List.of(), inline, false);
    }

    private static Node firstParagraph(Node node) {
        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            if (child instanceof Paragraph) return child;
        }
        return node;
    }

    private static List<LessonInlineContent> inline(Node node) {
        List<LessonInlineContent> content = new ArrayList<>();
        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            if (child instanceof Text text) content.add(new LessonInlineContent(LessonInlineContentType.TEXT, text.getChars().toString(), null));
            else if (child instanceof Code code) content.add(new LessonInlineContent(LessonInlineContentType.CODE, code.getText().toString(), null));
            else if (child instanceof Emphasis emphasis) content.add(new LessonInlineContent(LessonInlineContentType.EMPHASIS, plain(inline(emphasis)), null));
            else if (child instanceof StrongEmphasis strong) content.add(new LessonInlineContent(LessonInlineContentType.STRONG, plain(inline(strong)), null));
            else if (child instanceof Link link) appendLink(content, link);
            else if (child instanceof SoftLineBreak) content.add(new LessonInlineContent(LessonInlineContentType.TEXT, " ", null));
            else if (!(child instanceof com.vladsch.flexmark.ast.HtmlInline)) content.addAll(inline(child));
        }
        return List.copyOf(content);
    }

    private static void appendLink(List<LessonInlineContent> content, Link link) {
        String text = plain(inline(link));
        String url = safeHttpUrl(link.getUrl().toString());
        if (url == null) content.add(new LessonInlineContent(LessonInlineContentType.TEXT, text, null));
        else content.add(new LessonInlineContent(LessonInlineContentType.LINK, text, url));
    }

    private static String safeHttpUrl(String value) {
        try {
            URI uri = new URI(value);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) ? uri.toString() : null;
        } catch (URISyntaxException exception) {
            return null;
        }
    }

    private static String plain(List<LessonInlineContent> content) {
        return content.stream().map(LessonInlineContent::text).reduce("", String::concat);
    }
}
