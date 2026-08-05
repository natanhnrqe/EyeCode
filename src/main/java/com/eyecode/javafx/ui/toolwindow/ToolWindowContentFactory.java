package com.eyecode.javafx.ui.toolwindow;

import javafx.scene.Node;

public interface ToolWindowContentFactory {

    Node createContent(String toolWindowId);

    boolean supports(String toolWindowId);
}
