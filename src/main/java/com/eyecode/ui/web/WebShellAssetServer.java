package com.eyecode.ui.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public final class WebShellAssetServer implements AutoCloseable {
    private static final String WEB_SHELL_ROOT = "/webshell";
    private static final String MONACO_ROOT = "/monaco/editor";
    private static final Map<String, String> CONTENT_TYPES = Map.ofEntries(
            Map.entry("html", "text/html; charset=utf-8"),
            Map.entry("js", "text/javascript; charset=utf-8"),
            Map.entry("css", "text/css; charset=utf-8"),
            Map.entry("json", "application/json; charset=utf-8"),
            Map.entry("svg", "image/svg+xml"),
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("woff", "font/woff"),
            Map.entry("woff2", "font/woff2"),
            Map.entry("map", "application/json"));

    private final HttpServer server;
    private final ExecutorService executor;
    private final Supplier<String> bootstrapScript;
    private final Supplier<String> bootstrapPayload;
    private final Set<String> bootstrapOrigins;
    private final AtomicBoolean closed = new AtomicBoolean();

    private WebShellAssetServer(HttpServer server, ExecutorService executor, Supplier<String> bootstrapScript,
                                Supplier<String> bootstrapPayload, Set<String> bootstrapOrigins) {
        this.server = server;
        this.executor = executor;
        this.bootstrapScript = bootstrapScript == null ? () -> "" : bootstrapScript;
        this.bootstrapPayload = bootstrapPayload == null ? () -> "" : bootstrapPayload;
        this.bootstrapOrigins = bootstrapOrigins == null ? Set.of() : Set.copyOf(bootstrapOrigins);
    }

    public static WebShellAssetServer start() {
        return start(() -> "");
    }

    public static WebShellAssetServer start(Supplier<String> bootstrapScript) {
        return start(bootstrapScript, () -> "", Set.of());
    }

    public static WebShellAssetServer start(Supplier<String> bootstrapScript, Supplier<String> bootstrapPayload,
                                            Set<String> bootstrapOrigins) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
            ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
                Thread thread = new Thread(runnable, "eyecode-webshell-http");
                thread.setDaemon(true);
                return thread;
            });
            WebShellAssetServer assetServer = new WebShellAssetServer(server, executor, bootstrapScript,
                    bootstrapPayload, bootstrapOrigins);
            server.createContext(WEB_SHELL_ROOT, exchange -> assetServer.serve(exchange, WEB_SHELL_ROOT));
            server.createContext(MONACO_ROOT, exchange -> assetServer.serve(exchange, MONACO_ROOT));
            server.createContext(WEB_SHELL_ROOT + "/bootstrap", assetServer::serveBootstrap);
            server.setExecutor(executor);
            server.start();
            System.out.println("[WEBSHELL-HTTP] started " + assetServer.baseUrl());
            return assetServer;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to start loopback WebShell asset server", exception);
        }
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public String entryUrl() {
        return baseUrl() + WEB_SHELL_ROOT + "/index.html";
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        server.stop(0);
        executor.shutdown();
        try {
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) executor.shutdownNow();
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void serve(HttpExchange exchange, String root) throws IOException {
        try {
            if (!exchange.getRequestMethod().equals("GET") && !exchange.getRequestMethod().equals("HEAD")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            String resource = resourcePath(exchange.getRequestURI().getRawPath(), root);
            if (resource == null) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            try (InputStream input = WebShellAssetServer.class.getResourceAsStream(root + "/" + resource)) {
                if (input == null) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }
                byte[] body = body(resource, input.readAllBytes());
                exchange.getResponseHeaders().set("Content-Type", contentType(resource));
                if (root.equals(WEB_SHELL_ROOT) && isDiagnosticAsset(resource)) {
                    System.out.println("[WEBSHELL-HTTP] " + exchange.getRequestMethod() + " "
                            + exchange.getRequestURI().getRawPath() + " 200");
                }
                if (exchange.getRequestMethod().equals("HEAD")) {
                    exchange.sendResponseHeaders(200, -1);
                } else {
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                }
            }
        } finally {
            exchange.close();
        }
    }

    private void serveBootstrap(HttpExchange exchange) throws IOException {
        try {
            String origin = exchange.getRequestHeaders().getFirst("Origin");
            if (bootstrapOrigins.isEmpty() || origin == null || !bootstrapOrigins.contains(origin)) {
                exchange.sendResponseHeaders(403, -1);
                return;
            }
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", origin);
            exchange.getResponseHeaders().set("Vary", "Origin");
            if (exchange.getRequestMethod().equals("OPTIONS")) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if (!exchange.getRequestMethod().equals("GET")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            byte[] body = bootstrapPayload.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
        } finally {
            exchange.close();
        }
    }

    private byte[] body(String resource, byte[] source) {
        if (!resource.equals("index.html")) {
            return source;
        }
        String script = bootstrapScript.get();
        if (script == null || script.isBlank()) {
            return source;
        }
        String html = new String(source, StandardCharsets.UTF_8);
        String tag = "<script>" + script + "</script>";
        int headEnd = html.indexOf("</head>");
        if (headEnd < 0) {
            return source;
        }
        return (html.substring(0, headEnd) + tag + html.substring(headEnd))
                .getBytes(StandardCharsets.UTF_8);
    }

    private static String resourcePath(String rawPath, String root) {
        if (rawPath == null || !rawPath.startsWith(root) || rawPath.contains("%") || rawPath.contains("\\")) {
            return null;
        }
        String relative = rawPath.substring(root.length());
        if (relative.isEmpty() || relative.equals("/")) return "index.html";
        if (!relative.startsWith("/")) return null;
        relative = relative.substring(1);
        if (relative.isEmpty() || relative.contains("//")) return null;
        for (String segment : relative.split("/")) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) return null;
        }
        return relative;
    }

    private static String contentType(String resource) {
        int dot = resource.lastIndexOf('.');
        String extension = dot < 0 ? "" : resource.substring(dot + 1).toLowerCase();
        return CONTENT_TYPES.getOrDefault(extension, "application/octet-stream");
    }

    private static boolean isDiagnosticAsset(String resource) {
        return resource.equals("index.html") || resource.endsWith(".js") || resource.endsWith(".css");
    }
}
