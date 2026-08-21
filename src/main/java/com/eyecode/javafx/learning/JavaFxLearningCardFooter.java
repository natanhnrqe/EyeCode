package com.eyecode.javafx.learning;

import com.eyecode.learning.content.DocumentationTarget;
import com.eyecode.learning.content.LearningLink;
import com.eyecode.learning.content.LearningMetadata;
import com.eyecode.language.documentation.JdkSourceTarget;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;
import java.util.function.Function;

public final class JavaFxLearningCardFooter extends VBox {

    private final FlowPane relatedPane = new FlowPane();
    private final Hyperlink documentation = new Hyperlink();
    private final Hyperlink next = new Hyperlink();
    private final Hyperlink source = new Hyperlink("View Source </>");

    public JavaFxLearningCardFooter() {
        getStyleClass().add("learning-card-footer");
        relatedPane.getStyleClass().add("learning-card-related");
        documentation.getStyleClass().add("learning-card-documentation");
        next.getStyleClass().add("learning-card-next");
        source.getStyleClass().add("learning-card-source");
        getChildren().addAll(relatedPane, documentation, source, next);
    }

    public void show(
            LearningMetadata metadata,
            Consumer<String> relatedAction,
            Consumer<DocumentationTarget> documentationAction,
            Consumer<String> nextAction,
            Function<String, String> titleResolver
    ) {
        show(metadata, relatedAction, documentationAction, nextAction, titleResolver, null, target -> { });
    }

    public void show(
            LearningMetadata metadata,
            Consumer<String> relatedAction,
            Consumer<DocumentationTarget> documentationAction,
            Consumer<String> nextAction,
            Function<String, String> titleResolver,
            JdkSourceTarget sourceTarget,
            Consumer<JdkSourceTarget> sourceAction
    ) {
        relatedPane.getChildren().clear();
        if (!metadata.related().isEmpty()) {
            relatedPane.getChildren().add(new Label("Related:"));
            for (String identifier : metadata.related()) {
                Hyperlink link = new Hyperlink(titleResolver.apply(identifier));
                link.setUserData(LearningLink.toUri(identifier));
                link.setOnAction(event -> LearningLink.identifier((String) link.getUserData())
                        .ifPresent(relatedAction));
                relatedPane.getChildren().add(link);
            }
        }
        if (metadata.officialDocs() != null) {
            documentation.setText(metadata.officialDocs().label() + " ↗");
            documentation.setVisible(true);
            documentation.setManaged(true);
            documentation.setOnAction(event -> documentationAction.accept(metadata.officialDocs()));
        } else {
            documentation.setVisible(false);
            documentation.setManaged(false);
        }
        source.setVisible(sourceTarget != null);
        source.setManaged(sourceTarget != null);
        source.setOnAction(event -> {
            if (sourceTarget != null) {
                sourceAction.accept(sourceTarget);
            }
        });
        if (metadata.next() != null && !metadata.next().isBlank()) {
            next.setText("Next: " + titleResolver.apply(metadata.next()) + " →");
            next.setVisible(true);
            next.setManaged(true);
            next.setOnAction(event -> nextAction.accept(metadata.next()));
        } else {
            next.setVisible(false);
            next.setManaged(false);
        }
    }

    int relatedCountForTest() {
        return relatedPane.getChildren().size();
    }

    String documentationTextForTest() {
        return documentation.getText();
    }

    boolean sourceVisibleForTest() {
        return source.isVisible() && source.isManaged();
    }

    void fireSourceForTest() {
        source.fire();
    }
}
