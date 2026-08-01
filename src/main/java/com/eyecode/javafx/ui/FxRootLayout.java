package com.eyecode.javafx.ui;

import com.eyecode.javafx.editor.JavaFxEditor;
import com.eyecode.javafx.editor.JavaFxEditorController;
import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;

public final class FxRootLayout extends BorderPane {

    public FxRootLayout(Runnable onWindowClose) {
        getStyleClass().add("root-layout");

        FxToolbar toolbar = onWindowClose != null ? new FxToolbar(onWindowClose) : new FxToolbar();
        FxStatusBar statusBar = new FxStatusBar();
        Region workspace = buildWorkspace();

        setTop(toolbar);
        setCenter(workspace);
        setBottom(statusBar);
    }

    private Region buildWorkspace() {
        FxActivityBar activityBar = new FxActivityBar();
        FxExplorer explorer = new FxExplorer();
        FxEditorContainer editorContainer = new FxEditorContainer();
        FxConsole console = new FxConsole();

        SplitPane centerSplit = new SplitPane(explorer, editorContainer);
        centerSplit.getStyleClass().add("center-split");
        centerSplit.setDividerPositions(0.20);

        SplitPane rootSplit = new SplitPane(centerSplit, console);
        rootSplit.getStyleClass().add("root-split");
        rootSplit.setOrientation(Orientation.VERTICAL);
        rootSplit.setDividerPositions(0.78);

        BorderPane workspace = new BorderPane();
        workspace.getStyleClass().add("workspace");
        workspace.setLeft(activityBar);
        workspace.setCenter(rootSplit);

        return workspace;
    }
}
