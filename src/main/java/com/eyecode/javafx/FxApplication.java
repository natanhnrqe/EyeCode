package com.eyecode.javafx;

import com.eyecode.javafx.ceffx.CeffxRuntime;
import com.eyecode.javafx.ui.FxMainWindow;
import javafx.application.Application;
import javafx.stage.Stage;

public final class FxApplication extends Application {

    public static void main(String[] args) {
        System.setProperty("prism.lcdtext", "true");
        System.setProperty("prism.text", "native");
        System.setProperty("prism.allowhidpi", "false");
        System.setProperty("glass.win.uiScale", "1.0");
        CeffxRuntime.startup(args);
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        FxMainWindow window = new FxMainWindow(primaryStage);
        window.show();
    }


}
