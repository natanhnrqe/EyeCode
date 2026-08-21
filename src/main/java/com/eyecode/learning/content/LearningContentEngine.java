package com.eyecode.learning.content;

import com.eyecode.learning.html.LearningHtmlBuilder;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.emoji.EmojiExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;

import java.util.List;

public final class LearningContentEngine {

    private final LearningResourceLoader resourceLoader;
    private final LearningContentRepository contentRepository;
    private final LearningHtmlBuilder htmlBuilder;
    private final Parser parser;
    private final HtmlRenderer renderer;

    public LearningContentEngine() {
        this(new LearningResourceLoader());
    }

    public LearningContentEngine(LearningResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        contentRepository = new LearningContentRepository(resourceLoader);
        htmlBuilder = new LearningHtmlBuilder();
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, List.of(
                AutolinkExtension.create(),
                EmojiExtension.create(),
                TablesExtension.create()
        ));
        parser = Parser.builder(options).build();
        renderer = HtmlRenderer.builder(options).build();
    }

    public String loadMarkdown(String resourcePath) {
        return resourceLoader.load(resourcePath);
    }

    public String convert(String markdown) {
        return htmlBuilder.build(renderBody(markdown));
    }

    public String loadHtml(String resourcePath) {
        String markdown = loadMarkdown(resourcePath);
        if (markdown.trim().startsWith("---")) {
            return convert(frontMatterParser().parse(markdown, resourcePath).body());
        }
        return convert(markdown);
    }

    public String loadHtmlByIdentifier(String identifier) {
        return loadDocument(identifier).renderedHtml();
    }

    public LearningDocument loadDocument(String identifier) {
        LearningDocument source = contentRepository.loadDocument(identifier);
        return new LearningDocument(
                source.identifier(),
                source.metadata(),
                source.markdownBody(),
                convert(source.markdownBody())
        );
    }

    private String renderBody(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        return renderer.render(parser.parse(expandCallouts(markdown)));
    }

    private LearningFrontMatterParser frontMatterParser() {
        return new LearningFrontMatterParser();
    }

    private static String expandCallouts(String markdown) {
        String[] lines = markdown.split("\\R", -1);
        StringBuilder result = new StringBuilder(markdown.length());
        for (int index = 0; index < lines.length; index++) {
            String marker = lines[index].trim();
            String kind = marker.equals("> [!INFO]") ? "info"
                    : marker.equals("> [!WARNING]") ? "warning" : null;
            if (kind == null) {
                result.append(lines[index]).append('\n');
                continue;
            }
            StringBuilder message = new StringBuilder();
            int next = index + 1;
            while (next < lines.length && lines[next].trim().startsWith(">")) {
                String text = lines[next].trim().substring(1).trim();
                if (!message.isEmpty()) {
                    message.append(' ');
                }
                message.append(escapeHtml(text));
                next++;
            }
            result.append("<div class=\"learning-callout learning-callout-")
                    .append(kind)
                    .append("\"><strong>")
                    .append(kind.equals("info") ? "Info" : "Warning")
                    .append("</strong><p>")
                    .append(message)
                    .append("</p></div>\n");
            index = next - 1;
        }
        return result.toString();
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
