package com.eyecode.javafx.ui.toolwindow;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class PlaceholderToolWindowContentFactory implements ToolWindowContentFactory {

    private final Map<String, String> labelsById = new HashMap<>();

    public PlaceholderToolWindowContentFactory() {
        labelsById.put("project",       "Project Explorer");
        labelsById.put("search",        "Search");
        labelsById.put("learn",         "Lesson Browser");
        labelsById.put("roadmap",       "Roadmap");
        labelsById.put("documentation", "Documentation View");
        labelsById.put("preview",       "Preview Panel");
        labelsById.put("dependencies",  "Dependencies");
        labelsById.put("extensions",    "Extensions");
        labelsById.put("settings",      "Settings");
        labelsById.put("profile",       "Profile");

        labelsById.put("terminal",       "Terminal");
        labelsById.put("output",         "Output");
        labelsById.put("problems",       "Problems");
        labelsById.put("git",            "Git");
        labelsById.put("professor-ia",   "Professor IA");
    }

    @Override
    public Node createContent(String toolWindowId) {
        String text = labelsById.getOrDefault(toolWindowId, toolWindowId + " placeholder");
        Label label = new Label(text);
        label.getStyleClass().add("toolwindow-placeholder");
        VBox box = new VBox(label);
        box.getStyleClass().add("toolwindow-content");
        return box;
    }

    @Override
    public boolean supports(String toolWindowId) {
        return labelsById.containsKey(toolWindowId);
    }

    public Set<String> knownIds() {
        return Set.copyOf(labelsById.keySet());
    }
}
