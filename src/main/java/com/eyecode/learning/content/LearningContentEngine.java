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
    private final LearningHtmlBuilder htmlBuilder;
    private final Parser parser;
    private final HtmlRenderer renderer;

    public LearningContentEngine() {
        this(new LearningResourceLoader());
    }

    public LearningContentEngine(LearningResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
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
        return convert(loadMarkdown(resourcePath));
    }

    private String renderBody(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        return renderer.render(parser.parse(markdown));
    }
}
