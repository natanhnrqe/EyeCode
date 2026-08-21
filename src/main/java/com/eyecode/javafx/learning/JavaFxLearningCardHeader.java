package com.eyecode.javafx.learning;

import com.eyecode.learning.content.LearningMetadata;
import com.eyecode.editor.v2.completion.CompletionItemKind;
import com.eyecode.javafx.designsystem.JavaFxIconManager;
import com.eyecode.designsystem.icon.EyeCodeIcon;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class JavaFxLearningCardHeader extends VBox {

    private final Label title = new Label();
    private final Label subtitle = new Label();
    private final ImageView icon = new ImageView();
    private final VBox text = new VBox(title, subtitle);

    public JavaFxLearningCardHeader() {
        getStyleClass().add("learning-card-header");
        HBox titleRow = new HBox(icon, text);
        titleRow.getStyleClass().add("learning-card-title-row");
        titleRow.setSpacing(10);
        title.getStyleClass().add("learning-card-title");
        subtitle.getStyleClass().add("learning-card-subtitle");
        getChildren().add(titleRow);
    }

    public void show(LearningMetadata metadata) {
        title.setText(metadata.title());
        subtitle.setText((metadata.category() + " · " + metadata.level() + " · "
                + metadata.duration() + " MIN").toUpperCase());
        icon.setImage(iconFor(metadata.concept()));
        icon.setFitWidth(22);
        icon.setFitHeight(22);
        icon.setPreserveRatio(true);
    }

    private static javafx.scene.image.Image iconFor(String concept) {
        CompletionItemKind kind = switch (concept.toLowerCase()) {
            case "class" -> CompletionItemKind.CLASS;
            case "interface" -> CompletionItemKind.INTERFACE;
            case "enum" -> CompletionItemKind.ENUM;
            case "record" -> CompletionItemKind.RECORD;
            default -> null;
        };
        return kind == null
                ? JavaFxIconManager.icon(EyeCodeIcon.MARKDOWN, 22).getImage()
                : JavaFxIconManager.completionIcon(kind, 22).getImage();
    }

    String titleForTest() {
        return title.getText();
    }

    String subtitleForTest() {
        return subtitle.getText();
    }

    boolean iconLoadedForTest() {
        return icon.getImage() != null && !icon.getImage().isError();
    }
}
