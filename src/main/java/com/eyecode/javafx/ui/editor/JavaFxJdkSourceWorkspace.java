package com.eyecode.javafx.ui.editor;

import com.eyecode.language.documentation.JdkSourceLoader;
import com.eyecode.language.documentation.JdkSourceTarget;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class JavaFxJdkSourceWorkspace {

    private final JdkSourceLoader loader;
    private final Map<String, JavaFxJdkSourceTab> tabs = new LinkedHashMap<>();
    private Consumer<JdkSourceTarget> presenter;

    public JavaFxJdkSourceWorkspace() {
        this(new JdkSourceLoader());
    }

    JavaFxJdkSourceWorkspace(JdkSourceLoader loader) {
        this.loader = loader;
    }

    public void setPresenter(Consumer<JdkSourceTarget> presenter) {
        this.presenter = presenter;
    }

    public void open(JdkSourceTarget target) {
        if (target == null) {
            return;
        }
        if (presenter != null) {
            presenter.accept(target);
        } else {
            ensureTab(target);
        }
    }

    JavaFxJdkSourceTab ensureTab(JdkSourceTarget target) {
        JavaFxJdkSourceTab existing = tabs.get(target.tabId());
        if (existing != null) {
            return existing;
        }
        return loader.load(target).map(source -> {
            JavaFxJdkSourceTab tab = new JavaFxJdkSourceTab(target, source);
            tabs.put(target.tabId(), tab);
            return tab;
        }).orElse(null);
    }

    public JavaFxJdkSourceTab tab(String tabId) {
        return tabs.get(tabId);
    }

    public boolean contains(String tabId) {
        return tabs.containsKey(tabId);
    }

    public void close(String tabId) {
        JavaFxJdkSourceTab tab = tabs.remove(tabId);
        if (tab != null) {
            tab.dispose();
        }
    }

    public void dispose() {
        for (JavaFxJdkSourceTab tab : tabs.values()) {
            tab.dispose();
        }
        tabs.clear();
    }
}
