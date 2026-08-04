package com.eyecode.javafx.ui;

import com.eyecode.javafx.designsystem.FxCanvas;
import com.eyecode.javafx.designsystem.FxSpacing;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
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
        FxBottomToolWindowBar bottomBar = new FxBottomToolWindowBar();

        SplitPane centerSplit = new SplitPane(explorer, editorContainer);
        centerSplit.getStyleClass().add("center-split");
        centerSplit.setDividerPositions(0.20);

        SplitPane rootSplit = new SplitPane(centerSplit, console);
        rootSplit.getStyleClass().add("root-split");
        rootSplit.setOrientation(Orientation.VERTICAL);
        rootSplit.setDividerPositions(0.72);

        HBox bottomWrap = new HBox(bottomBar);
        bottomWrap.getStyleClass().add("bottom-tool-window-wrap");
        HBox.setHgrow(bottomBar, Priority.ALWAYS);
        bottomBar.setMaxWidth(Double.MAX_VALUE);

        BorderPane workspace = new BorderPane();
        workspace.getStyleClass().add("workspace");
        workspace.setCenter(rootSplit);
        workspace.setLeft(activityBar);
        workspace.setBottom(bottomWrap);

        FxCanvas canvas = new FxCanvas(workspace);
        canvas.setPadding(new Insets(
                FxSpacing.CANVAS_PADDING,
                FxSpacing.CANVAS_PADDING,
                FxSpacing.CANVAS_PADDING,
                FxSpacing.CANVAS_PADDING));
        return canvas;
    }
}
