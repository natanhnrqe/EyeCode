package com.eyecode.javafx.learning;

import com.eyecode.learning.content.LearningMetadata;
import com.eyecode.learning.content.LearningKind;
import com.eyecode.editor.v2.completion.CompletionItemKind;
import com.eyecode.javafx.designsystem.JavaFxIconManager;
import com.eyecode.designsystem.icon.EyeCodeIcon;
import javafx.scene.control.Label;
import javafx.scene.control.Hyperlink;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class JavaFxLearningCardHeader extends VBox {

    private final Label title = new Label();
    private final Label subtitle = new Label();
    private final ImageView icon = new ImageView();
    private final FlowPane breadcrumb = new FlowPane();
    private final List<Hyperlink> breadcrumbLinks = new ArrayList<>();
    private final VBox text = new VBox(title, subtitle);

    public JavaFxLearningCardHeader() {
        getStyleClass().add("learning-card-header");
        HBox titleRow = new HBox(icon, text);
        titleRow.getStyleClass().add("learning-card-title-row");
        titleRow.setSpacing(10);
        title.getStyleClass().add("learning-card-title");
        subtitle.getStyleClass().add("learning-card-subtitle");
        breadcrumb.getStyleClass().add("learning-card-breadcrumb");
        getChildren().addAll(titleRow, breadcrumb);
    }

    public void show(LearningMetadata metadata) {
        show(metadata, List.of(), ignored -> { });
    }

    public void show(LearningMetadata metadata, List<LearningMetadata> ancestors,
                     Consumer<String> navigationAction) {
        title.setText(metadata.title());
        subtitle.setText((metadata.category() + " · " + metadata.level() + " · "
                + metadata.duration() + " MIN").toUpperCase());
        icon.setImage(iconFor(metadata.concept(), metadata.kind()));
        icon.setFitWidth(22);
        icon.setFitHeight(22);
        icon.setPreserveRatio(true);
        breadcrumb.getChildren().clear();
        breadcrumbLinks.clear();
        if (ancestors == null || ancestors.isEmpty()) {
            breadcrumb.setVisible(false);
            breadcrumb.setManaged(false);
            return;
        }
        breadcrumb.setVisible(true);
        breadcrumb.setManaged(true);
        for (int index = 0; index < ancestors.size(); index++) {
            LearningMetadata ancestor = ancestors.get(index);
            if (index > 0) {
                breadcrumb.getChildren().add(new Label(" > "));
            }
            Hyperlink link = new Hyperlink(ancestor.title());
            link.setUserData(ancestor.id());
            link.setOnAction(event -> navigationAction.accept((String) link.getUserData()));
            breadcrumbLinks.add(link);
            breadcrumb.getChildren().add(link);
        }
        breadcrumb.getChildren().add(new Label(" > "));
        String parentTitle = ancestors.getLast().title();
        String childTitle = metadata.title();
        if (childTitle.startsWith(parentTitle + ".")) {
            childTitle = childTitle.substring(parentTitle.length() + 1);
        }
        breadcrumb.getChildren().add(new Label(childTitle));
    }

    private static javafx.scene.image.Image iconFor(String concept, LearningKind learningKind) {
        CompletionItemKind kind = learningKind == LearningKind.MEMBER
                ? CompletionItemKind.METHOD
                : switch (concept.toLowerCase()) {
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

    String breadcrumbTextForTest() {
        return breadcrumb.getChildren().stream()
                .map(node -> node instanceof Hyperlink link ? link.getText() : ((Label) node).getText())
                .reduce((left, right) -> left + right)
                .orElse("");
    }

    void fireBreadcrumbForTest(int index) {
        breadcrumbLinks.get(index).fire();
    }
}
