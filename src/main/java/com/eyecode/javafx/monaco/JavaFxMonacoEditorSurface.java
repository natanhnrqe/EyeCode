package com.eyecode.javafx.monaco;

import com.eyecode.javafx.ceffx.CeffxRuntime;
import com.techsenger.ceffx.core.CefClient;
import com.techsenger.ceffx.core.browser.CefBrowser;
import com.techsenger.ceffx.core.browser.CefFrame;
import com.techsenger.ceffx.core.browser.CefMessageRouter;
import com.techsenger.ceffx.core.callback.CefQueryCallback;
import com.techsenger.ceffx.core.handler.CefMessageRouterHandlerAdapter;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class JavaFxMonacoEditorSurface extends Region {
    private final MonacoBridge injectedBridge;
    private final Map<String, ModelState> models = new LinkedHashMap<>();
    private MonacoBridge bridge;
    private Node browserNode;
    private String activeModel;
    private boolean disposed;
    private Consumer<MonacoEvent> eventListener;

    public JavaFxMonacoEditorSurface() {
        this(null);
    }

    public JavaFxMonacoEditorSurface(MonacoBridge bridge) {
        this.injectedBridge = bridge;
        getStyleClass().add("monaco-editor-surface");
        setMinSize(0, 0);
        setPrefSize(800, 600);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        if (bridge != null) {
            attachBridge(bridge, null);
        } else {
            createBrowser();
        }
    }

    public void setEventListener(Consumer<MonacoEvent> listener) {
        eventListener = listener;
    }

    public void openModel(String id, String language, String content, boolean readOnly) {
        if (disposed || id == null) return;
        ModelState state = new ModelState(language, content == null ? "" : content, readOnly);
        models.put(id, state);
        send(new MonacoCommand.OpenModel(id, language, state.content, readOnly));
        if (activeModel == null) activateModel(id);
    }

    public void activateModel(String id) {
        if (disposed || !models.containsKey(id)) return;
        activeModel = id;
        send(new MonacoCommand.ActivateModel(id, models.get(id).readOnly));
    }

    public void updateModelContent(String id, String content, long version, String origin) {
        ModelState state = models.get(id);
        if (disposed || state == null) return;
        String nextContent = content == null ? "" : content;
        if (state.content.equals(nextContent)) {
            state.version = Math.max(state.version, version);
            return;
        }
        state.content = nextContent;
        state.version = version;
        send(new MonacoCommand.UpdateModel(id, state.content, version, origin == null ? "host" : origin));
    }

    public void closeModel(String id) {
        if (id == null) return;
        models.remove(id);
        send(new MonacoCommand.CloseModel(id));
        if (id.equals(activeModel)) activeModel = models.keySet().stream().findFirst().orElse(null);
    }

    public boolean containsModel(String id) { return models.containsKey(id); }
    public String getActiveModel() { return activeModel; }
    public int modelCount() { return models.size(); }
    public String modelContent(String id) { return models.containsKey(id) ? models.get(id).content : null; }

    public void setReadOnly(String id, boolean readOnly) {
        ModelState state = models.get(id);
        if (state == null) return;
        state.readOnly = readOnly;
        send(new MonacoCommand.SetReadOnly(id, readOnly));
    }

    public void revealPosition(String id, int line, int column) {
        send(new MonacoCommand.RevealPosition(id, line, column));
    }

    public void focusEditor() { send(new MonacoCommand.Focus()); }

    void receiveEventForTest(MonacoEvent event) { dispatchEvent(event); }

    static MonacoEvent parseEventForTest(String json) { return parseEvent(json); }

    public void dispose() {
        if (disposed) return;
        disposed = true;
        MonacoBridge current = bridge;
        bridge = null;
        models.clear();
        if (current != null) current.dispose();
        getChildren().clear();
    }

    @Override
    protected void layoutChildren() {
        if (browserNode != null) browserNode.resizeRelocate(0, 0, getWidth(), getHeight());
    }

    private void send(MonacoCommand command) {
        MonacoBridge current = bridge;
        if (current != null) current.send(command);
    }

    private void createBrowser() {
        try {
            CeffxRuntime.runLater(() -> {
                try {
                    CefClient client = CeffxRuntime.app().createClient();
                    CefMessageRouter router = CefMessageRouter.create(new RouterHandler());
                    client.addMessageRouter(router);
                    String url = getClass().getResource("/monaco/editor/index.html").toExternalForm();
                    CefBrowser browser = client.createBrowser(url, true, false);
                    browser.createImmediately();
                    Platform.runLater(() -> attachBridge(new CeffxMonacoBridge(client, browser, router), browser.getPane()));
                } catch (Throwable failure) {
                    Platform.runLater(() -> showFailure(failure));
                }
            });
        } catch (Throwable failure) {
            showFailure(failure);
        }
    }

    private void attachBridge(MonacoBridge created, Node node) {
        if (disposed) { created.dispose(); return; }
        bridge = created;
        created.setEventListener(this::dispatchEvent);
        if (node != null) {
            browserNode = node;
            getChildren().setAll(node);
            requestLayout();
        }
        for (Map.Entry<String, ModelState> entry : models.entrySet()) {
            ModelState state = entry.getValue();
            send(new MonacoCommand.OpenModel(entry.getKey(), state.language, state.content, state.readOnly));
        }
        if (activeModel != null) send(new MonacoCommand.ActivateModel(activeModel, models.get(activeModel).readOnly));
    }

    private void dispatchEvent(MonacoEvent event) {
        if (event != null && event.type() == MonacoEvent.Type.CONTENT_CHANGED) {
            ModelState state = models.get(event.modelId());
            if (state != null) {
                state.content = event.content() == null ? "" : event.content();
                state.version = event.version();
            }
        }
        Platform.runLater(() -> {
            if (!disposed && eventListener != null) eventListener.accept(event);
        });
    }

    private void showFailure(Throwable failure) {
        if (!disposed) {
            Label label = new Label("Monaco editor failed to initialize: " + failure.getMessage());
            label.getStyleClass().add("toolwindow-placeholder");
            getChildren().setAll(label);
        }
    }

    private final class RouterHandler extends CefMessageRouterHandlerAdapter {
        @Override
        public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request,
                               boolean persistent, CefQueryCallback callback) {
            MonacoEvent event = parseEvent(request);
            if (event != null) dispatchEvent(event);
            callback.success("ok");
            return true;
        }
    }

    private static MonacoEvent parseEvent(String json) {
        String kind = value(json, "kind");
        String id = value(json, "id");
        if ("ready".equals(kind)) return MonacoEvent.ready();
        if ("change".equals(kind)) return MonacoEvent.contentChanged(id, value(json, "content"), number(json, "version"));
        if ("selection".equals(kind)) return MonacoEvent.caretChanged(id, number(json, "line"), number(json, "column"),
                number(json, "endLine"), number(json, "endColumn"));
        return null;
    }

    private static String value(String json, String key) {
        String marker = "\"" + key + "\":";
        int start = json.indexOf(marker);
        if (start < 0) return null;
        start += marker.length();
        if (start < json.length() && json.charAt(start) == '\"') {
            int end = start + 1;
            StringBuilder result = new StringBuilder();
            boolean escaped = false;
            for (; end < json.length(); end++) {
                char c = json.charAt(end);
                if (escaped) {
                    switch (c) {
                        case 'n' -> result.append('\n');
                        case 'r' -> result.append('\r');
                        case 't' -> result.append('\t');
                        case 'b' -> result.append('\b');
                        case 'f' -> result.append('\f');
                        case '"', '\\', '/' -> result.append(c);
                        default -> result.append(c);
                    }
                    escaped = false;
                }
                else if (c == '\\') escaped = true;
                else if (c == '\"') break;
                else result.append(c);
            }
            return result.toString();
        }
        int end = json.indexOf(',', start);
        if (end < 0) end = json.indexOf('}', start);
        return end < 0 ? json.substring(start) : json.substring(start, end);
    }

    private static int number(String json, String key) {
        try { return Integer.parseInt(value(json, key)); } catch (Exception ignored) { return 0; }
    }

    private static final class ModelState {
        private final String language;
        private String content;
        private long version;
        private boolean readOnly;

        private ModelState(String language, String content, boolean readOnly) {
            this.language = language == null ? "java" : language;
            this.content = content;
            this.readOnly = readOnly;
        }
    }

    private static final class CeffxMonacoBridge implements MonacoBridge {
        private final CefClient client;
        private final CefBrowser browser;
        private final CefMessageRouter router;
        private Consumer<MonacoEvent> listener;
        private boolean disposed;

        private CeffxMonacoBridge(CefClient client, CefBrowser browser, CefMessageRouter router) {
            this.client = client; this.browser = browser; this.router = router;
        }

        @Override public void send(MonacoCommand command) {
            if (disposed) return;
            CeffxRuntime.runLater(() -> browser.executeJavaScript("window.eyecodeMonacoCommand(" + json(command) + ")", browser.getURL(), 0));
        }

        @Override public void setEventListener(Consumer<MonacoEvent> listener) { this.listener = listener; }
        @Override public void dispose() {
            if (disposed) return;
            disposed = true;
            router.dispose(); browser.close(true); client.dispose();
        }
    }

    private static String json(MonacoCommand command) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (command instanceof MonacoCommand.OpenModel c) { values.put("type", "openModel"); values.put("id", c.id()); values.put("language", c.language()); values.put("content", c.content()); values.put("readOnly", c.readOnly()); }
        else if (command instanceof MonacoCommand.ActivateModel c) { values.put("type", "activateModel"); values.put("id", c.id()); values.put("readOnly", c.readOnly()); }
        else if (command instanceof MonacoCommand.UpdateModel c) { values.put("type", "updateModel"); values.put("id", c.id()); values.put("content", c.content()); values.put("version", c.version()); values.put("origin", c.origin()); }
        else if (command instanceof MonacoCommand.CloseModel c) { values.put("type", "closeModel"); values.put("id", c.id()); }
        else if (command instanceof MonacoCommand.SetReadOnly c) { values.put("type", "setReadOnly"); values.put("id", c.id()); values.put("readOnly", c.readOnly()); }
        else if (command instanceof MonacoCommand.RevealPosition c) { values.put("type", "revealPosition"); values.put("id", c.id()); values.put("line", c.line()); values.put("column", c.column()); }
        else if (command instanceof MonacoCommand.Focus) values.put("type", "focus");
        else if (command instanceof MonacoCommand.ApplyEdit c) { values.put("type", "applyEdit"); values.put("id", c.id()); values.put("start", c.start()); values.put("end", c.end()); values.put("text", c.text()); }
        return object(values);
    }

    private static String object(Map<String, Object> values) {
        StringBuilder result = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (!first) result.append(',');
            first = false;
            result.append('"').append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof Boolean || value instanceof Number) result.append(value);
            else result.append('"').append(escape(String.valueOf(value))).append('"');
        }
        return result.append('}').toString();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t");
    }
}
