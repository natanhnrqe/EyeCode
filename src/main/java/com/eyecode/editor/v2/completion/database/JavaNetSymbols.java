package com.eyecode.editor.v2.completion.database;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionItemKind;

import java.util.List;

public final class JavaNetSymbols {

    private JavaNetSymbols() {}

    public static List<CompletionItem> getAll() {
        return List.of(
                CompletionItem.builder("URL", "URL", CompletionItemKind.CLASS)
                        .detail("java.net.URL")
                        .owner("java.net")
                        .category("Class")
                        .documentation("Represents a Uniform Resource Locator, a reference to a web resource.")
                        .example("URL url = new URL(\"https://example.com\");")
                        .build(),

                CompletionItem.builder("URI", "URI", CompletionItemKind.CLASS)
                        .detail("java.net.URI")
                        .owner("java.net")
                        .category("Class")
                        .documentation("Represents a Uniform Resource Identifier, a more general form than URL.")
                        .build(),

                CompletionItem.builder("Socket", "Socket", CompletionItemKind.CLASS)
                        .detail("java.net.Socket")
                        .owner("java.net")
                        .category("Class")
                        .documentation("Provides a client-side TCP socket for network communication.")
                        .build(),

                CompletionItem.builder("ServerSocket", "ServerSocket", CompletionItemKind.CLASS)
                        .detail("java.net.ServerSocket")
                        .owner("java.net")
                        .category("Class")
                        .documentation("Listens for incoming TCP connections from clients.")
                        .build(),

                CompletionItem.builder("InetAddress", "InetAddress", CompletionItemKind.CLASS)
                        .detail("java.net.InetAddress")
                        .owner("java.net")
                        .category("Class")
                        .documentation("Represents an IP address, either IPv4 or IPv6.")
                        .build(),

                CompletionItem.builder("HttpURLConnection", "HttpURLConnection", CompletionItemKind.CLASS)
                        .detail("java.net.HttpURLConnection")
                        .owner("java.net")
                        .category("Class")
                        .documentation("Provides HTTP-specific functionality for URL connections.")
                        .build()
        );
    }
}
