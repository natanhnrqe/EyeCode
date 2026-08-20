package com.eyecode.javafx.ceffx;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class CeffxDataUrl {

    private static final String HTML_PREFIX = "data:text/html;base64,";

    private CeffxDataUrl() {
    }

    public static String html(String html) {
        String content = html == null ? "" : html;

        String encoded = Base64.getEncoder()
                .encodeToString(content.getBytes(StandardCharsets.UTF_8));

        return HTML_PREFIX + encoded;
    }
}