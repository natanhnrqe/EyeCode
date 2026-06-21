package com.eyecode.editor;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EditorContext {

    private final List<EditorTab> tabs;
    private EditorTab activeTab;
    private final RecentFiles recentFiles;

    public EditorContext() {
        this.tabs = new ArrayList<>();
        this.activeTab = null;
        this.recentFiles = new RecentFiles();
    }

    public void openTab(File file) {
        if (file == null) return;
        for (EditorTab tab : tabs) {
            if (tab.getFile().equals(file)) {
                activeTab = tab;
                return;
            }
        }
        EditorTab tab = new EditorTab(file);
        tabs.add(tab);
        activeTab = tab;
    }

    public void closeTab(int index) {
        if (index < 0 || index >= tabs.size()) return;
        boolean wasActive = tabs.get(index) == activeTab;
        tabs.remove(index);
        if (tabs.isEmpty()) {
            activeTab = null;
        } else if (wasActive) {
            activeTab = tabs.get(Math.min(index, tabs.size() - 1));
        }
    }

    public EditorTab getActiveTab() {
        return activeTab;
    }

    public void setActiveTab(int index) {
        if (index >= 0 && index < tabs.size()) {
            activeTab = tabs.get(index);
        }
    }

    public int getActiveTabIndex() {
        if (activeTab == null) return -1;
        return tabs.indexOf(activeTab);
    }

    public List<EditorTab> getTabs() {
        return Collections.unmodifiableList(tabs);
    }

    public void markDirty(int index) {
        if (index >= 0 && index < tabs.size()) {
            tabs.get(index).setDirty(true);
        }
    }

    public void markClean(int index) {
        if (index >= 0 && index < tabs.size()) {
            tabs.get(index).setDirty(false);
        }
    }

    public boolean isDirty(int index) {
        if (index < 0 || index >= tabs.size()) return false;
        return tabs.get(index).isDirty();
    }

    public void addRecentFile(File file) {
        recentFiles.add(file);
    }

    public List<File> getRecentFiles() {
        return recentFiles.getFiles();
    }

    private static class RecentFiles {
        private static final int MAX_SIZE = 10;
        private final List<File> files = new ArrayList<>();

        void add(File file) {
            if (file == null) return;
            files.remove(file);
            files.add(0, file);
            if (files.size() > MAX_SIZE) {
                files.remove(files.size() - 1);
            }
        }

        List<File> getFiles() {
            return Collections.unmodifiableList(files);
        }
    }
}
