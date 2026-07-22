package com.eyecode.learning.html;

import com.eyecode.learning.content.LearningResourceLoader;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class LearningHtmlBuilder {

    private static final String CSS_RESOURCE = "/learning/css/learning.css";
    private static final String HIGHLIGHT_JS_RESOURCE = "/learning/js/highlight.min.js";
    private static final String JS_RESOURCE = "/learning/js/learning.js";

    private final String cssHref;
    private final String highlightJsSrc;
    private final String jsSrc;

    public LearningHtmlBuilder() {
        LearningResourceLoader resourceLoader = new LearningResourceLoader();
        cssHref = toDataUrl("text/css", resourceLoader.load(CSS_RESOURCE));
        highlightJsSrc = toDataUrl("text/javascript", resourceLoader.load(HIGHLIGHT_JS_RESOURCE));
        jsSrc = toDataUrl("text/javascript", resourceLoader.load(JS_RESOURCE));
    }

    public String build(String bodyContent) {
        String body = bodyContent == null ? "" : bodyContent;
        return "<!DOCTYPE html>\n"
                + "<html>\n"
                + "<head>\n"
                + "<meta charset=\"UTF-8\">\n"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
                + "<link rel=\"stylesheet\" href=\"" + cssHref + "\">\n"
                + "<script defer src=\"" + highlightJsSrc + "\"></script>\n"
                + "<script defer src=\"" + jsSrc + "\"></script>\n"
                + "</head>\n"
                + "<body class=\"learning-markdown\">\n"
                + body
                + "\n</body>\n"
                + "</html>\n";
    }

    private String toDataUrl(String mimeType, String content) {
        String safeContent = content == null ? "" : content;
        return "data:" + mimeType + ";base64,"
                + Base64.getEncoder().encodeToString(safeContent.getBytes(StandardCharsets.UTF_8));
    }
}
